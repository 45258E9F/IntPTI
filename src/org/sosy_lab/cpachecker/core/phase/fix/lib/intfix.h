#ifndef __INTFIX_TSMART_H
#define __INTFIX_TSMART_H

extern void tsmart_fix_int_error(const char *) __attribute__((noreturn));

extern char tsmart_fix_int_s_char(long long int x);

extern char tsmart_fix_int_u_char(long long unsigned int x);

extern signed char tsmart_fix_int_s_schar(long long int x);

extern signed char tsmart_fix_int_u_schar(long long unsigned int x);

extern unsigned char tsmart_fix_int_s_uchar(long long int x);

extern unsigned char tsmart_fix_int_u_uchar(long long unsigned int x);

extern short tsmart_fix_int_s_short(long long int x);

extern short tsmart_fix_int_u_short(long long unsigned int x);

extern unsigned short tsmart_fix_int_s_ushort(long long int x);

extern unsigned short tsmart_fix_int_u_ushort(long long unsigned int x);

extern int tsmart_fix_int_s_int(long long int x);

extern int tsmart_fix_int_u_int(long long unsigned int x);

extern unsigned int tsmart_fix_int_s_uint(long long int x);

extern unsigned int tsmart_fix_int_u_uint(long long unsigned int x);

extern long tsmart_fix_int_s_lint(long long int x);

extern long tsmart_fix_int_u_lint(long long unsigned int x);

extern unsigned long tsmart_fix_int_s_ulint(long long int x);

extern unsigned long tsmart_fix_int_u_ulint(long long unsigned int x);

extern long long int tsmart_fix_int_s_llint(long long int x);

extern long long int tsmart_fix_int_u_llint(long long unsigned int x);

extern long long unsigned tsmart_fix_int_s_ullint(long long int x);

extern long long unsigned tsmart_fix_int_u_ullint(long long unsigned int x);

extern long long int tsmart_fix_int_add_s(long long int x, long long int y);

extern long long unsigned int tsmart_fix_int_add_u(long long unsigned int x, long long unsigned int y);

extern long long int tsmart_fix_int_minus_s(long long int x, long long int y);

extern long long unsigned int tsmart_fix_int_minus_u(long long unsigned int x, long long unsigned int y);

extern long long int tsmart_fix_int_multiply_s(long long int x, long long int y);

extern long long unsigned int tsmart_fix_int_multiply_u(long long unsigned int x, long long unsigned int y);

#endif // __INTFIX_TSMART_H
