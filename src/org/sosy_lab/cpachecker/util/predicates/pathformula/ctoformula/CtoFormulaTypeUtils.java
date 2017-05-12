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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

class CtoFormulaTypeUtils {

  public static boolean areEqualWithMatchingPointerArray(CType t1, CType t2) {
    if (t1 == null || t2 == null) {
      return t1 == t2;
    }

    t1 = t1.getCanonicalType();
    t2 = t2.getCanonicalType();

    return t1.equals(t2)
        || areMatchingPointerArrayTypes(t1, t2)
        || areMatchingPointerArrayTypes(t2, t1);
  }

  private static boolean areMatchingPointerArrayTypes(CType t1, CType t2) {
    if ((t1 instanceof CPointerType) && (t2 instanceof CArrayType)) {

      CType componentType1 = ((CPointerType) t1).getType();
      CType componentType2 = ((CArrayType) t2).getType();

      return (t1.isConst() == t2.isConst())
          && (t1.isVolatile() == t2.isVolatile())
          && componentType1.equals(componentType2);
    } else {
      return false;
    }
  }

  /**
   * CFieldReferences can be direct or indirect (pointer-dereferencing).
   * This method nests the owner in a CUnaryExpression if the access is indirect.
   */
  public static CExpression getRealFieldOwner(CFieldReference fExp)
      throws UnrecognizedCCodeException {
    CExpression fieldOwner = fExp.getFieldOwner();
    if (fExp.isPointerDereference()) {
      CType t = fieldOwner.getExpressionType().getCanonicalType();
      if (!(t instanceof CPointerType)) {
        throw new UnrecognizedCCodeException("Can't dereference a non-pointer in a field reference",
            fExp);
      }
      CType dereferencedType = ((CPointerType) t).getType();
      return new CPointerExpression(fExp.getFileLocation(), dereferencedType, fieldOwner);
    }
    return fieldOwner;
  }
}
