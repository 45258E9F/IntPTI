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

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.core.bugfix.MutableASTForFix;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;

public class ArithFixMetaInfo implements IntegerFixMetaInfo {

  private final String op1InStr;
  private final String op2InStr;
  private final int operator;
  private final boolean isSigned;

  private ArithFixMetaInfo(MutableASTForFix op1, MutableASTForFix op2, int op, boolean signed) {
    op1InStr = op1.synthesize();
    op2InStr = op2.synthesize();
    operator = op;
    isSigned = signed;
  }

  public static ArithFixMetaInfo of(MutableASTForFix op1, MutableASTForFix op2, int op, boolean
      signed) {
    return new ArithFixMetaInfo(op1, op2, op, signed);
  }

  @Override
  public IntegerFixMode getMode() {
    return IntegerFixMode.CHECK_ARITH;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\"_op1\":").append("\"").append(SourceStringInliner.inline(op1InStr)).append("\"")
        .append(",");
    sb.append("\"_op2\":").append("\"").append(SourceStringInliner.inline(op2InStr)).append("\"")
        .append(",");
    sb.append("\"_sign\":").append(isSigned ? 1 : 0).append(",");
    String opStr;
    switch (operator) {
      case IASTBinaryExpression.op_plus:
        opStr = "addition";
        break;
      case IASTBinaryExpression.op_minus:
        opStr = "subtraction";
        break;
      default:
        opStr = "multiplication";
    }
    sb.append("\"_optr\":").append("\"").append(opStr).append("\"");
    return sb.toString();
  }
}
