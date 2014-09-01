package com.rulecity.williams.biz;

import com.rulecity.williams.interfaces.ILogger;
import jssc.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * Created by Matt on 8/29/2014.
 */
public class listener
{
	SerialPort m_serialPort;
	ILogger m_logger;
	boolean m_bGotSyncByte = false;
	int m_iPacketLength = 0;
	int m_iPacketIdx = 0;
	int m_iCRCIdx = 0;
	int m_iRawIdx = 0;
	int[] m_packetBuf;
	int[] m_crc = new int[2];	// workaround java's signed/unsigned LAMENESS
	boolean m_bGotBuildNumber = false;
	long m_uBuildNumberRequestedTimestamp = 0;
	boolean m_bFirmwareUpdateRequestSending = false;
	long m_uFirmwareUpdateRequestedTimestamp = 0;
	String m_strFirmwareFilePath;

	public listener(ILogger logger, String strFirmwareFilePath)
	{
		m_logger = logger;
		m_strFirmwareFilePath = strFirmwareFilePath;
	}

	public void init(String strSerialDev) throws SerialPortException
	{
		m_serialPort = new SerialPort(strSerialDev);

		m_serialPort.openPort();
		m_serialPort.setParams(1000000,   // bit rate is 1 megabit
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

		int mask = SerialPort.MASK_RXCHAR;//Prepare mask
		m_serialPort.setEventsMask(mask);//Set mask
		m_serialPort.addEventListener(new SerialPortReader());//Add SerialPortEventListener
	}

	public void shutdown() throws SerialPortException
	{
		m_serialPort.closePort();
	}

	class SerialPortReader implements SerialPortEventListener
	{
		public void serialEvent(SerialPortEvent event)
		{
			//If data is available
			if (event.isRXCHAR())
			{
				int iBytesAvailable = event.getEventValue();
				try
				{
					byte buffer[] = m_serialPort.readBytes(iBytesAvailable);

					processBytes(buffer);
				}
				catch (SerialPortException e)
				{
					e.printStackTrace();
				}
			}
		}
	} // end inner class

	public void processBytes(byte[] buffer)
	{
		for (byte chSigned : buffer)
		{
			int ch = chSigned & 0xFF;	// workaround java's lameness

			// if we are waiting for the sync byte
			if (!m_bGotSyncByte)
			{
				// If we get a 0, it means we've found the beginning of a new packet
				//  so we are _probably_ in sync (CRC could contain a 0 for example, so this is not guaranteed)
				// If we aren't in sync, this will definitely help us get in sync until we eventually are in sync again.
				if (ch != 0)
				{
					continue;
				}

				m_iPacketLength = 0;
				m_iPacketIdx = 0;
				m_iCRCIdx = 0;
				m_iRawIdx = 0;

				m_bGotSyncByte = true;
			}

			// else if we're waiting for the first byte of the packet length
			else if (m_iRawIdx == 1)
			{
				m_iPacketLength = ch;
			}
			// else if we're waiting for the second byte of the packet length
			else if (m_iRawIdx == 2)
			{
				int u8Tmp = ch ^ 0xFF;

				// CHANGED: the second byte MUST be the XOR of the first byte (so we know the packet length is correct)
				if (m_iPacketLength == u8Tmp)
				{
					m_packetBuf = new int[m_iPacketLength];	// use int instead of byte to mimic unsigned behavior
				}
				// else XOR is off so the packet is corrupt, retry
				else
				{
					String s = "Packet length is corrupt.  Discarding packet.";
					log(s);

					m_bGotSyncByte = false;
					continue;
				}
			}
			// else if we are in the middle of receiving the packet
			else if (m_iPacketIdx < m_iPacketLength)
			{
				m_packetBuf[m_iPacketIdx++] = ch;
			}
			// else if we are receiving the CRC at the end of the packet
			else if (m_iCRCIdx < 2)
			{
				m_crc[m_iCRCIdx++] = ch;

				// if we're ready to process the packet
				if (m_iCRCIdx == 2)
				{
					processPacket();
					m_bGotSyncByte = false;
					continue;    // keep rawIdx at 0
				}
			}

			// this makes it convenient to figure out where we are
			m_iRawIdx++;
		}
	}

	public long getMs()
	{
		return System.nanoTime() / 1000000;    // millisecond value
	}

	public void processPacket()
	{
		try
		{
			Crc crc = new Crc();

			// check CRC first
			crc.crc_ccitt_init();
			for (int i = 0; i < m_iPacketLength; i++)
			{
				crc.crc_ccitt_update(m_packetBuf[i]);
			}

			// little endian
			int iTmp1 = m_crc[0];
			int iTmp2 = m_crc[1] << 8;
			int uReceivedCRC = (m_crc[0] | (m_crc[1] << 8)) & 0xFFFF;
			if (uReceivedCRC != crc.crc_ccitt_crc())
			{
				String s = "CRC mismatch.  Expected ";
				s += Integer.toHexString(uReceivedCRC);
				s += " but got " + Integer.toHexString(crc.crc_ccitt_crc());
				s += ".  Discarding packet.";
				throw new RuntimeException(s);
			}

			// get command code
			int chCmd = m_packetBuf[0];

			// if we are trying to force firmware update and we are getting a different packet, ignore it
			if (m_bFirmwareUpdateRequestSending && (chCmd != 'Z') && (chCmd != 'L'))
			{
				return;
			}

			switch (chCmd)
			{
				case 'E':    // error log
				{
					String strPacket = new String(this.toByteArray(m_packetBuf));
					String strMsg = "AVR ERR: " + strPacket.substring(1);	// get rid of first byte
					logError(strMsg);
					break;
				}
				// log message
				case 'L':
				{
					String strPacket = new String(this.toByteArray(m_packetBuf));
					String strMsg = "AVR: " + strPacket.substring(1);	// get rid of first byte
					log(strMsg);
					break;
				}

				// build number
				case 'N':
					checkPacketLength("build number", 5);
					RandomAccessFile stream = new RandomAccessFile(m_strFirmwareFilePath, "r");
					int u16CRC = getCRCOfStream(stream);
					int u16RemoteCRC = m_packetBuf[1] | (m_packetBuf[2] << 8);

					if (u16CRC == u16RemoteCRC)
					{
						log("Dexter board's firmware is up to date.");
						m_bGotBuildNumber = true;
					}
					// if the CRC value is all F's it probably means that there is no bootloader present so don't try to update
					else if (u16RemoteCRC == 0xFFFF)
					{
						log("AVR does not appear to have a bootloader installed, so we will not attempt to update.");
						m_bGotBuildNumber = true;
					} else
					{
						log("AVR's firmware is not up to date; attempting to update.");
						startSendingReprogramRequests();
					}

					break;

				// send ROM program for updating
				case 'Z':
					// verify version
					if (m_packetBuf[1] != 0)
					{
						throw new RuntimeException("Unexpected firmware update request version");
					}

					// verify size
					if (m_packetBuf[2] != 8)
					{
						throw new RuntimeException("Unexpected firmware update page size");
					}

					int u16PageIdx = m_packetBuf[3] | (m_packetBuf[4] << 8);

					// this is a convenient place to indicate that we don't need to keep re-sending firmware update requests to the main AVR anymore,
					//   because the updater is running
					m_bFirmwareUpdateRequestSending = false;

					sendFirmwarePage(u16PageIdx);

					break;

				// else unknown
				default:
				{
					String s = "Unknown command: ";
					s += m_packetBuf[0];
					throw new RuntimeException(s);
				}
			}
		}
		catch (Exception e)
		{
			logError(e.getMessage());
		}
	}

	private byte [] toByteArray(int[] src)
	{
		byte [] result = new byte[src.length];

		for (int i = 0; i < src.length; i++)
		{
			result[i] = (byte) (src[i]);
		}

		return result;
	}

	public void think() throws SerialPortException
	{
		long uTimestamp = getMs();

		// if we are trying to force firmware update and we are getting a different packet, ignore it
		if (m_bFirmwareUpdateRequestSending)
		{
			// check to see if it's been long enough that we need to re-send firmware update request
			if ((uTimestamp - m_uFirmwareUpdateRequestedTimestamp) > 2000)
			{
				log("Sending reprogram request");
				sendReprogramRequest();
			}
			return;
		}

		// if we haven't verified remote build number yet
		if (!m_bGotBuildNumber)
		{
			// check to see if it's been long enough that we need to re-send request
			if ((uTimestamp - m_uBuildNumberRequestedTimestamp) > 2000)
			{
				// this helps us understand when there is a problem with I/O
				log("Sending request to verify firmware");
				sendBuildNumberRequest();
			}
		}
	}

	private void sendFirmwarePage(int u16PageIdx)
	{
		final int stLastPageIdx = 247;
		final int stPageSize = 256;
		final byte u8CommandLetter = 'P';

		// reset timestamps so other requests are not sent during firmware programming
		m_uBuildNumberRequestedTimestamp = getMs();

		// if all pages have been sent successfully
		if (u16PageIdx == 0xFFFF)
		{
			log("Programming successful!");
			return;
		}
		else
		{
			String s = "Firmware: Sending page ";
			s += u16PageIdx;
			s += " / ";
			s += stLastPageIdx;
			s += " (";
			s += stPageSize;
			s += " bytes)";
			log(s);
		}

		try
		{
			RandomAccessFile stream = new RandomAccessFile(m_strFirmwareFilePath, "r");

			byte[] pSendBuf = new byte[stPageSize + 3];

			pSendBuf[0] = u8CommandLetter;
			pSendBuf[1] = (byte) (u16PageIdx & 0xFF);
			pSendBuf[2] = (byte) (u16PageIdx >>> 8);

			// set the array to "empty" values because our file may not extend as far as the page request covers
			for (int i = 3; i < pSendBuf.length; i++)
			{
				pSendBuf[i] = (byte) 0xFF;
			}

			long u64Offset = u16PageIdx * stPageSize;

			// if we have more of the file to return
			if (u64Offset < stream.length())
			{
				// read in the page requested
				stream.seek(u64Offset);
				stream.read(pSendBuf, 3, stPageSize);
			}
			// else we're passed the end of the file, so just return default filled buffer

			// if we are sending the last page, put the CRC-16 value as the last 2 bytes so that the firmware knows its own CRC value without having to calculate it
			if (u16PageIdx == stLastPageIdx)
			{
				int u16CRC = getCRCOfStream(stream);

				// store final result (little endian) as last 2 bytes of page
				pSendBuf[stPageSize + 1] = (byte) (u16CRC & 0xFF);
				pSendBuf[stPageSize + 2] = (byte) (u16CRC >>> 8);
			}

			// send buffer now
			sendPacket(pSendBuf);
		}
		catch (Exception ex)
		{
			logError(ex.getMessage());
		}
	}

	private int getCRCOfStream(RandomAccessFile stream) throws IOException
	{
		Crc crc = new Crc();
		crc.crc_ccitt_init();
		int u8;

		// save position so that we can restore it
		long lPos = stream.getFilePointer();

		stream.seek(0);

		for (; ; )
		{
			u8 = stream.read();

			// EOF
			if (u8 == -1)
			{
				break;
			}

			crc.crc_ccitt_update((byte) u8);
		}

		// restore previous position
		stream.seek(lPos);

		return crc.crc_ccitt_crc();
	}

	private void sendBuildNumberRequest() throws SerialPortException
	{
		String strPacket = "N";
		sendPacket(strPacket.getBytes());
		m_uBuildNumberRequestedTimestamp = getMs();
	}

	private void log(String strMsg)
	{
		m_logger.Log(strMsg);
	}

	private void logError(String strMsg)
	{
		m_logger.LogError(strMsg);
	}

	private void checkPacketLength(String strPacketName, int stExpectedLength) throws UnsupportedOperationException
	{
		if (m_iPacketLength != stExpectedLength)
		{
			String s = "Unexpected length for ";
			s += strPacketName;
			s += " command: ";
			s += Integer.toString(m_iPacketLength);
			s += " (ignoring)";
			throw new UnsupportedOperationException(s);
		}
	}

	void sendPacket(byte[] buffer) throws SerialPortException
	{
		int u16Length = buffer.length & 0xFFFF;    // length is a 16-bit value based on protocol definition
		byte u8;
		Crc crc = new Crc();

		u8 = 0;
		m_serialPort.writeByte(u8);
		u8 = (byte) (u16Length & 0xFF);
		m_serialPort.writeByte(u8);    // send length LSB
		u8 = (byte) (u16Length >>> 8);
		m_serialPort.writeByte(u8);    // send length MSB
		u8 = (byte) ((u16Length & 0xFF) ^ 0xFF);
		m_serialPort.writeByte(u8);    // send length LSB XOR 0xFF
		u8 = (byte) ((u16Length >>> 8) ^ 0xFF);
		m_serialPort.writeByte(u8);    // send length MSB XOR 0xFF
		crc.crc_ccitt_init();
		m_serialPort.writeBytes(buffer);
		for (byte b : buffer)
		{
			crc.crc_ccitt_update(b);
		}

		// send CRC (little endian)
		int u16 = crc.crc_ccitt_crc();
		u8 = (byte) (u16 & 0xFF);
		m_serialPort.writeByte(u8);
		u8 = (byte) (u16 >>> 8);
		m_serialPort.writeByte(u8);
	}

	private void startSendingReprogramRequests()
	{
		m_bFirmwareUpdateRequestSending = true;
		m_uFirmwareUpdateRequestedTimestamp = 0;
	}

	private void sendReprogramRequest() throws SerialPortException
	{
		String strSettingsRequest = "Z";
		sendPacket(strSettingsRequest.getBytes());
		log("Sent a reprogram request to controller");
		m_uFirmwareUpdateRequestedTimestamp = getMs();
	}
}
