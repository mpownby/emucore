#include <avr/io.h>
#include <avr/interrupt.h>
#include "serial.h"
#include "strings.h"
#include "protocol.h"
#include <string.h>

int main(void)
{
	// setup 16-bit timer to divide by 1024 (we will want to be delaying a second or two if possible)
	// Setup CTC to trigger when timer hits its boundary.
	TCCR1B = (1 << CS12) | (1 << CS10) | (1 << WGM12);
	
	serial_init();
	
	// put all pins in input mode to detect what jumpers are installed on the board
	DDRA = 0;
	DDRB = 0;
	DDRC = 0;
	DDRD = 0;
	DDRE &= ~((1 << PE2) | (1 << PE3) | (1 << PE4) | (1 << PE5) | (1 << PE6) | (1 << PE7));	// RX/TX on PE0/PE1
	DDRF &= ~((1 << PF0) | (1 << PF1));
	
	DDRG = (1 << PG0);	// LED (output)
	
	// put internal pull-ups on pins 10, 16, and 24 to detect special chip jumpers forcing these to ground
	PORTB |= (1 << PB1) | (1 << PB7);	// pin 10 & 16
	PORTC |= (1 << PC7);	// pin 24
	
	// enable interrupts so serial port I/O will work
	sei();
	
    while(1)
    {
		TCNT1 = 0;

		// if PD4 (Pin29) is high (VCC) and PB1,PB7 and PC7 are low, it means jumpers are setup for special chip
		if (((PIND & (1 << PD4)) != 0) && ((PINB & ((1 << PB1)|(1 << PB7))) == 0) && ((PINC & (1 << PC7)) == 0))
		{
			char s[30];	// don't retain this on stack
			char s1[30];	// " " "
			string_to_buf(s, STRING_SPECIAL_CHIP);
			string_to_buf(s1, STRING_SPACE);
			strcat(s, s1);
			string_to_buf(s1, STRING_DETECTED);
			strcat(s, s1);
			SendLog(s);
		}
		// else we don't know what the jumpers are set to
		else
		{
			char s[30];	// don't retain this on stack
			string_to_buf(s, STRING_UNKNOWN);
			SendLog(s);			
		}

		// wait so that we don't flood the TX buffer
		while (TCNT1 < 7812)
		{
			io_think();	// service low priority tasks
		}
    }
	
	serial_shutdown();
}