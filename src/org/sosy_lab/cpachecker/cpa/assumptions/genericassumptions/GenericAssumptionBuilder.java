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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import java.util.List;

/**
 * Abstraction of a class that generates generic
 * assumption invariants from CFA edges
 */
public interface GenericAssumptionBuilder {

  /**
   * Return a list of assumption predicate that the system assumes when
   * it encounters the given edge. The assumptions are evaluated in
   * the pre-state of the edge.
   *
   * @return A non-null, possibly empty list of predicates representing the assumptions
   */
  public List<CExpression> assumptionsForEdge(CFAEdge edge);

}
