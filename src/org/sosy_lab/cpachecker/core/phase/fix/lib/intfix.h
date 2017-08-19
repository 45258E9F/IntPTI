#ifndef __INTFIX_TSMART_H
#define __INTFIX_TSMART_H

extern void pti_int_error(const char *) __attribute__((noreturn));

// exposed APIs

extern char pti_s_char(long long int x);

extern char pti_u_char(unsigned long long int x);

extern signed char pti_s_schar(long long int x);

extern signed char pti_u_schar(unsigned long long int x);

extern unsigned char pti_s_uchar(long long int x);

extern unsigned char pti_u_uchar(unsigned long long int x);

extern short int pti_s_short(long long int x);

extern short int pti_u_short(unsigned long long int x);

extern unsigned short int pti_s_ushort(long long int x);

extern unsigned short int pti_u_ushort(unsigned long long int x);

extern int pti_s_int(long long int x);

extern int pti_u_int(unsigned long long int x);

extern unsigned int pti_s_uint(long long int x);

extern unsigned int pti_u_uint(unsigned long long int x);

extern long int pti_s_lint(long long int x);

extern long int pti_u_lint(unsigned long long int x);

extern unsigned long int pti_s_ulint(long long int x);

extern unsigned long int pti_u_ulint(unsigned long long int x);

extern long long int pti_s_llint(long long int x);

extern long long int pti_u_llint(unsigned long long int x);

extern unsigned long long int pti_s_ullint(long long int x);

extern unsigned long long int pti_u_ullint(unsigned long long int x);

extern int pti_add_int(long long int x, long long int y, int mode);

extern unsigned int pti_add_uint(long long int x, long long int y, int mode);

extern long int pti_add_lint(long long int x, long long int y, int mode);

extern unsigned long int pti_add_ulint(long long int x, long long int y, int mode);

extern long long int pti_add_llint(long long int x, long long int y, int mode);

extern unsigned long long int pti_add_ullint(long long int x, long long int y, int mode);

extern int pti_minus_int(long long int x, long long int y, int mode);

extern unsigned int pti_minus_uint(long long int x, long long int y, int mode);

extern long int pti_minus_lint(long long int x, long long int y, int mode);

extern unsigned long int pti_minus_ulint(long long int x, long long int y, int mode);

extern long long int pti_minus_llint(long long int x, long long int y, int mode);

extern unsigned long long int pti_minus_ullint(long long int x, long long int y, int mode);

extern int pti_multiply_int(long long int x, long long int y, int mode);

extern unsigned int pti_multiply_uint(long long int x, long long int y, int mode);

extern long int pti_multiply_lint(long long int x, long long int y, int mode);

extern unsigned long int pti_multiply_ulint(long long int x, long long int y, int mode);

extern long long int pti_multiply_llint(long long int x, long long int y, int mode);

extern unsigned long long int pti_multiply_ullint(long long int x, long long int y, int mode);

// auxiliary functions

extern long long int pti_add_s(long long int x, long long int y, int mode, int * meta);

extern unsigned long long int pti_add_u(long long int x, long long int y, int mode, int * meta);

extern long long int pti_add_us_s(unsigned long long int lx, long long int ly, int * meta);

extern unsigned long long int pti_add_us_u(unsigned long long int lx, long long int ly, int * meta);

extern unsigned long long int pti_add_u_u(unsigned long long int lx, unsigned long long int ly, int * meta);

extern long long int pti_add_s_s(long long int lx, long long int ly, int * meta);

extern unsigned long long int pti_add_pn_u(long long int x, long long int y, int * meta);

extern long long int pti_minus_s(long long int x, long long int y, int mode, int * meta);

extern unsigned long long int pti_minus_u(long long int x, long long int y, int mode, int * meta);

extern long long int pti_minus_s_s(long long int lx, long long int ly, int * meta);

extern long long int pti_multiply_s(long long int x, long long int y, int mode, int * meta);

extern unsigned long long int pti_multiply_u(long long int x, long long int y, int mode, int * meta);

extern long long int pti_multiply_s_s(long long int x, long long int y, int * meta);

extern unsigned long long int pti_multiply_u_u(unsigned long long int x, unsigned long long int y, int * meta);

extern long long int pti_multiply_us_s(unsigned long long int lx, long long int ly, int * meta);

#endif // __INTFIX_TSMART_H
