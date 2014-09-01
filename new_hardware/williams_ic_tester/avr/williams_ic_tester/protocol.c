#include <avr/io.h>
#include <avr/eeprom.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>
#include "serial.h"
#include <string.h>
#include "crc.h"
#include "protocol.h"

// 645
#define EEPROM_SIZE_BYTES 2048
#define FLASH_SIZE_BYTES 65536

// CRC-16 value stored as the last 2 bytes of application memory.  The bootloader takes up the last 2k, so subtract that first.
#define FLASH_CRC_OFFSET ((FLASH_SIZE_BYTES - 2048) - 2)

// this is somewhat arbitrary, just remember that SRAM is precious, so the lower this is, the better
#define MAX_PACKET_SIZE 32

extern uint8_t g_u8WhiteFlags[2];
extern uint32_t g_u32PicNums[3][2];
extern uint8_t g_bUnmuteQueued;

uint8_t g_bGotSyncByte = 0;
uint16_t g_u16RawIdx = 0;
uint16_t g_u16PacketLength = 0;
uint8_t g_u8PacketXorTmp = 0;
uint16_t g_u16PacketIdx = 0;
uint8_t g_pPacketBuf[MAX_PACKET_SIZE];
uint8_t g_u8CRCIdx = 0;
uint8_t g_crc[2];

////////////////////////////////

// call this regularly
void io_think()
{
	// if no character is waiting in the receive buffer, we're done
	if (!rx_is_char_waiting())
	{
		// empty output buffer
		DO_TX_THINK();
		return;
	}

	// handle 1 byte from receive buffer
	uint8_t ch = rx_from_buf();

	// if we are waiting for the sync byte
	if (!g_bGotSyncByte)
	{
		// If we get a 0, it means we've found the beginning of a new packet
		//  so we are _probably_ in sync (CRC could contain a 0 for example, so this is not guaranteed)
		// If we aren't in sync, this will definitely help us get in sync until we eventually are in sync again.
		if (ch != 0)
		{
			g_u16RawIdx = 0;
			return;	// don't increment RawIdx
		}

		g_bGotSyncByte = 1;
	}
	// else if we're waiting for the first byte of the packet length
	else if (g_u16RawIdx == 1)
	{
		g_u16PacketLength = ch;
	}
	// else if we're waiting for the second byte of the packet length
	else if (g_u16RawIdx == 2)
	{
		g_u16PacketLength = (ch << 8) | g_u16PacketLength;
	}
	// else if we're waiting for the first XOR byte of the packet length
	else if (g_u16RawIdx == 3)
	{
		g_u8PacketXorTmp = ch;
	}
	// else if we're waiting for the second XOR byte of the packet length
	else if (g_u16RawIdx == 4)
	{
		// the second byte MUST be the XOR of the first byte (so we know the packet length is correct)
		if (g_u16PacketLength == ((g_u8PacketXorTmp | (ch << 8)) ^ 0xFFFF))
		{
			// check for packet size being too big
			if (g_u16PacketLength > MAX_PACKET_SIZE)
			{
				LogString(STRING_BAD_PACKET_LENGTH);
				goto reset;
			}
			// else
			// everything is ok
		}
		else
		{
			LogString(STRING_BAD_PACKET_LENGTH);
			goto reset;
		}
	}
	// else if we are in the middle of receiving the packet
	else if (g_u16PacketIdx < g_u16PacketLength)
	{
		g_pPacketBuf[g_u16PacketIdx++] = ch;
	}
	// else if we are receiving the CRC at the end of the packet
	else if (g_u8CRCIdx < 2)
	{
		g_crc[g_u8CRCIdx++] = ch;

		// if we've received the full packet, then process it
		if (g_u8CRCIdx == 2)
		{
			ProcessPacket();

reset:
			g_bGotSyncByte = 0;
			g_u16PacketLength = 0;
			g_u16PacketIdx = 0;
			g_u8CRCIdx = 0;
			g_u16RawIdx = 0;
			g_u8PacketXorTmp = 0;
			return;	// don't increment rawIdx
		}
	}

	// this makes it convenient to figure out where we are
	g_u16RawIdx++;

}

void ProcessPacket()
{
	// check CRC first
	crc_ccitt_init();
	for (uint16_t i = 0; i < g_u16PacketLength; i++)
	{
		crc_ccitt_update(g_pPacketBuf[i]);
	}

	uint16_t u16ActualCRC = crc_ccitt_crc();
	uint16_t u16ReceivedCRC = g_crc[0] | (g_crc[1] << 8);	// little endian

	// if CRC does not match
	if (u16ReceivedCRC != u16ActualCRC)
	{
		LogString(STRING_CRC_ERROR);
		return;
	}

	// get command code
	unsigned char chCmd = g_pPacketBuf[0];

	switch (chCmd)
	{
	default:
		{
			char s[20];
			char msg[2];
			string_to_buf(s, STRING_UNKNOWN_PACKET);
			msg[0] = chCmd;
			msg[1] = 0;
			strcat(s, msg);
			SendLog(s);
		}
		break;
	case 'N':	// build number request
		SendBuildNumber();
		break;
	case 'Z':	// force firmware update
		ForceReprogram();
		break;
	}
}

void ForceReprogram()
{
	// tell bootloader that it needs to reprogram
	eeprom_update_word((uint16_t *) EEPROM_SIZE_BYTES - 2, 0);

	// disable all interrupts that may have been enabled so bootloader runs properly
	EIMSK = 0;	// disable all external interrupts
	serial_shutdown();	// disable serial port interrupt
	cli();	// disable all interrupts

	// jump to the bootloader now
	asm volatile("jmp 0x7C00"::);
}

void LogString(StringID id)
{
	char s[30];
	string_to_buf(s, id);
	SendLog(s);
}

///////////////////////////////////////////

void SendBuf(unsigned char *pBuf, uint8_t u8Length)
{
	unsigned int uCRC = 0;

	crc_ccitt_init();

	tx_to_buf(0);	// sync byte
	tx_to_buf(u8Length);	// length LSB
	tx_to_buf(u8Length ^ 0xFF);	// xor of length LSB
	for (uint8_t i = 0; i < u8Length; i++)
	{
		tx_to_buf(pBuf[i]);
		crc_ccitt_update(pBuf[i]);
	}
	uCRC = crc_ccitt_crc();
	tx_to_buf(uCRC & 0xFF);	// little endian
	tx_to_buf(uCRC >> 8);
}

void SendLogHelper(const char *s, char ch)
{
// ASSUMPTION: log message is <= 255 bytes
// (if this isn't true, it should be true)

	uint16_t uCRC = 0;
	unsigned char u = strlen(s) + 1;	// add 1 to take into account ASCII letter in front of message
	unsigned char i = 0;

	crc_ccitt_init();

	tx_to_buf(0);	// sync byte
	tx_to_buf(u);	// length LSB
	tx_to_buf(u ^ 255);	// length XOR
	tx_to_buf(ch);
	crc_ccitt_update(ch);
	while (s[i] != 0)
	{
		tx_to_buf(s[i]);
		crc_ccitt_update(s[i]);
		i++;
	}

	uCRC = crc_ccitt_crc();
	tx_to_buf(uCRC & 0xFF);	// little endian
	tx_to_buf(uCRC >> 8);

}

void SendBuildNumber()
{
	unsigned char buf[5] = { 'N', 0, 0, 0, 0 };
	
	// grab CRC value from end of ROM (updater put it there for us)
	uint16_t u16 = pgm_read_word(FLASH_CRC_OFFSET);
	buf[1] = u16;
	buf[2] = u16 >> 8;
	
	SendBuf(buf, sizeof(buf));
}

void SendLog(const char *s)
{
	SendLogHelper(s, 'L');
}
