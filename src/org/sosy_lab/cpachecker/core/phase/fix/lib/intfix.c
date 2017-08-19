#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

#define TSMART_FIX_INT_UNDET_VALUE 0
#define VALID_RESULT 1
#define INVALID_RESULT -1

void pti_int_error(const char * errmsg)
{
	fputs(errmsg, stderr);
	exit(1);
}

char pti_s_char(long long int x)
{
	if(x >= (long long int) CHAR_MIN && x <= (long long int) CHAR_MAX)
	{
		return (char) x;
	}
	else
	{
		pti_int_error("char conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

char pti_u_char(unsigned long long int x)
{
	// unsigned value always has non-negative value
	if(x <= (unsigned long long int) CHAR_MAX)
	{
		return (char) x;
	}
	else
	{
		pti_int_error("char conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

signed char pti_s_schar(long long int x)
{
	if(x >= (long long int) SCHAR_MIN && x <= (long long int) SCHAR_MAX)
	{
		return (signed char) x;
	}
	else
	{
		pti_int_error("schar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

signed char pti_u_schar(unsigned long long int x)
{
	if(x <= (unsigned long long int) SCHAR_MAX)
	{
		return (signed char) x;
	}
	else
	{
		pti_int_error("schar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned char pti_s_uchar(long long int x)
{
	if(x >= (long long int) 0 && x <= (long long int) UCHAR_MAX) 
	{
		return (unsigned char) x;
	}
	else 
	{
		pti_int_error("uchar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned char pti_u_uchar(unsigned long long int x)
{
	if(x <= (unsigned long long int) UCHAR_MAX)
	{
		return (unsigned char) x;
	}
	else
	{
		pti_int_error("uchar conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

short pti_s_short(long long int x)
{
	if(x >= (long long int) SHRT_MIN && x <= (long long int) SHRT_MAX)
	{
		return (short) x;
	}
	else
	{
		pti_int_error("short conversion failed");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

short pti_u_short(unsigned long long int x)
{
	if(x <= (unsigned long long int) SHRT_MAX)
	{
		return (short) x;
	}
	else
	{
		pti_int_error("short conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned short pti_s_ushort(long long int x)
{
	if(x >= (long long int) 0 && x <= (long long int) USHRT_MAX)
	{
		return (unsigned short) x;
	}
	else 
	{
		pti_int_error("ushort conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned short pti_u_ushort(unsigned long long int x)
{
	if(x <= (unsigned long long int) USHRT_MAX)
	{
		return (unsigned short) x;
	}
	else
	{
		pti_int_error("ushort conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

int pti_s_int(long long int x)
{
	if(x >= (long long int) INT_MIN && x <= (long long int) INT_MAX)
	{
		return (int) x;
	}
	else
	{
		pti_int_error("int conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

int pti_u_int(unsigned long long int x)
{
	if(x <= (unsigned long long int) INT_MAX)
	{
		return (int) x;
	}
	else
	{
		pti_int_error("int conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned int pti_s_uint(long long int x)
{
	if(x >= (long long int) 0 && x <= (long long int) UINT_MAX)
	{
		return (unsigned int) x;
	}
	else
	{
		pti_int_error("uint conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned int pti_u_uint(unsigned long long int x)
{
	if(x <= (unsigned long long int) UINT_MAX)
	{
		return (unsigned int) x;
	}
	else 
	{
		pti_int_error("uint conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long pti_s_lint(long long int x)
{
	if(x >= (long long int) LONG_MIN && x <= (long long int) LONG_MAX)
	{
		return (long) x;
	}
	else
	{
		pti_int_error("long conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long pti_u_lint(unsigned long long int x)
{
	if(x <= (unsigned long long int) LONG_MAX)
	{
		return (long) x;
	}
	else
	{
		pti_int_error("long conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned long pti_s_ulint(long long int x)
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
	pti_int_error("ulong conversion failed\n");
	return TSMART_FIX_INT_UNDET_VALUE;
}

unsigned long pti_u_ulint(unsigned long long int x)
{
	if(x <= (unsigned long long int) ULONG_MAX)
	{
		return (unsigned long) x;
	}
	else 
	{
		pti_int_error("ulong conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long long int pti_s_llint(long long int x)
{
	return x;
}

long long int pti_u_llint(unsigned long long int x)
{
	if(x <= (unsigned long long int) LLONG_MAX)
	{
		return (long long int) x;
	}
	else
	{
		pti_int_error("llong conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long long unsigned pti_s_ullint(long long int x)
{
	if(x >= (long long int) 0)
	{
		return (long long unsigned) x;
	}
	else
	{
		pti_int_error("ullong conversion failed\n");
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

long long unsigned pti_u_ullint(unsigned long long int x)
{
	return x;
}

// declarations of auxiliary functions

long long int pti_add_us_s(unsigned long long int lx, long long int ly, int * meta);

unsigned long long int pti_add_us_u(unsigned long long int lx, long long int ly, int * meta);

unsigned long long int pti_add_u_u(unsigned long long int lx, unsigned long long int ly, int * meta);

long long int pti_add_s_s(long long int lx, long long int ly, int * meta);

unsigned long long int pti_add_pn_u(long long int x, long long int y, int * meta);

long long int pti_minus_s_s(long long int lx, long long int ly, int * meta);

long long int pti_multiply_s_s(long long int x, long long int y, int * meta);

unsigned long long int pti_multiply_u_u(unsigned long long int x, unsigned long long int y, int * meta);

long long int pti_multiply_us_s(unsigned long long int lx, long long int ly, int * meta);

long long int pti_add_s(long long int x, long long int y, int mode, int * meta)
{
	if (mode == 0)
	{
		unsigned long long int lx = (unsigned long long int) x;
		unsigned long long int ly = (unsigned long long int) y;
		unsigned long long int sum = lx + ly;
		if (sum < lx || sum < ly)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		if (sum <= (unsigned long long int) LLONG_MAX)
		{
			*meta = VALID_RESULT;
			return (long long int) sum;
		}
		else
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
	}
	else if (mode == 1)
	{
		unsigned long long int lx = (unsigned long long int) x;
		long long int ly = y;
		return pti_add_us_s(lx, ly, meta);
	}
	else if (mode == 2)
	{
		long long int lx = x;
		unsigned long long int ly = (unsigned long long int) y;
		return pti_add_us_s(ly, lx, meta);
	}
	else
	{
		return pti_add_s_s(x, y, meta);
	}
}

long long int pti_add_us_s(unsigned long long int lx, long long int ly, int * meta)
{
	unsigned long long int delta = 0;
	if (ly >= 0)
	{
		delta = LLONG_MAX - ly;
	}
	else
	{
		delta = (unsigned long long int) LLONG_MAX;
		if (ly == LLONG_MIN)
		{
			delta = delta * 2 + 1;
		}
		else
		{
			delta = delta + (unsigned long long int)(-ly);
		}
	}
	if (lx > delta)
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	else
	{
		*meta = VALID_RESULT;
		return (LLONG_MAX - (delta - lx));
	}
}

unsigned long long int pti_add_u(long long int x, long long int y, int mode, int * meta)
{
	if (mode == 0)
	{
		unsigned long long int lx = (unsigned long long int) x;
		unsigned long long int ly = (unsigned long long int) y;
		return pti_add_u_u(lx, ly, meta);
	}
	else if (mode == 1)
	{
		unsigned long long int lx = (unsigned long long int) x;
		long long int ly = y;
		return pti_add_us_u(lx, ly, meta);
	}
	else if (mode == 2)
	{
		long long int lx = x;
		unsigned long long int ly = (unsigned long long int) y;
		return pti_add_us_u(ly, lx, meta);
	}
	else
	{
		if (x >= 0)
		{
			if (y >= 0)
			{
				*meta = VALID_RESULT;
				return (unsigned long long int) x + (unsigned long long int) y;
			}
			else
			{
				return pti_add_pn_u(x, y, meta);
			}
		}
		else
		{
			if (y >= 0)
			{
				return pti_add_pn_u(y, x, meta);
			}
			else
			{
				*meta = INVALID_RESULT;
				return TSMART_FIX_INT_UNDET_VALUE;
			}
		}
	}
}

unsigned long long int pti_add_us_u(unsigned long long int lx, long long int ly, int * meta)
{
	unsigned long long int delta = 0;
	if (ly >= 0)
	{
		delta = ly;
		return pti_add_u_u(lx, delta, meta);
	}
	else if (ly == LLONG_MIN)
	{
		delta = (unsigned long long int) LLONG_MAX + 1;
	}
	else
	{
		delta = (unsigned long long int) -ly;
	}
	if (lx < delta)
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return (lx - delta);
}

unsigned long long int pti_add_pn_u(long long int x, long long int y, int * meta)
{
	unsigned long long int lx = (unsigned long long int) x;
	unsigned long long int ly = 0;
	if (y == LLONG_MIN)
	{
		ly = (unsigned long long int) LLONG_MAX + 1;
	}
	else
	{
		ly = (unsigned long long int) -y;
	}
	if (lx < ly)
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return (lx - ly);
}

unsigned long long int pti_add_u_u(unsigned long long int lx, unsigned long long int ly, int * meta)
{
	unsigned long long int sum = lx + ly;
	if (sum < lx || sum < ly)
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return sum;
}

long long int pti_add_s_s(long long int lx, long long int ly, int * meta)
{
	long long int sum = lx + ly;
	if ((lx > 0 && ly > 0 && sum < 0) || (lx < 0 && ly < 0 && sum > 0))
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return sum;
}

long long int pti_minus_s(long long int x, long long int y, int mode, int * meta)
{
	if (mode == 0)
	{
		unsigned long long int lx = (unsigned long long int) x;
		unsigned long long int ly = (unsigned long long int) y;
		if (lx < ly)
		{
			unsigned long long int delta = ly - lx;
			if (delta > (unsigned long long int) LLONG_MAX + 1)
			{
				*meta = INVALID_RESULT;
				return TSMART_FIX_INT_UNDET_VALUE;
			}
			*meta = VALID_RESULT;
			return (0 - (long long int) delta);
		}
		else
		{
			unsigned long long int delta = lx - ly;
			if (delta > (unsigned long long int) LLONG_MAX)
			{
				*meta = INVALID_RESULT;
				return TSMART_FIX_INT_UNDET_VALUE;
			}
			*meta = VALID_RESULT;
			return (long long int) delta;
		}
	}
	else if (mode == 1)
	{
		unsigned long long int lx = (unsigned long long int) x;
		long long int ly = y;
		if (ly == LLONG_MIN)
		{
			unsigned long long int oppo_y = (unsigned long long int) LLONG_MAX + 1;
			unsigned long long int result = pti_add_u_u(lx, oppo_y, meta);
			if (*meta == VALID_RESULT)
			{
				if (result <= (unsigned long long int) LLONG_MAX)
				{
					return (long long int) result;
				}
				*meta = INVALID_RESULT;
				return TSMART_FIX_INT_UNDET_VALUE;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			long long int oppo_y = -ly;
			return pti_add_us_s(lx, oppo_y, meta);
		}
	}
	else if (mode == 2)
	{
		long long int lx = x;
		unsigned long long int ly = (unsigned long long int) y;
		unsigned long long int delta_x = 0;
		if (lx < 0)
		{
			delta_x = (unsigned long long int) (lx - LLONG_MIN);
		}
		else 
		{
			delta_x = (unsigned long long int) LLONG_MAX + 1;
			delta_x = delta_x + lx;
		}
		if (ly > delta_x)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		*meta = VALID_RESULT;
		return (long long int) ((delta_x - ly) + (unsigned long long int) LLONG_MIN);
	}
	else
	{
		return pti_minus_s_s(x, y, meta);
	}
}

unsigned long long int pti_minus_u(long long int x, long long int y, int mode, int * meta)
{
	if (mode == 0)
	{
		unsigned long long int lx = (unsigned long long int) x;
		unsigned long long int ly = (unsigned long long int) y;
		if (lx < ly)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		*meta = VALID_RESULT;
		return lx - ly;
	}
	else if (mode == 1)
	{
		unsigned long long int lx = (unsigned long long int) x;
		long long int ly = y;
		if (ly == LLONG_MIN)
		{
			unsigned long long int oppo_y = (unsigned long long int) LLONG_MAX + 1;
			return pti_add_u_u(lx, oppo_y, meta);
		}
		else
		{
			long long int oppo_y = -ly;
			return pti_add_us_u(lx, oppo_y, meta);
		}
	}
	else if (mode == 2)
	{
		long long int lx = x;
		unsigned long long int ly = (unsigned long long int) y;
		if (lx < 0)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		unsigned long long int px = lx;
		if (px < ly)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		*meta = VALID_RESULT;
		return (px - ly);
	}
	else
	{
		if (y >= 0)
		{
			long long int oppo_y = -y;
			if (x >= 0)
			{
				return pti_add_pn_u(x, oppo_y, meta);
			}
			else
			{
				*meta = INVALID_RESULT;
				return TSMART_FIX_INT_UNDET_VALUE;
			}
		}
		else 
		{
			unsigned long long int oppo_y = 0;
			if (y == LLONG_MIN)
			{
				oppo_y = (unsigned long long int) LLONG_MAX + 1;
			}
			else
			{
				oppo_y = (unsigned long long int) (-y);
			}
			return pti_add_us_u(oppo_y, x, meta);
		}
	}
}

long long int pti_minus_s_s(long long int lx, long long int ly, int * meta)
{
	long long int delta = lx - ly;
	if ((lx < 0 && ly > 0 && delta > 0) || (lx > 0 && ly < 0 && delta < 0))
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return delta;
}

long long int pti_multiply_s(long long int x, long long int y, int mode, int * meta)
{
	if (mode == 0)
	{
		unsigned long long int lx = (unsigned long long int) x;
		unsigned long long int ly = (unsigned long long int) y;
		unsigned long long int result = pti_multiply_u_u(lx, ly, meta);
		if (*meta == VALID_RESULT)
		{
			if (result > (unsigned long long int) LLONG_MAX)
			{
				*meta = INVALID_RESULT;
				return TSMART_FIX_INT_UNDET_VALUE;
			}
			return (long long int) result;
		}
		else
		{
			return 0;
		}
	}
	else if (mode == 1)
	{
		unsigned long long int lx = (unsigned long long int) x;
		return pti_multiply_us_s(lx, y, meta);
	}
	else if (mode == 2)
	{
		unsigned long long int ly = (unsigned long long int) y;
		return pti_multiply_us_s(ly, x, meta);
	}
	else
	{
		return pti_multiply_s_s(x, y, meta);
	}
}

long long int pti_multiply_us_s(unsigned long long int lx, long long int ly, int * meta)
{
	if (lx == 0 || ly == 0)
	{
		*meta = VALID_RESULT;
		return 0;
	}
	else if (ly == -1)
	{
		if (lx <= (unsigned long long int) LLONG_MAX + 1)
		{
			*meta = VALID_RESULT;
			return (long long int) ((unsigned long long int) 0 - lx);
		}
		else
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
	}
	else
	{
		if (lx <= (unsigned long long int) LLONG_MAX)
		{
			long long int llx = (long long int) lx;
			return pti_multiply_s_s(llx, ly, meta);
		}
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
}

unsigned long long int pti_multiply_u(long long int x, long long int y, int mode, int * meta)
{
	if (mode == 0)
	{
		unsigned long long int lx = (unsigned long long int) x;
		unsigned long long int ly = (unsigned long long int) y;
		return pti_multiply_u_u(lx, ly, meta);
	}
	else if (mode == 1)
	{
		unsigned long long int lx = (unsigned long long int) x;
		if (y < 0)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		unsigned long long int ly = (unsigned long long int) y;
		return pti_multiply_u_u(lx, ly, meta);
	}
	else if (mode == 2)
	{
		unsigned long long int ly = (unsigned long long int) y;
		if (x < 0)
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		unsigned long long int lx = (unsigned long long int) x;
		return pti_multiply_u_u(lx, ly, meta);
	}
	else
	{
		if ((x > 0 && y < 0) || (x < 0 && y > 0))
		{
			*meta = INVALID_RESULT;
			return TSMART_FIX_INT_UNDET_VALUE;
		}
		if (x > 0)
		{
			unsigned long long int lx = (unsigned long long int) x;
			unsigned long long int ly = (unsigned long long int) y;
			return pti_multiply_u_u(lx, ly, meta);
		}
		else
		{
			unsigned long long int lx, ly;
			if (x == LLONG_MIN)
			{
				lx = (unsigned long long int) LLONG_MAX + 1;
			}
			else
			{
				lx = (unsigned long long int) -x;
			}
			if (y == LLONG_MIN)
			{
				ly = (unsigned long long int) LLONG_MAX + 1;
			}
			else
			{
				ly = (unsigned long long int) -y;
			}
			return pti_multiply_u_u(lx, ly, meta);
		}
	}
}

long long int pti_multiply_s_s(long long int x, long long int y, int * meta)
{
	long long int product = x * y;
	if (product != 0 && product / x != y)
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return product;
}

unsigned long long int pti_multiply_u_u(unsigned long long int x, unsigned long long int y, int * meta)
{
	unsigned long long int product = x * y;
	if (product != 0 && product / x != y)
	{
		*meta = INVALID_RESULT;
		return TSMART_FIX_INT_UNDET_VALUE;
	}
	*meta = VALID_RESULT;
	return product;
}

int pti_add_int(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_add_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("add overflow\n");
	}
	return pti_s_int(result);
}

unsigned int pti_add_uint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_add_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("add overflow\n");
	}
	return pti_u_uint(result);
}

long int pti_add_lint(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_add_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("add overflow\n");
	}
	return pti_s_lint(result);
}

unsigned long int pti_add_ulint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_add_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("add overflow\n");
	}
	return pti_u_ulint(result);
}

long long int pti_add_llint(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_add_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("add overflow\n");
	}
	return result;
}

unsigned long long int pti_add_ullint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_add_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("add overflow\n");
	}
	return result;
}

int pti_minus_int(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_minus_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("minus overflow\n");
	}
	return pti_s_int(result);
}

unsigned int pti_minus_uint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_minus_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("minus overflow\n");
	}
	return pti_u_uint(result);
}

long int pti_minus_lint(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_minus_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("minus overflow\n");
	}
	return pti_s_lint(result);
}

unsigned long int pti_minus_ulint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_minus_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("minus overflow\n");
	}
	return pti_u_ulint(result);
}

long long int pti_minus_llint(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_minus_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("minus overflow\n");
	}
	return result;
}

unsigned long long int pti_minus_ullint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_minus_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("minus overflow\n");
	}
	return result;
}

int pti_multiply_int(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_multiply_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("multiply overflow\n");
	}
	return pti_s_int(result);
}

unsigned int pti_multiply_uint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_multiply_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("multiply overflow\n");
	}
	return pti_u_uint(result);
}

long int pti_multiply_lint(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_multiply_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("multiply overflow\n");
	}
	return pti_s_lint(result);
}

unsigned long int pti_multiply_ulint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_multiply_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("multiply overflow\n");
	}
	return pti_u_ulint(result);
}

long long int pti_multiply_llint(long long int x, long long int y, int mode)
{
	int meta = 0;
	long long int result = pti_multiply_s(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("multiply overflow\n");
	}
	return result;
}

unsigned long long int pti_multiply_ullint(long long int x, long long int y, int mode)
{
	int meta = 0;
	unsigned long long int result = pti_multiply_u(x, y, mode, &meta);
	if (meta == INVALID_RESULT)
	{
		pti_int_error("multiply overflow\n");
	}
	return result;
}