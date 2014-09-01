#ifndef STRINGS_H
#define STRINGS_H

// The purpose of this file is to force the compiler to store strings inside ROM and not put them inside SRAM.

typedef enum
{
	STRING_CRC_ERROR,
	STRING_UNKNOWN_PACKET,
	STRING_BAD_PACKET_LENGTH,
	STRING_SPECIAL_CHIP,
	STRING_DETECTED,
	STRING_SPACE,
	STRING_UNKNOWN,
	STRING_COUNT	/* this must come at the end */
} StringID;

void string_to_buf(char *s, StringID id);

#endif //  STRINGS_H
