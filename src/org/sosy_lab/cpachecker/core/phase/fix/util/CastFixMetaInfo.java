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
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;
import org.sosy_lab.cpachecker.weakness.Weakness;

import javax.annotation.Nullable;

public class CastFixMetaInfo implements IntegerFixMetaInfo {

  private final Weakness defect;

  // for overflow
  private final BinaryOperator binOp;
  private final UnaryOperator unOp;
  private final Boolean isSigned;

  // for conversion
  private final CType sourceType;
  private final CType targetType;

  // shared by binary/unary/cast operation
  private String op1InStr = null;
  // only used for binary operation
  private String op2InStr = null;

  private CastFixMetaInfo(CExpression pExp, boolean pIsSigned) {
    defect = Weakness.INTEGER_OVERFLOW;
    if (pExp instanceof CBinaryExpression) {
      binOp = ((CBinaryExpression) pExp).getOperator();
      unOp = null;
      isSigned = pIsSigned;
    } else {
      assert (pExp instanceof CUnaryExpression);
      binOp = null;
      unOp = ((CUnaryExpression) pExp).getOperator();
      isSigned = pIsSigned;
    }
    sourceType = null;
    targetType = null;
  }

  private CastFixMetaInfo(CExpression pExp, CType pTargetType) {
    defect = Weakness.INTEGER_CONVERSION;
    sourceType = pExp.getExpressionType();
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

  public void setOp(String pOpInStr) {
    op1InStr = pOpInStr;
  }

  public void setOp(String pOp1, String pOp2) {
    op1InStr = pOp1;
    op2InStr = pOp2;
  }

  @Nullable
  public BinaryOperator getBinaryOperator() {
    return binOp;
  }

  @Nullable
  public UnaryOperator getUnaryOperator() {
    return unOp;
  }

  @Override
  public IntegerFixMode getMode() {
    return IntegerFixMode.CAST;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (defect == Weakness.INTEGER_OVERFLOW) {
      sb.append("\"_defect\":").append("\"").append("overflow").append("\"").append(",");
      if (binOp != null) {
        sb.append("\"_ary\":").append(2).append(",");
        assert (op1InStr != null && op2InStr != null);
        sb.append("\"_op1\":").append("\"").append(SourceStringInliner.inline(op1InStr)).append
            ("\"").append(",");
        sb.append("\"_op2\":").append("\"").append(SourceStringInliner.inline(op2InStr)).append
            ("\"").append(",");
        sb.append("\"_optr\":").append("\"").append(SourceStringInliner.inline(binOp.getOperator
            ())).append("\"").append(",");
      } else {
        sb.append("\"_ary\":").append(1).append(",");
        assert (op1InStr != null);
        sb.append("\"_op\":").append("\"").append(SourceStringInliner.inline(op1InStr)).append
            ("\"").append(",");
        sb.append("\"_optr\":").append("\"").append(SourceStringInliner.inline(unOp.getOperator()
        )).append("\"").append(",");
      }
      sb.append("\"_sign\":").append(isSigned ? 1 : 0);
    } else {
      sb.append("\"_defect\":").append("\"").append("conversion").append("\"").append(",");
      assert (sourceType != null && targetType != null);
      sb.append("\"_origin\":").append("\"").append(sourceType.toString()).append("\"").append(",");
      sb.append("\"_target\":").append("\"").append(targetType.toString()).append("\"").append(",");
      assert (op1InStr != null);
      sb.append("\"_op\":").append("\"").append(SourceStringInliner.inline(op1InStr)).append("\"");
    }
    return sb.toString();
  }
}
