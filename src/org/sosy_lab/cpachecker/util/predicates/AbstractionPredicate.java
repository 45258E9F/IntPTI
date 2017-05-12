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
package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.solver.api.BooleanFormula;

/**
 * A generic representation of a predicate
 */
public class AbstractionPredicate {

  private final Region abstractVariable;
  private final BooleanFormula symbolicVariable;
  private final BooleanFormula symbolicAtom;
  private final int variableNumber;

  AbstractionPredicate(
      Region pAbstractVariable,
      BooleanFormula pSymbolicVariable, BooleanFormula pSymbolicAtom,
      int variableNumber) {
    abstractVariable = Preconditions.checkNotNull(pAbstractVariable);
    symbolicVariable = Preconditions.checkNotNull(pSymbolicVariable);
    symbolicAtom = Preconditions.checkNotNull(pSymbolicAtom);
    this.variableNumber = variableNumber;
  }

  /**
   * Returns an formula representing this predicate.
   *
   * @return an abstract formula
   */
  public Region getAbstractVariable() {
    return abstractVariable;
  }

  public BooleanFormula getSymbolicVariable() {
    return symbolicVariable;
  }

  public BooleanFormula getSymbolicAtom() {
    return symbolicAtom;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof AbstractionPredicate)) {
      return false;
    } else {
      AbstractionPredicate other = (AbstractionPredicate) pObj;
      return this.abstractVariable.equals(other.abstractVariable);
    }
  }

  @Override
  public int hashCode() {
    return abstractVariable.hashCode();
  }

  @Override
  public String toString() {
    return abstractVariable + " <-> " + symbolicVariable + " <-> " + symbolicAtom;
  }

  public int getVariableNumber() {
    return variableNumber;
  }
}
