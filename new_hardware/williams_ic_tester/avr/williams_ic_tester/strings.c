#include "strings.h"
#include <avr/pgmspace.h>

// The purpose of this file is to force the compiler to store strings inside ROM and not put them inside SRAM.

// IMPORTANT: keep these strings shorter than the buffer in log_string!!
const char strCRCError[] PROGMEM = "CRC err";
const char strUnknownPacket[] PROGMEM = "Unknown packet: ";
const char strBadPacketLength[] PROGMEM = "Bad len";
const char strSpecialChip[] PROGMEM = "Special chip";
const char strDetected[] PROGMEM = "detected";
const char strSpace[] PROGMEM = " ";
const char strUnknown[] PROGMEM = "Unknown";

PGM_P const string_table[] PROGMEM =
{
	strCRCError,
	strUnknownPacket,
	strBadPacketLength,
	strSpecialChip,
	strDetected,
	strSpace,
	strUnknown
};

void string_to_buf(char *s, StringID id)
{
	strcpy_P(s, (PGM_P)pgm_read_word(&(string_table[id])));
}
