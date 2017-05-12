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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;

import java.util.List;

/**
 * Abstract state for the generic assumption generator CPA;
 * encapsulate a symbolic formula that represents the
 * assumption.
 */
public class GenericAssumptionsState implements AbstractState, AssumptionReportingState {

  // The inner representation is an expression.
  private final ImmutableList<CExpression> assumptions;

  public GenericAssumptionsState(Iterable<CExpression> pAssumptions) {
    assumptions = ImmutableList.copyOf(pAssumptions);
  }

  @Override
  public List<CExpression> getAssumptions() {
    return assumptions;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof GenericAssumptionsState) {
      return assumptions.equals(((GenericAssumptionsState) pObj).assumptions);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return assumptions.hashCode();
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public String toString() {
    return from(assumptions).transform(CExpression.TO_AST_STRING).join(Joiner.on(", "));
  }
}
