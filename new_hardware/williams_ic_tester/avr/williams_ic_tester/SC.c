/*
 * SC.c
 *
 * Created: 9/1/2014 2:02:30 PM
 *  Author: Matt
 */ 

#include "protocol.h"
#include "string.h"
#include <avr/io.h>
#include <avr/interrupt.h>

// whether we are testing special chip version 2
uint8_t g_bSCIsVersion2 = 0;

// 
volatile uint16_t g_u16SCStep = 0;

void SpecialChipStart()
{
	uint8_t bDone = 0;
	
	
	// Setup 16-bit timer
	// (other drivers may change this so we must set it here)
	TCCR1B = (1 << CS10) | (1 << WGM12);	// no prescaling (search datasheet for TCCR1B for details), CTC mode enabled
	
	while (!bDone)
	{
		switch ()
	}
}

ISR(TIMER1_COMPA_vect)
{
}