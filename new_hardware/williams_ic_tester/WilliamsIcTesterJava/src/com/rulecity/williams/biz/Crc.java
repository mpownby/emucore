package com.rulecity.williams.biz;

/**
 * Created by Matt on 8/29/2014.
 */
public class Crc
{
	// datatype is 32-bits to avoid dealing with signed issues with 16-bit short
    int m_crc;

///////

	public void crc_ccitt_init()
	{
		m_crc = 0xffff;    // 0xFFFF initialization standard CCITT
	}

    // see http://www.lammertbies.nl/comm/info/Crc-calculation.html
//  and http://zorc.breitbandkatze.de/Crc.html
//  to verify the accuracy of this algorithm.
    public void crc_ccitt_update(int u8)
    {
		u8 &= 0xFF;	// just in case incoming data is a signed character (workaround java's lameness)
        int crc_new = ((m_crc >>> 8) | (m_crc << 8));
        crc_new ^= u8;
        crc_new ^= ((crc_new & 0xff) >>> 4);
        crc_new ^= crc_new << 12;
        crc_new ^= (crc_new & 0xff) << 5;
        m_crc = crc_new & 0xFFFF;	// maintain 16-bit result to avoid sign issues
    }

	public int crc_ccitt_crc()
	{
		return m_crc;
	}

}
