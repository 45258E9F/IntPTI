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
package org.sosy_lab.cpachecker.cfa.simplification;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/**
 * This visitor visits an expression and evaluates it.
 * It tries to evaluate only the outermost operator,
 * i.e., it evaluates 1+2 to 3, but not (1+1)+1.
 * If evaluation is successful, it returns a CIntegerLiteralExpression with the new value,
 * otherwise it returns the original expression.
 */
public class NonRecursiveExpressionSimplificationVisitor extends ExpressionSimplificationVisitor {

  public NonRecursiveExpressionSimplificationVisitor(
      MachineModel mm,
      LogManagerWithoutDuplicates pLogger) {
    super(mm, pLogger);
  }

  /**
   * return a simplified version of the expression.
   * --> disabled, because this is the "non-recursive-visitor".
   */
  @Override
  protected CExpression recursive(CExpression expr) {
    return expr;
  }
}
