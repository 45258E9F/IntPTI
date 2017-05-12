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
package org.sosy_lab.cpachecker.cpa.range.util;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cpa.range.Range;

import javax.annotation.Nonnull;

public final class BindRefinePair {

  private final CExpression expression;
  private final Range restriction;

  public BindRefinePair(@Nonnull CExpression pExpression, @Nonnull Range pRestrict) {
    expression = pExpression;
    restriction = pRestrict;
  }

  public CExpression getExpression() {
    return expression;
  }

  public Range getRestrictRange() {
    return restriction;
  }

}
