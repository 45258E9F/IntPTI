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
package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;

/**
 * Like toASTString, but with original names.
 *
 * NOT necessarily equivalent to specific parts of the original code file.
 */
enum CStatementToOriginalCodeVisitor implements CStatementVisitor<String, RuntimeException> {

  INSTANCE;

  @Override
  public String visit(CExpressionStatement pIastExpressionStatement) {
    return pIastExpressionStatement.getExpression().accept(CExpressionToOrinalCodeVisitor.INSTANCE)
        + ";";
  }

  @Override
  public String visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {

    CExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor =
        CExpressionToOrinalCodeVisitor.INSTANCE;

    String leftHandSide =
        pIastExpressionAssignmentStatement.getLeftHandSide().accept(expressionToOrinalCodeVisitor);
    String rightHandSide =
        pIastExpressionAssignmentStatement.getRightHandSide().accept(expressionToOrinalCodeVisitor);

    return leftHandSide + " == " + rightHandSide + "; ";
  }

  @Override
  public String visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {

    CExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor =
        CExpressionToOrinalCodeVisitor.INSTANCE;

    String leftHandSide = pIastFunctionCallAssignmentStatement.getLeftHandSide()
        .accept(expressionToOrinalCodeVisitor);
    String rightHandSide = handleFunctionCallExpression(
        pIastFunctionCallAssignmentStatement.getFunctionCallExpression());

    return leftHandSide
        + " == "
        + rightHandSide
        + "; ";
  }

  @Override
  public String visit(CFunctionCallStatement pIastFunctionCallStatement) {
    return handleFunctionCallExpression(pIastFunctionCallStatement.getFunctionCallExpression())
        + ";";
  }

  private static String handleFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(parenthesize(pFunctionCallExpression.getFunctionNameExpression()));
    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString,
        transform(pFunctionCallExpression.getParameterExpressions(),
            new Function<CExpression, String>() {

              @Override
              public String apply(CExpression pInput) {
                return pInput.accept(CExpressionToOrinalCodeVisitor.INSTANCE);
              }
            }));
    lASTString.append(")");

    return lASTString.toString();
  }

  static String parenthesize(String pInput) {
    return "(" + pInput + ")";
  }

  static String parenthesize(CExpression pInput) {
    String result = pInput.accept(CExpressionToOrinalCodeVisitor.INSTANCE);
    if (pInput instanceof CIdExpression) {
      return result;
    }
    return parenthesize(result);
  }

}
