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
package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Equivalence;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Helper methods for CType instances.
 */
public final class CTypes {

  private CTypes() {
  }

  private static class CanonicalCTypeEquivalence extends Equivalence<CType> {
    private static final CanonicalCTypeEquivalence INSTANCE = new CanonicalCTypeEquivalence();

    @Override
    protected boolean doEquivalent(CType pA, CType pB) {
      return pA.getCanonicalType().equals(pB.getCanonicalType());
    }

    @Override
    protected int doHash(CType pT) {
      return pT.getCanonicalType().hashCode();
    }
  }

  /**
   * Return an {@link Equivalence} based on the canonical type,
   * i.e., two types are defined as equal if their canonical types are equal.
   */
  public static Equivalence<CType> canonicalTypeEquivalence() {
    return CanonicalCTypeEquivalence.INSTANCE;
  }

  /**
   * Return a copy of a given type that has the "const" flag not set.
   * If the given type is already a non-const type, it is returned unchanged.
   *
   * This method only eliminates the outer-most const flag, if it is present,
   * i.e., it does not change a non-const pointer to a const int.
   */
  public static <T extends CType> T withoutConst(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (!type.isConst()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
        T result = (T) type.accept(ForceConstVisitor.FALSE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "const" flag set.
   * If the given type is already a const type, it is returned unchanged.
   *
   * This method only adds the outer-most const flag, if it is not present,
   * i.e., it does not change a const pointer to a non-const int.
   */
  public static <T extends CType> T withConst(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (type.isConst()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
        T result = (T) type.accept(ForceConstVisitor.TRUE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "volatile" flag not set.
   * If the given type is already a non-volatile type, it is returned unchanged.
   *
   * This method only eliminates the outer-most volatile flag, if it is present,
   * i.e., it does not change a non-volatile pointer to a volatile int.
   */
  public static <T extends CType> T withoutVolatile(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (!type.isVolatile()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
        T result = (T) type.accept(ForceVolatileVisitor.FALSE);
    return result;
  }

  /**
   * Return a copy of a given type that has the "volatile" flag set.
   * If the given type is already a volatile type, it is returned unchanged.
   *
   * This method only adds the outer-most volatile flag, if it is not present,
   * i.e., it does not change a volatile pointer to a non-volatile int.
   */
  public static <T extends CType> T withVolatile(T type) {
    if (type instanceof CProblemType) {
      return type;
    }

    if (type.isVolatile()) {
      return type;
    }
    @SuppressWarnings("unchecked") // Visitor always creates instances of exact same class
        T result = (T) type.accept(ForceVolatileVisitor.TRUE);
    return result;
  }

  private static enum ForceConstVisitor implements CTypeVisitor<CType, RuntimeException> {
    FALSE(false),
    TRUE(true);

    private final boolean constValue;

    private ForceConstVisitor(boolean pConstValue) {
      constValue = pConstValue;
    }

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(constValue, t.isVolatile(), t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(constValue, t.isVolatile(), t.getKind(), t.getMembers(),
          t.getName(), t.getOrigName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(constValue, t.isVolatile(), t.getKind(), t.getName(),
          t.getOrigName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(constValue, t.isVolatile(), t.getEnumerators(), t.getName(),
          t.getOrigName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      return new CFunctionType(constValue, t.isVolatile(), t.getReturnType(), t.getParameters(),
          t.takesVarArgs());
    }

    @Override
    public CPointerType visit(CPointerType t) {
      return new CPointerType(constValue, t.isVolatile(), t.getType());
    }

    @Override
    public CProblemType visit(CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(CSimpleType t) {
      return new CSimpleType(constValue, t.isVolatile(), t.getType(), t.isLong(), t.isShort(),
          t.isSigned(), t.isUnsigned(), t.isComplex(), t.isImaginary(), t.isLongLong());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(constValue, t.isVolatile(), t.getName(), t.getRealType());
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(constValue, t.isVolatile());
    }
  }

  private static enum ForceVolatileVisitor implements CTypeVisitor<CType, RuntimeException> {
    FALSE(false),
    TRUE(true);

    private final boolean volatileValue;

    private ForceVolatileVisitor(boolean pVolatileValue) {
      volatileValue = pVolatileValue;
    }

    // Make sure to always return instances of exactly the same classes!

    @Override
    public CArrayType visit(CArrayType t) {
      return new CArrayType(t.isConst(), volatileValue, t.getType(), t.getLength());
    }

    @Override
    public CCompositeType visit(CCompositeType t) {
      return new CCompositeType(t.isConst(), volatileValue, t.getKind(), t.getMembers(),
          t.getName(), t.getOrigName());
    }

    @Override
    public CElaboratedType visit(CElaboratedType t) {
      return new CElaboratedType(t.isConst(), volatileValue, t.getKind(), t.getName(),
          t.getOrigName(), t.getRealType());
    }

    @Override
    public CEnumType visit(CEnumType t) {
      return new CEnumType(t.isConst(), volatileValue, t.getEnumerators(), t.getName(),
          t.getOrigName());
    }

    @Override
    public CFunctionType visit(CFunctionType t) {
      return new CFunctionType(t.isConst(), volatileValue, t.getReturnType(), t.getParameters(),
          t.takesVarArgs());
    }

    @Override
    public CPointerType visit(CPointerType t) {
      return new CPointerType(t.isConst(), volatileValue, t.getType());
    }

    @Override
    public CProblemType visit(CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(CSimpleType t) {
      return new CSimpleType(t.isConst(), volatileValue, t.getType(), t.isLong(), t.isShort(),
          t.isSigned(), t.isUnsigned(), t.isComplex(), t.isImaginary(), t.isLongLong());
    }

    @Override
    public CTypedefType visit(CTypedefType t) {
      return new CTypedefType(t.isConst(), volatileValue, t.getName(), t.getRealType());
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(t.isConst(), volatileValue);
    }
  }

  private static List<Pair<CSimpleType, Pair<BigInteger, BigInteger>>> signedRangeMap = null;
  private static List<Pair<CSimpleType, Pair<BigInteger, BigInteger>>> unsignedRangeMap = null;

  /**
   * Merge two types with respect to the ranges of two types. If there is no existing type
   * capable to contain all the possible values, the merged type is NULL.
   */
  @Nullable
  public static CSimpleType mergeType(MachineModel model, CSimpleType t1, CSimpleType t2) {
    if (t1 == null || t2 == null) {
      return null;
    }
    if (t1.equals(t2)) {
      return t1;
    }
    // In general, floating type covers any integer type
    if (Types.isFloatType(t1) || Types.isFloatType(t2)) {
      // Since floating values do not have signedness, for each two types, one contains the other.
      BigInteger min1 = model.getMinimalIntegerValue(t1);
      BigInteger min2 = model.getMinimalIntegerValue(t2);
      BigInteger max1 = model.getMaximalIntegerValue(t1);
      BigInteger max2 = model.getMaximalIntegerValue(t2);
      if (min1.compareTo(min2) >= 0 && max1.compareTo(max2) <= 0) {
        return t2;
      } else {
        return t1;
      }
    }
    // case 2: both are integer types
    // STEP 1: determine the signedness. The merged type is unsigned only if both types are
    // unsigned.
    boolean unsigned = t1.isUnsigned() && t2.isUnsigned();
    BigInteger min = model.getMinimalIntegerValue(t1).min(model.getMinimalIntegerValue(t2));
    BigInteger max = model.getMaximalIntegerValue(t1).max(model.getMaximalIntegerValue(t2));
    // for each two signed/unsigned types, one contains the other
    if (unsigned) {
      if (unsignedRangeMap == null) {
        initializeUnsignedRangeMap(model);
      }
      return queryTypeByRange(unsignedRangeMap, min, max);
    } else {
      if (signedRangeMap == null) {
        initializeSignedRangeMap(model);
      }
      return queryTypeByRange(signedRangeMap, min, max);
    }
  }

  /**
   * Obtain the minimum type that can fully covers the given range [min, max].
   */
  @Nullable
  public static CSimpleType getTypeFromRange(MachineModel model, BigInteger min, BigInteger max) {
    // necessary initialization
    if (signedRangeMap == null) {
      initializeSignedRangeMap(model);
    }
    if (unsignedRangeMap == null) {
      initializeUnsignedRangeMap(model);
    }
    CSimpleType type = queryTypeByRange(signedRangeMap, min, max);
    if (type != null) {
      return type;
    }
    if (min.signum() >= 0) {
      // unsigned type is also possible
      return queryTypeByRange(unsignedRangeMap, min, max);
    }
    return null;
  }

  private static void initializeUnsignedRangeMap(MachineModel model) {
    unsignedRangeMap = new ArrayList<>();
    // examine the signedness of char, which is architecture-dependent
    BigInteger minChar = model.getMinimalIntegerValue(CNumericTypes.CHAR);
    if (minChar.signum() == 0) {
      unsignedRangeMap.add(Pair.of(CNumericTypes.CHAR, Pair.of(minChar, model
          .getMaximalIntegerValue(CNumericTypes.CHAR))));
    }
    unsignedRangeMap.add(Pair.of(CNumericTypes.UNSIGNED_CHAR,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.UNSIGNED_CHAR),
            model.getMaximalIntegerValue(CNumericTypes.UNSIGNED_CHAR))));
    unsignedRangeMap.add(Pair.of(CNumericTypes.UNSIGNED_SHORT_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.UNSIGNED_SHORT_INT),
            model.getMaximalIntegerValue(CNumericTypes.UNSIGNED_SHORT_INT))));
    unsignedRangeMap.add(Pair.of(CNumericTypes.UNSIGNED_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.UNSIGNED_INT),
            model.getMaximalIntegerValue(CNumericTypes.UNSIGNED_INT))));
    unsignedRangeMap.add(Pair.of(CNumericTypes.UNSIGNED_LONG_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.UNSIGNED_LONG_INT),
            model.getMaximalIntegerValue(CNumericTypes.UNSIGNED_LONG_INT))));
    unsignedRangeMap.add(Pair.of(CNumericTypes.UNSIGNED_LONG_LONG_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.UNSIGNED_LONG_LONG_INT),
            model.getMaximalIntegerValue(CNumericTypes.UNSIGNED_LONG_LONG_INT))));
  }

  private static void initializeSignedRangeMap(MachineModel model) {
    signedRangeMap = new ArrayList<>();
    BigInteger minChar = model.getMinimalIntegerValue(CNumericTypes.CHAR);
    if (minChar.signum() < 0) {
      signedRangeMap.add(Pair.of(CNumericTypes.CHAR, Pair.of(minChar, model
          .getMaximalIntegerValue(CNumericTypes.CHAR))));
    }
    signedRangeMap.add(Pair.of(CNumericTypes.SIGNED_CHAR,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.SIGNED_CHAR),
            model.getMaximalIntegerValue(CNumericTypes.SIGNED_CHAR))));
    signedRangeMap.add(Pair.of(CNumericTypes.SHORT_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.SHORT_INT),
            model.getMaximalIntegerValue(CNumericTypes.SHORT_INT))));
    signedRangeMap.add(Pair.of(CNumericTypes.INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.INT),
            model.getMaximalIntegerValue(CNumericTypes.INT))));
    signedRangeMap.add(Pair.of(CNumericTypes.LONG_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.LONG_INT),
            model.getMaximalIntegerValue(CNumericTypes.LONG_INT))));
    signedRangeMap.add(Pair.of(CNumericTypes.LONG_LONG_INT,
        Pair.of(model.getMinimalIntegerValue(CNumericTypes.LONG_LONG_INT),
            model.getMaximalIntegerValue(CNumericTypes.LONG_LONG_INT))));
  }

  @Nullable
  private static CSimpleType queryTypeByRange(
      List<Pair<CSimpleType, Pair<BigInteger, BigInteger>>> table, BigInteger lower, BigInteger
      upper) {
    for (Pair<CSimpleType, Pair<BigInteger, BigInteger>> entry : table) {
      CSimpleType type = entry.getFirstNotNull();
      BigInteger min = entry.getSecondNotNull().getFirstNotNull();
      BigInteger max = entry.getSecondNotNull().getSecondNotNull();
      if (lower.compareTo(min) >= 0 && upper.compareTo(max) <= 0) {
        return type;
      }
    }
    return null;
  }

}
