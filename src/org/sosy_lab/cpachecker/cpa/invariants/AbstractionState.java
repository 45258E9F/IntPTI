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
package org.sosy_lab.cpachecker.cpa.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Set;


interface AbstractionState {

  /**
   * Determine on which variables to use abstraction when merging two
   * invariants states having this and the given abstraction state.
   *
   * @param pOther the other abstraction state.
   * @return the set of widening targets.
   */
  public Set<MemoryLocation> determineWideningTargets(AbstractionState pOther);

  public Set<BooleanFormula<CompoundInterval>> getWideningHints();

  public AbstractionState addEnteringEdge(CFAEdge pEdge);

  public AbstractionState join(AbstractionState pOther);

  public boolean isLessThanOrEqualTo(AbstractionState pOther);

}