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
package org.sosy_lab.cpachecker.cpa.shape.util;


import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class KnownTypes {
  /**
   * Create the "long long" type
   */
  public static CType createLongLongIntType() {
    return new CSimpleType(
        false,          // non const
        false,          // non volatile
        CBasicType.INT, // integer
        false,          // isLong
        false,          // isShort
        true,           // isSigned
        false,          // isUnsigned
        false,          // isComplex
        false,          // isImaginary
        true            // isLongLong
    );
  }

  /**
   * Create the "bool" type
   */
  public static CType createBoolIntType() {
    // TODO: check if this definition is correct? isShort?
    return new CSimpleType(
        false,          // non const
        false,          // non volatile
        CBasicType.BOOL, // integer
        false,          // isLong
        false,          // isShort
        false,          // isSigned
        true,           // isUnsigned
        false,          // isComplex
        false,          // isImaginary
        false           // isLongLong
    );
  }
}
