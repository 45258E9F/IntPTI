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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

import java.util.List;

/**
 * Sub-interface for {@link AbstractState}s that marks states
 * with an assumption.
 * This is intended for other CPAs to use in their strengthen operator,
 * such that all the other CPAs can add these assumptions to their abstract state.
 */
public interface AbstractStateWithAssumptions extends AbstractState {

  /**
   * Get the list of assumptions in form of statements.
   *
   * @return A (possibly empty list) of statements.
   */
  List<AStatement> getAssumptions();

  /**
   * Get the list of assumptions transformed into AssumeEdges.
   * This might be easier to use by other CPAs.
   *
   * Assumptions about function return value are transformed from
   * "return N;" to "[retVar == N]", where "retVar" is the name of a pseudo variable
   * (just as {@link AReturnStatement#asAssignment()} does.
   *
   * The CFANodes attached to the produced edges are not real nodes
   * and should not be used. In particular, there is no guarantee that the list
   * of edges corresponds to a connected chain of nodes and edges.
   *
   * @param functionName the function name where the assumptions are
   * @return A (possibly empty list) of assume edges.
   */
  List<AssumeEdge> getAsAssumeEdges(String functionName);
}