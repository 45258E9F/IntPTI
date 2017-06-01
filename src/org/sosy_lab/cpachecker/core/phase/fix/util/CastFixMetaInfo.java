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
package org.sosy_lab.cpachecker.core.phase.fix.util;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.weakness.Weakness;

public class CastFixMetaInfo implements IntegerFixMetaInfo {

  private final Weakness defect;
  private final CExpression expression;

  // for overflow
  private final BinaryOperator binOp;
  private final UnaryOperator unOp;
  private final Boolean isSigned;

  // for conversion
  private final CType sourceType;
  private final CType targetType;

  private CastFixMetaInfo(CExpression pExp, boolean pIsSigned) {
    defect = Weakness.INTEGER_OVERFLOW;
    expression = pExp;
    if (expression instanceof CBinaryExpression) {
      binOp = ((CBinaryExpression) expression).getOperator();
      unOp = null;
      isSigned = pIsSigned;
    } else {
      assert (expression instanceof CUnaryExpression);
      binOp = null;
      unOp = ((CUnaryExpression) expression).getOperator();
      isSigned = pIsSigned;
    }
    sourceType = null;
    targetType = null;
  }

  private CastFixMetaInfo(CExpression pExp, CType pTargetType) {
    defect = Weakness.INTEGER_CONVERSION;
    expression = pExp;
    sourceType = expression.getExpressionType();
    targetType = pTargetType;
    binOp = null;
    unOp = null;
    isSigned = null;
  }

  public static CastFixMetaInfo overflowOf(CExpression pExp, boolean pSigned) {
    return new CastFixMetaInfo(pExp, pSigned);
  }

  public static CastFixMetaInfo convertOf(CExpression pExp, CType pTargetType) {
    return new CastFixMetaInfo(pExp, pTargetType);
  }

}
