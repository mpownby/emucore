#ifndef SERIAL_H
#define SERIAL_H

// 16 MHz clock allows 1 megabit transfer rate
#define USART_BAUDRATE 1000000

void serial_init();
void serial_shutdown();
void send_byte_now(unsigned char ch);
void debug_send(const char *s);	// don't use this for production!
unsigned char rx_is_char_waiting();
unsigned char rx_from_buf();
void tx_to_buf(unsigned char u8Byte);	// NOTE : this _will_ enable tx interrupt

extern volatile unsigned char g_u8TxBufCount;

// uncomment for production, comment out for debugging since TX interrupts mess up stepping through code
#define TX_USE_INT

#ifdef TX_USE_INT

#define DO_TX_THINK()

 // Enable the USART Data Register Empty interrupt (if data is available in TX buffer, the ISR _must_ not be enabled if TX buffer is empty)
#define TX_INT_ENABLE() if (g_u8TxBufCount != 0) UCSR0B |= (1 << UDRIE0)

 // Disable the USART Data Register Empty interrupt
#define TX_INT_DISABLE() UCSR0B &= ~(1 << UDRIE0);

#else
#define TX_INT_ENABLE()
#define TX_INT_DISABLE()

#define DO_TX_THINK() tx_think()
void tx_think();

#endif // TX_USE_INT

#endif // SERIAL_H
