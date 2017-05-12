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
package org.sosy_lab.cpachecker.cfa.model;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class AssumeEdge extends AbstractCFAEdge {

  private final boolean truthAssumption;
  protected final AExpression expression;

  protected AssumeEdge(
      String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor,
      CFANode pSuccessor, AExpression pExpression, boolean pTruthAssumption) {

    super("[" + pRawStatement + "]", pFileLocation, pPredecessor, pSuccessor);
    truthAssumption = pTruthAssumption;
    expression = pExpression;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.AssumeEdge;
  }

  public boolean getTruthAssumption() {
    return truthAssumption;
  }

  public AExpression getExpression() {
    return expression;
  }

  @Override
  public String getCode() {
    if (truthAssumption) {
      return expression.toASTString();
    }
    return "!(" + expression.toASTString() + ")";
  }

  @Override
  public String getDescription() {
    return "[" + getCode() + "]";
  }

  /**
   * TODO
   * Warning: for instances with {@link #getTruthAssumption()} == false, the
   * return value of this method does not represent exactly the return value
   * of {@link #getRawStatement()} (it misses the outer negation of the expression).
   */
  @Override
  public Optional<? extends AExpression> getRawAST() {
    return Optional.of(expression);
  }
}
