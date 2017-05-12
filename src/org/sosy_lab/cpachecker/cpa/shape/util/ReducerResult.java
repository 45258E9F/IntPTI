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

import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;

/**
 * The result of symbolic reducer consists of two parts: (1) (possibly altered) symbolic
 * expression; (2) the flag indicating whether the symbolic expression is changed.
 */
public class ReducerResult {

  private final boolean changeFlag;
  private final ConstraintRepresentation se;

  private ReducerResult(boolean pFlag, ConstraintRepresentation pSE) {
    changeFlag = pFlag;
    se = pSE;
  }

  public static ReducerResult of(boolean pFlag, ConstraintRepresentation pSE) {
    return new ReducerResult(pFlag, pSE);
  }

  public boolean getChangeFlag() {
    return changeFlag;
  }

  public ConstraintRepresentation getExpression() {
    return se;
  }

}
