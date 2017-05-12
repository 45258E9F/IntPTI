#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

#define TSMART_FIX_INT_UNDET_VALUE 0

void tsmart_fix_int_error(const char * errmsg)
{
	fputs(errmsg, stderr);
	exit(1);
}

char tsmart_fix_int_s_char(long long int x)
{
	if(x >= (long long int) CHAR_MIN && x <= (long long int) CHAR_MAX)
	{
		return (char) x;
	}
	else
	{
		tsmart_fix_int_error("char conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

char tsmart_fix_int_u_char(long long unsigned int x)
{
	// unsigned value always has non-negative value
	if(x <= (long long unsigned int) CHAR_MAX)
	{
		return (char) x;
	}
	else
	{
		tsmart_fix_int_error("char conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

signed char tsmart_fix_int_s_schar(long long int x)
{
	if(x >= (long long int) SCHAR_MIN && x <= (long long int) SCHAR_MAX)
	{
		return (signed char) x;
	}
	else
	{
		tsmart_fix_int_error("schar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

signed char tsmart_fix_int_u_schar(long long unsigned int x)
{
	if(x <= (long long unsigned int) SCHAR_MAX)
	{
		return (signed char) x;
	}
	else
	{
		tsmart_fix_int_error("schar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned char tsmart_fix_int_s_uchar(long long int x)
{
	if(x >= (long long int) 0 && x <= (long long int) UCHAR_MAX) 
	{
		return (unsigned char) x;
	}
	else 
	{
		tsmart_fix_int_error("uchar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned char tsmart_fix_int_u_uchar(long long unsigned int x)
{
	if(x <= (long long unsigned int) UCHAR_MAX)
	{
		return (unsigned char) x;
	}
	else
	{
		tsmart_fix_int_error("uchar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

short tsmart_fix_int_s_short(long long int x)
{
	if(x >= (long long int) SHRT_MIN && x <= (long long int) SHRT_MAX)
	{
		return (short) x;
	}
	else
	{
		tsmart_fix_int_error("short conversion failed");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

short tsmart_fix_int_u_short(long long unsigned int x)
{
	if(x <= (long long unsigned int) SHRT_MAX)
	{
		return (short) x;
	}
	else
	{
		tsmart_fix_int_error("short conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned short tsmart_fix_int_s_ushort(long long int x)
{
	if(x >= (long long int) 0 && x <= (long long int) USHRT_MAX)
	{
		return (unsigned short) x;
	}
	else 
	{
		tsmart_fix_int_error("ushort conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned short tsmart_fix_int_u_ushort(long long unsigned int x)
{
	if(x <= (long long unsigned int) USHRT_MAX)
	{
		return (unsigned short) x;
	}
	else
	{
		tsmart_fix_int_error("ushort conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

int tsmart_fix_int_s_int(long long int x)
{
	if(x >= (long long int) INT_MIN && x <= (long long int) INT_MAX)
	{
		return (int) x;
	}
	else
	{
		tsmart_fix_int_error("int conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

int tsmart_fix_int_u_int(long long unsigned int x)
{
	if(x <= (long long unsigned int) INT_MAX)
	{
		return (int) x;
	}
	else
	{
		tsmart_fix_int_error("int conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned int tsmart_fix_int_s_uint(long long int x)
{
	if(x >= (long long int) 0 && x <= (long long int) UINT_MAX)
	{
		return (unsigned int) x;
	}
	else
	{
		tsmart_fix_int_error("uint conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned int tsmart_fix_int_u_uint(long long unsigned int x)
{
	if(x <= (long long unsigned int) UINT_MAX)
	{
		return (unsigned int) x;
	}
	else 
	{
		tsmart_fix_int_error("uint conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long tsmart_fix_int_s_lint(long long int x)
{
	if(x >= (long long int) LONG_MIN && x <= (long long int) LONG_MAX)
	{
		return (long) x;
	}
	else
	{
		tsmart_fix_int_error("long conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long tsmart_fix_int_u_lint(long long unsigned int x)
{
	if(x <= (long long unsigned int) LONG_MAX)
	{
		return (long) x;
	}
	else
	{
		tsmart_fix_int_error("long conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned long tsmart_fix_int_s_ulint(long long int x)
{
	// ULONG_MAX may not be representable under long long int in certain architectures
	// the sanitizing routine handles the following two cases:
	// (1) the size of `unsigned long` is equal to the size of `long long int`: no upperbound is specified
	// (2) otherwise (strictly less than): the normal sanitizing routine
	if(sizeof(unsigned long) == sizeof(long long int))
	{
		if(x >= (long long int) 0)
		{
			return (unsigned long) x;
		}
	} 
	else
	{
		if(x >= (long long int) 0 && x <= (long long int) ULONG_MAX)
		{
			return (unsigned long) x;
		}
	}
	tsmart_fix_int_error("ulong conversion failed\n");
	return TSMART_FIX_INT_UNDET_VALUE;
}

unsigned long tsmart_fix_int_u_ulint(long long unsigned int x)
{
	if(x <= (long long unsigned int) ULONG_MAX)
	{
		return (unsigned long) x;
	}
	else 
	{
		tsmart_fix_int_error("ulong conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long long int tsmart_fix_int_s_llint(long long int x)
{
	return x;
}

long long int tsmart_fix_int_u_llint(long long unsigned int x)
{
	if(x <= (long long unsigned int) LLONG_MAX)
	{
		return (long long int) x;
	}
	else
	{
		tsmart_fix_int_error("llong conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long long unsigned tsmart_fix_int_s_ullint(long long int x)
{
	if(x >= (long long int) 0)
	{
		return (long long unsigned) x;
	}
	else
	{
		tsmart_fix_int_error("ullong conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long long unsigned tsmart_fix_int_u_ullint(long long unsigned int x)
{
	return x;
}

long long int tsmart_fix_int_add_s(long long int x, long long int y)
{
	long long int sum = x + y;
	if ((x > 0 && y > 0 && sum < 0) || (x < 0 && y < 0 && sum > 0))
	{
		tsmart_fix_int_error("add-s overflow\n");
	}
	return sum;
}

long long unsigned int tsmart_fix_int_add_u(long long unsigned int x, long long unsigned int y)
{
	long long unsigned int sum = x + y;
	if (sum < x || sum < y)
	{
		tsmart_fix_int_error("add-u overflow\n");
	}
	return sum;
}

long long int tsmart_fix_int_minus_s(long long int x, long long int y)
{
	long long int delta = x - y;
	if ((x < 0 && y > 0 && delta > 0) || (x > 0 && y < 0 && delta < 0))
	{
		tsmart_fix_int_error("sub-s overflow\n");
	}
	return delta;
}

long long unsigned int tsmart_fix_int_minus_u(long long unsigned int x, long long unsigned int y)
{
	if (x < y)
	{
		tsmart_fix_int_error("sub-u overflow\n");
	}
	return (x - y);
}

long long int tsmart_fix_int_multiply_s(long long int x, long long int y)
{
	long long int product = x * y;
	if (product != 0 && product / x != y)
	{
		tsmart_fix_int_error("mul-s overflow\n");
	}
	return product;
}

long long unsigned int tsmart_fix_int_multiply_u(long long unsigned int x, long long unsigned int y)
{
	long long unsigned int product = x * y;
	if (product != 0 && product / x != y)
	{
		tsmart_fix_int_error("mul-u overflow\n");
	}
	return product;
}