/*
 * Tsmart-BD: The static analysis component of Tsmart platform
 *
 * Copyright (C) 2013-2017  Tsinghua University
 *
 * Open-source component:
 *
 * CPAchecker
 * Copyright (C) 2007-2014  Dirk Beyer
 *
 * Guava: Google Core Libraries for Java
 * Copyright (C) 2010-2006  Google
 *
 *
 */
package org.sosy_lab.cpachecker.cpa.range;

import static org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus.NAN;
import static org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus.NINF;
import static org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus.NORM;
import static org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus.PINF;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.annotation.Nonnull;

/**
 * An integer class that supports infinite value and NaN
 */
public final class CompInteger implements Comparable<CompInteger> {

  public enum IntegerStatus {
    /**
     * normal integer
     */
    NORM,
    /**
     * positive infinite value
     */
    PINF,
    /**
     * negative infinite value
     */
    NINF,
    /**
     * not a number, which is derived by division-by-zero or other illegal operations
     */
    NAN
  }

  private BigInteger value;

  private IntegerStatus status;

  private boolean isFloat = false;

  /**
   * Note: originally we use BigDecimal to represent the fractional part. Since the fractional
   * part ranges in (-1,1), double is sufficiently precise for these numbers.
   */
  private Double fraction = 0.0;

  /**
   * Some basic settings of floating point precision.
   */

  private static final int SCALE = 12;

  public CompInteger() {
    this.value = BigInteger.ZERO;
    this.status = NORM;
  }

  public CompInteger(IntegerStatus pStatus) {
    this.value = BigInteger.ZERO;
    this.status = pStatus;
  }

  public CompInteger(BigInteger val) {
    this.value = val;
    this.status = NORM;
  }

  public CompInteger(long val) {
    this.value = BigInteger.valueOf(val);
    this.status = NORM;
  }

  public CompInteger(Double dValue) {
    if (dValue.equals(Double.POSITIVE_INFINITY)) {
      this.value = BigInteger.ZERO;
      this.status = PINF;
    } else if (dValue.equals(Double.NEGATIVE_INFINITY)) {
      this.value = BigInteger.ZERO;
      this.status = NINF;
    } else if (dValue.equals(Double.NaN)) {
      this.value = BigInteger.ZERO;
      this.status = NAN;
    } else {
      this.status = NORM;
      BigDecimal decimal = BigDecimal.valueOf(dValue);
      this.value = decimal.toBigInteger();
      this.fraction = CompIntegers.getFraction(decimal);
      if (!CompIntegers.isAlmostZero(this.fraction)) {
        this.isFloat = true;
      }
    }
  }

  public CompInteger(BigDecimal decimal) {
    this.status = NORM;
    this.value = decimal.toBigInteger();
    this.fraction = CompIntegers.getFraction(decimal);
    if (!CompIntegers.isAlmostZero(this.fraction)) {
      this.isFloat = true;
    }
  }

  public CompInteger(BigInteger integer, Double fractional) {
    this.status = NORM;
    BigDecimal decimal = BigDecimal.valueOf(fractional);
    this.value = integer.add(decimal.toBigInteger());
    this.fraction = CompIntegers.getFraction(decimal);
    switch (CompIntegers.signum(this.fraction)) {
      case 1:
        isFloat = true;
        if (this.value.signum() < 0) {
          this.value = this.value.add(BigInteger.ONE);
          this.fraction = this.fraction - 1.0;
        }
        break;
      case -1:
        isFloat = true;
        if (this.value.signum() > 0) {
          this.value = this.value.subtract(BigInteger.ONE);
          this.fraction = this.fraction + 1.0;
        }
        break;
      default:
        isFloat = false;
    }
  }

  public IntegerStatus getStatus() {
    return status;
  }

  public BigInteger getValue() {
    return value;
  }

  public boolean isFloating() {
    return isFloat;
  }

  public static final CompInteger POSITIVE_INF = new CompInteger(PINF);
  public static final CompInteger NEGATIVE_INF = new CompInteger(NINF);
  public static final CompInteger NaN = new CompInteger(NAN);
  public static final CompInteger ZERO = new CompInteger(BigInteger.ZERO);
  public static final CompInteger ONE = new CompInteger(BigInteger.ONE);

  /**
   * Approximation of \pi is used in trigonometric arithmetic
   */
  static final CompInteger PI_LAPPROX = new CompInteger(Math.PI);

  public CompInteger add(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NORM) {
        return new CompInteger(this.value.add(that.value), this.fraction + that.fraction);
      } else {
        return new CompInteger(that.status);
      }
    } else if (this.status == NINF) {
      if (that.status == NORM || that.status == NINF) {
        return NEGATIVE_INF;
      } else {
        return NaN;
      }
    } else if (this.status == PINF) {
      if (that.status == NORM || that.status == PINF) {
        return POSITIVE_INF;
      } else {
        return NaN;
      }
    } else {
      return NaN;
    }
  }

  public CompInteger abs() {
    if (this.status == NORM) {
      return new CompInteger(this.value.abs(), Math.abs(this.fraction));
    } else if (this.status == PINF || this.status == NINF) {
      return POSITIVE_INF;
    } else {
      return NaN;
    }
  }

  public int signum() {
    if (this.status == NORM) {
      int intSig = this.value.signum();
      if (intSig != 0) {
        return intSig;
      }
      return CompIntegers.signum(this.fraction);
    } else if (this.status == NINF) {
      return -1;
    } else if (this.status == PINF) {
      return 1;
    } else {
      throw new RuntimeException("NaN has no sign");
    }
  }

  public CompInteger divide(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NAN) {
        return NaN;
      } else if (that.status == NINF || that.status == PINF) {
        return ZERO;
      } else {
        if (that.signum() == 0) {
          return NaN;
        } else {
          BigDecimal x = this.decimalValue();
          BigDecimal y = that.decimalValue();
          return new CompInteger(x.divide(y, SCALE, BigDecimal.ROUND_HALF_UP));
        }
      }
    } else if (this.status == NINF) {
      if (that.status == PINF || that.status == NINF || that.status == NAN) {
        return NaN;
      } else {
        switch (that.signum()) {
          case 0:
            return NaN;
          case -1:
            return POSITIVE_INF;
          default:
            // the signum() returns 1
            return NEGATIVE_INF;
        }
      }
    } else if (this.status == PINF) {
      if (that.status == PINF || that.status == NINF || that.status == NAN) {
        return NaN;
      } else {
        switch (that.signum()) {
          case 0:
            return NaN;
          case 1:
            return POSITIVE_INF;
          default:
            return NEGATIVE_INF;
        }
      }
    } else {
      return NaN;
    }
  }

  public CompInteger max(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NORM) {
        switch (this.value.compareTo(that.value)) {
          case 1:
            return this;
          case -1:
            return that;
          default: {
            switch (CompIntegers.signum(this.fraction - that.fraction)) {
              case 1:
                return this;
              default:
                return that;
            }
          }
        }
      } else if (that.status == PINF) {
        return POSITIVE_INF;
      } else {
        // NaN is incomparable with any other integers
        return this;
      }
    } else if (this.status == NINF) {
      if (that.status == NORM) {
        return that;
      } else if (that.status == PINF) {
        return POSITIVE_INF;
      } else {
        return NEGATIVE_INF;
      }
    } else if (this.status == PINF) {
      return POSITIVE_INF;
    } else {
      // this function returns NaN only if two operands are NaN
      return that;
    }
  }

  public CompInteger min(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NORM) {
        switch (this.value.compareTo(that.value)) {
          case 1:
            return that;
          case -1:
            return this;
          default: {
            switch (CompIntegers.signum(this.fraction - that.fraction)) {
              case 1:
                return that;
              default:
                return this;
            }
          }
        }
      } else if (that.status == NINF) {
        return NEGATIVE_INF;
      } else {
        return this;
      }
    } else if (this.status == NINF) {
      return NEGATIVE_INF;
    } else if (this.status == PINF) {
      if (that.status == NORM) {
        return that;
      } else if (that.status == NINF) {
        return NEGATIVE_INF;
      } else {
        return POSITIVE_INF;
      }
    } else {
      return that;
    }
  }

  public CompInteger mod(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NORM) {
        if (that.signum() == 0) {
          return NaN;
        } else {
          BigDecimal x = this.decimalValue();
          BigDecimal y = that.decimalValue();
          BigDecimal remainder = x.remainder(y);
          if (remainder.signum() < 0) {
            remainder = remainder.add(y);
          }
          return new CompInteger(remainder);
        }
      } else if (that.status == NINF || that.status == PINF) {
        return this;
      } else {
        return NaN;
      }
    } else {
      return NaN;
    }
  }

  public CompInteger multiply(CompInteger that) {
    if (this.status == NORM) {
      int sign = this.signum();
      if (that.status == NORM) {
        if (!this.isFloat && !that.isFloat) {
          BigInteger integer = this.value.multiply(that.value);
          return new CompInteger(integer);
        }
        BigDecimal x = this.decimalValue();
        BigDecimal y = that.decimalValue();
        return new CompInteger(x.multiply(y));
      } else if (that.status == NINF) {
        switch (sign) {
          case 1:
            return NEGATIVE_INF;
          case 0:
            return ZERO;
          default:
            return POSITIVE_INF;
        }
      } else if (that.status == PINF) {
        switch (sign) {
          case 1:
            return POSITIVE_INF;
          case 0:
            return ZERO;
          default:
            return NEGATIVE_INF;
        }
      } else {
        if (sign == 0) {
          return ZERO;
        } else {
          return NaN;
        }
      }
    } else if (this.status == NINF) {
      if (that.status == NORM) {
        switch (that.signum()) {
          case 0:
            return ZERO;
          case 1:
            return NEGATIVE_INF;
          default:
            return POSITIVE_INF;
        }
      } else if (that.status == NINF) {
        return POSITIVE_INF;
      } else if (that.status == PINF) {
        return NEGATIVE_INF;
      } else {
        return NaN;
      }
    } else if (this.status == PINF) {
      if (that.status == NORM) {
        switch (that.signum()) {
          case 0:
            return ZERO;
          case 1:
            return POSITIVE_INF;
          default:
            return NEGATIVE_INF;
        }
      } else if (that.status == NINF) {
        return NEGATIVE_INF;
      } else if (that.status == PINF) {
        return POSITIVE_INF;
      } else {
        return NaN;
      }
    } else {
      if (that.status == NORM && that.signum() == 0) {
        return ZERO;
      } else {
        return NaN;
      }
    }
  }

  public CompInteger negate() {
    if (this.status == NORM) {
      return new CompInteger(this.value.negate(), -this.fraction);
    } else if (this.status == NINF) {
      return POSITIVE_INF;
    } else if (this.status == PINF) {
      return NEGATIVE_INF;
    } else {
      return NaN;
    }
  }

  /**
   * Different from {@link CompInteger#mod(CompInteger)}, this function is equivalent to %
   * operator. Negative results are permitted.
   *
   * @param that the second operand of remainder operation
   * @return remainder result
   */
  public CompInteger remainder(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NORM) {
        if (that.signum() == 0) {
          return NaN;
        } else {
          BigDecimal x = this.decimalValue();
          BigDecimal y = that.decimalValue();
          return new CompInteger(x.remainder(y));
        }
      } else if (that.status == PINF) {
        return this;
      } else if (that.status == NINF) {
        return NEGATIVE_INF;
      } else {
        return NaN;
      }
    } else {
      return NaN;
    }
  }

  public CompInteger subtract(CompInteger that) {
    if (this.status == NORM) {
      if (that.status == NORM) {
        return new CompInteger(this.value.subtract(that.value), this.fraction - that.fraction);
      } else if (that.status == NINF) {
        return POSITIVE_INF;
      } else if (that.status == PINF) {
        return NEGATIVE_INF;
      } else {
        return NaN;
      }
    } else if (this.status == NINF) {
      if (that.status == NORM || that.status == PINF) {
        return NEGATIVE_INF;
      } else {
        return NaN;
      }
    } else if (this.status == PINF) {
      if (that.status == NORM || that.status == NINF) {
        return POSITIVE_INF;
      } else {
        return NaN;
      }
    } else {
      return NaN;
    }
  }

  public CompInteger shiftLeft(int n) {
    if (this.status == NORM) {
      try {
        // if current composite integer has non-negligible fractional part, then we simply
        // discard it
        if (n < 0) {
          n = 0;
        } else if (n > 64) {
          n = 64;
        }
        return new CompInteger(round().value.shiftLeft(n));
      } catch (ArithmeticException e) {
        if (this.value.signum() > 0) {
          return POSITIVE_INF;
        } else {
          return NEGATIVE_INF;
        }
      }
    } else {
      // no change for other cases
      return this;
    }
  }

  public CompInteger shiftRight(int n) {
    if (this.status == NORM) {
      try {
        return new CompInteger(round().value.shiftRight(n));
      } catch (ArithmeticException e) {
        // this is because n is negative
        if (this.value.signum() > 0) {
          return POSITIVE_INF;
        } else {
          return NEGATIVE_INF;
        }
      }
    } else {
      return this;
    }
  }

  public CompInteger floor() {
    if (!isFloat) {
      return new CompInteger(this.value);
    }
    return new CompInteger(this.value.subtract(BigInteger.ONE));
  }

  public CompInteger ceil() {
    if (!isFloat) {
      return new CompInteger(this.value);
    }
    return new CompInteger(this.value.add(BigInteger.ONE));
  }

  public CompInteger round() {
    if (!CompIntegers.overHalf(this.fraction)) {
      return new CompInteger(this.value);
    }
    if (CompIntegers.signum(this.fraction) > 0) {
      return new CompInteger(this.value.add(BigInteger.ONE));
    } else {
      return new CompInteger(this.value.subtract(BigInteger.ONE));
    }
  }

  public CompInteger trunc() {
    return new CompInteger(this.value);
  }

  /**
   * This function only extract the integer part as output.
   *
   * @return Integer value if it is in safe range.
   */
  public Integer intValue() {
    if (this.status == NORM) {
      BigInteger val = this.value;
      BigInteger int_max = BigInteger.valueOf(Integer.MAX_VALUE);
      BigInteger int_min = BigInteger.valueOf(Integer.MIN_VALUE);
      if (val.compareTo(int_max) <= 0 && val.compareTo(int_min) >= 0) {
        return val.intValue();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  public Long longValue() {
    if (this.status == NORM) {
      BigInteger val = this.value;
      BigInteger long_min = BigInteger.valueOf(Long.MIN_VALUE);
      BigInteger long_max = BigInteger.valueOf(Long.MAX_VALUE);
      if (val.compareTo(long_max) <= 0 && val.compareTo(long_min) >= 0) {
        return val.longValue();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @Nonnull
  public Double doubleValue() {
    if (this.status == NORM) {
      BigDecimal val = this.decimalValue();
      return val.doubleValue();
    } else if (this.status == PINF) {
      return Double.POSITIVE_INFINITY;
    } else if (this.status == NINF) {
      return Double.NEGATIVE_INFINITY;
    } else {
      return 0.0;
    }
  }

  /**
   * Convert a composite integer into decimal.
   * Note that BigDecimal cannot hold NaN or infinite values.
   *
   * @return decimal value of current composite number
   */
  private BigDecimal decimalValue() {
    if (this.status == NORM) {
      BigDecimal integral = new BigDecimal(this.value);
      BigDecimal fractional = BigDecimal.valueOf(this.fraction);
      return integral.add(fractional);
    } else if (this.status == NINF) {
      return BigDecimal.valueOf(Double.NEGATIVE_INFINITY);
    } else if (this.status == PINF) {
      return BigDecimal.valueOf(Double.POSITIVE_INFINITY);
    } else {
      return BigDecimal.valueOf(Double.NaN);
    }
  }

  @Override
  public String toString() {
    if (this.status == NORM) {
      if (!isFloat) {
        return this.value.toString();
      }
      return this.decimalValue().toString();
    } else if (this.status == NINF) {
      return "-inf";
    } else if (this.status == PINF) {
      return "+inf";
    } else {
      return "NaN";
    }
  }

  @Override
  public int compareTo(CompInteger that) {
    if (this.status == NINF) {
      if (that.status == NINF) {
        return 0;
      } else if (that.status == NAN) {
        throw new RuntimeException("NaN is not comparable to any other integers");
      } else {
        return -1;
      }
    } else if (this.status == PINF) {
      if (that.status == PINF) {
        return 0;
      } else if (that.status == NAN) {
        throw new RuntimeException("NaN is not comparable to any other integers");
      } else {
        return 1;
      }
    } else if (this.status == NAN) {
      throw new RuntimeException("NaN is not comparable to any other integers");
    } else {
      if (that.status == NINF) {
        return 1;
      } else if (that.status == PINF) {
        return -1;
      } else if (that.status == NORM) {
        int intCompare = this.value.compareTo(that.value);
        if (intCompare == 0) {
          return CompIntegers.signum(this.fraction - that.fraction);
        }
        return intCompare;
      } else {
        throw new RuntimeException("NaN is not comparable to any other integers");
      }
    }
  }

  public int compareTo(BigInteger biValue) {
    if (this.status == NINF) {
      return -1;
    } else if (this.status == PINF) {
      return 1;
    } else if (this.status == NORM) {
      int intCompare = this.value.compareTo(biValue);
      if (intCompare == 0) {
        return CompIntegers.signum(this.fraction);
      }
      return intCompare;
    } else {
      throw new RuntimeException("NaN is not comparable to any other integers");
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other != null && getClass().equals(other.getClass())) {
      CompInteger that = (CompInteger) other;
      if (this.status != that.status) {
        return false;
      }
      if (this.status == NORM) {
        return this.value.equals(that.value) && CompIntegers.signum(this.fraction - that
            .fraction) == 0;
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, fraction, isFloat, status);
  }
}
