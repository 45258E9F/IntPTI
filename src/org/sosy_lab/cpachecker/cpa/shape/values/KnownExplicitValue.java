/*
 * IntPTI: integer error fixing by proper-type inference
 * Copyright (c) 2017.
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
package org.sosy_lab.cpachecker.cpa.shape.values;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;

import java.math.BigInteger;

/**
 * A convention: all value interfaces (and abstract classes) start with "Shape" while classes have
 * no such prefixes.
 */
public final class KnownExplicitValue extends ShapeKnownValue implements ShapeExplicitValue {

  private KnownExplicitValue(Number pValue, NumberKind pKind) {
    super(pValue, pKind);
  }

  public static final KnownExplicitValue ONE = new KnownExplicitValue(1, NumberKind.INT);

  public static final KnownExplicitValue ZERO = new KnownExplicitValue(0, NumberKind.INT);

  private static final int SIZE_OF_JAVA_LONG = 64;
  private static final int SIZE_OF_JAVA_FLOAT = 32;
  private static final int SIZE_OF_JAVA_DOUBLE = 64;

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof KnownExplicitValue)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    int result = 5;
    result = 31 * result + super.hashCode();
    return result;
  }

  @Override
  public ShapeExplicitValue add(ShapeExplicitValue pRValue) {
    if (pRValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    NumberKind thisKind = getKind();
    NumberKind thatKind = ((KnownExplicitValue) pRValue).getKind();
    if (thisKind == NumberKind.DOUBLE || thatKind == NumberKind.DOUBLE) {
      return valueOf(getAsDouble() + pRValue.getAsDouble());
    } else if (thisKind == NumberKind.FLOAT || thatKind == NumberKind.FLOAT) {
      return valueOf(getAsFloat() + pRValue.getAsFloat());
    } else {
      return valueOf(getAsBigInteger().add(((KnownExplicitValue) pRValue).getAsBigInteger()));
    }
  }

  @Override
  public ShapeExplicitValue subtract(ShapeExplicitValue pRValue) {
    if (pRValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    NumberKind thisKind = getKind();
    NumberKind thatKind = ((KnownExplicitValue) pRValue).getKind();
    if (thisKind == NumberKind.DOUBLE || thatKind == NumberKind.DOUBLE) {
      return valueOf(getAsDouble() - pRValue.getAsDouble());
    } else if (thisKind == NumberKind.FLOAT || thatKind == NumberKind.FLOAT) {
      return valueOf(getAsFloat() - pRValue.getAsFloat());
    } else {
      return valueOf(getAsBigInteger().subtract(((KnownExplicitValue) pRValue).getAsBigInteger()));
    }
  }

  @Override
  public ShapeExplicitValue multiply(ShapeExplicitValue pRValue) {
    if (pRValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    NumberKind thisKind = getKind();
    NumberKind thatKind = ((KnownExplicitValue) pRValue).getKind();
    if (thisKind == NumberKind.DOUBLE || thatKind == NumberKind.DOUBLE) {
      return valueOf(getAsDouble() * pRValue.getAsDouble());
    } else if (thisKind == NumberKind.FLOAT || thatKind == NumberKind.FLOAT) {
      return valueOf(getAsFloat() * pRValue.getAsFloat());
    } else {
      return valueOf(getAsBigInteger().multiply(((KnownExplicitValue) pRValue).getAsBigInteger()));
    }
  }

  @Override
  public ShapeExplicitValue divide(ShapeExplicitValue pRValue) {
    if (pRValue.isUnknown() || pRValue.equals(KnownExplicitValue.ZERO)) {
      return UnknownValue.getInstance();
    }
    NumberKind thisKind = getKind();
    NumberKind thatKind = ((KnownExplicitValue) pRValue).getKind();
    if (thisKind == NumberKind.DOUBLE || thatKind == NumberKind.DOUBLE) {
      return valueOf(getAsDouble() / pRValue.getAsDouble());
    } else if (thisKind == NumberKind.FLOAT || thatKind == NumberKind.FLOAT) {
      return valueOf(getAsFloat() / pRValue.getAsFloat());
    } else {
      return valueOf(getAsBigInteger().divide(((KnownExplicitValue) pRValue).getAsBigInteger()));
    }
  }

  @Override
  public ShapeExplicitValue shiftLeft(ShapeExplicitValue pRValue) {
    if (pRValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    NumberKind thisKind = getKind();
    NumberKind thatKind = ((KnownExplicitValue) pRValue).getKind();
    if (thatKind == NumberKind.INT) {
      if (thisKind == NumberKind.INT || thisKind == NumberKind.BIG_INT) {
        return valueOf(getAsBigInteger().shiftLeft(pRValue.getAsInt()));
      }
    }
    return UnknownValue.getInstance();
  }

  @Override
  public ShapeExplicitValue shiftRight(ShapeExplicitValue pRValue) {
    if (pRValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    NumberKind thisKind = getKind();
    NumberKind thatKind = ((KnownExplicitValue) pRValue).getKind();
    if (thatKind == NumberKind.INT) {
      if (thisKind == NumberKind.INT || thisKind == NumberKind.BIG_INT) {
        return valueOf(getAsBigInteger().shiftRight(pRValue.getAsInt()));
      }
    }
    return UnknownValue.getInstance();
  }

  @Override
  public KnownExplicitValue castValue(CType pType, MachineModel pMachineModel) {
    final CType type = pType.getCanonicalType();
    if (type instanceof CSimpleType) {
      CSimpleType sType = (CSimpleType) type;
      switch (sType.getType()) {
        case BOOL:
        case INT:
        case CHAR: {
          final int size = pMachineModel.getSizeofInBits(sType);
          final long value = getAsLong();
          final boolean isSigned = pMachineModel.isSigned(sType);
          if (size < SIZE_OF_JAVA_LONG) {
            long max = 1L << size;
            long result = value % max;
            if (isSigned) {
              if (result > (max >> 1) - 1) {
                result -= max;
              } else if (result < -(max >> 1)) {
                result += max;
              }
            } else {
              if (result < 0) {
                result += max;
              }
            }
            return KnownExplicitValue.valueOf(result);
          } else if (size == SIZE_OF_JAVA_LONG) {
            if (!isSigned && value < 0) {
              BigInteger result = BigInteger.valueOf(value);
              return KnownExplicitValue.valueOf(result.andNot(BigInteger.valueOf(-1).shiftLeft
                  (size)));
            }
            return KnownExplicitValue.valueOf(value);
          } else {
            // under the existing framework, this is impossible
            return this;
          }
        }
        case FLOAT: {
          final float value = getAsFloat();
          final int size = pMachineModel.getSizeofInBits(sType);
          if (size == SIZE_OF_JAVA_FLOAT || size == SIZE_OF_JAVA_DOUBLE) {
            return KnownExplicitValue.valueOf(value);
          } else {
            return this;
          }
        }
        case DOUBLE: {
          final double value = getAsDouble();
          final int size = pMachineModel.getSizeofInBits(sType);
          if (size == SIZE_OF_JAVA_DOUBLE) {
            return KnownExplicitValue.valueOf(value);
          } else if (size == SIZE_OF_JAVA_FLOAT) {
            return KnownExplicitValue.valueOf((float) value);
          } else {
            return this;
          }
        }
        default:
          return this;
      }
    } else {
      return this;
    }
  }

  /**
   * Derive the sign of the explicit value.
   *
   * @return 1 for positive, -1 for negative and 0 for zero.
   */
  public int signum() {
    switch (getKind()) {
      case DOUBLE:
      case FLOAT: {
        double innerValue = getAsDouble();
        if (CompIntegers.isAlmostZero(innerValue)) {
          return 0;
        } else {
          return innerValue < 0 ? -1 : 1;
        }
      }
      case BIG_INT:
      case INT: {
        BigInteger innerValue = getAsBigInteger();
        return innerValue.signum();
      }
      default:
        throw new IllegalStateException("impossible kind of explicit value");
    }
  }

  public static KnownExplicitValue valueOf(BigInteger pValue) {
    Preconditions.checkNotNull(pValue);
    if (pValue.equals(BigInteger.ZERO)) {
      return ZERO;
    } else if (pValue.equals(BigInteger.ONE)) {
      return ONE;
    } else {
      // downgrade the value type if possible
      if (pValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 &&
          pValue.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
        return new KnownExplicitValue(pValue.longValue(), NumberKind.INT);
      } else {
        return new KnownExplicitValue(pValue, NumberKind.BIG_INT);
      }
    }
  }

  public static KnownExplicitValue valueOf(long pValue) {
    if (pValue == 0) {
      return ZERO;
    } else if (pValue == 1) {
      return ONE;
    } else {
      return new KnownExplicitValue(pValue, NumberKind.INT);
    }
  }

  public static KnownExplicitValue valueOf(double pValue) {
    if (CompIntegers.isAlmostZero(pValue)) {
      return ZERO;
    } else if (CompIntegers.isAlmostZero(pValue - 1.0)) {
      return ONE;
    } else {
      return new KnownExplicitValue(pValue, NumberKind.DOUBLE);
    }
  }

  public static KnownExplicitValue valueOf(float pValue) {
    if (CompIntegers.isAlmostZero(pValue)) {
      return ZERO;
    } else if (CompIntegers.isAlmostZero(pValue - 1.0f)) {
      return ONE;
    } else {
      return new KnownExplicitValue(pValue, NumberKind.FLOAT);
    }
  }

  /**
   * To prevent name collision, we specify an alternative name for this method.
   */
  public static KnownExplicitValue of(Number pValue) {
    if (pValue instanceof Double) {
      return KnownExplicitValue.valueOf(pValue.doubleValue());
    } else if (pValue instanceof Float) {
      return KnownExplicitValue.valueOf(pValue.floatValue());
    } else if (pValue instanceof BigInteger) {
      return KnownExplicitValue.valueOf((BigInteger) pValue);
    } else {
      return KnownExplicitValue.valueOf(pValue.longValue());
    }
  }
}
