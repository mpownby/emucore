/*
 * SC.h
 *
 * Created: 8/22/2014 8:29:06 PM
 *  Author: Matt
 */ 

#ifndef SC_H_
#define SC_H_

#include <avr/pgmspace.h>

/* pin-out:
------------
1:	E'
2:	TCF'
3:	HTCF'
4:	D7
5:	HALT'
6:	BA-BS'
7:	D6
8:	D5
9:	D4
10:	GND
11: D3
12: D2
13: D1
14: U/L'
15: NC
16:	GND
17: D0
18: R/W'
19: A3
20: A4
21: A5
22: A6
23: A7
24:	GND
25: CS'
26: A2
27: A1
28: A0
29:	VCC
30: A8
31: A9
32: A10
33: A11
34: A12
35: 4MHz
36: RESET'
37: A13
38: A14
39: A15
40: WINH'

*/

typedef enum
{
	SC_IDLE,	/* CPU has control */
	SC_ACTIVE	/* SC has control */
} SCBus_t;

typedef enum
{
	SC_EN0_4MHZ0,	/* E' is low, 4MHz is low */
	SC_EN0_4MHZ1,
	SC_EN1_4MHZ0,
	SC_EN1_4MHZ1	/* E' is high, 4MHz is high */
} SCClocks_t;

typedef struct
{
	uint16_t u16A;
	uint8_t u8D;
} SCState_t;

typedef struct
{
	SCBus_t busEntering;
	uint8_t arrExpected[5];
	
	SCBus_t busLeaving;
	uint8_t arrChanged[5];
	
} SCEntry_t;

void SC_start();

#endif /* SC_H_ */