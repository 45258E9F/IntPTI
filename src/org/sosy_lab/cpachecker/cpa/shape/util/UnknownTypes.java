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


import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.math.BigInteger;

public final class UnknownTypes {

  /**
   * An anonymous type of N bytes is treated as a char array of length N bytes
   *
   * @param pSizeInBytes length of specified type
   * @return desired C type
   */
  public static CType createTypeWithLength(long pSizeInBytes) {
    CIntegerLiteralExpression arrayLen = new CIntegerLiteralExpression(FileLocation.DUMMY,
        CNumericTypes.UNSIGNED_LONG_INT, BigInteger.valueOf(pSizeInBytes));
    return new CArrayType(false, false, CNumericTypes.SIGNED_CHAR, arrayLen);
  }
}
