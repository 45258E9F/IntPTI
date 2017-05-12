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
package org.sosy_lab.cpachecker.core.interfaces.summary;

import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

public interface SummaryAcceptableState {

  /**
   * Apply function summary to current state.
   *
   * @param pSummaryList list of summaries to be applied with
   * @param inEdge       incoming edge of the function
   * @param outEdge      leaving edge of the function
   * @param pOtherStates other state components
   * @return the resultant states after applied with the summaries
   */
  Collection<? extends AbstractState> applyFunctionSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      CFAEdge outEdge,
      List<AbstractState> pOtherStates) throws CPATransferException;


  /**
   * Apply loop summary to current state.
   * Invariant: the key set of resultant mapping should be a subset of <code>outEdges</code>.
   *
   * @param pSummaryList list of summaries to be applied with
   * @param inEdge       incoming edge of the loop
   * @param outEdges     leaving edges of the loop
   * @param pOtherStates other state components
   * @return the resultant states after applied with the summaries. Note that each successor state
   * is associated with its leaving CFA edge.
   */
  Multimap<CFAEdge, AbstractState> applyExternalLoopSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      Collection<CFAEdge> outEdges,
      List<AbstractState> pOtherStates) throws CPATransferException;


  /**
   * Apply loop summary to current state.
   *
   * @param pSummaryList list of summaries to be applied with
   * @param inEdge       incoming edge of the loop
   * @param pOtherStates other state components
   * @return the resultant states after applied with the summaries
   */
  Collection<? extends AbstractState> applyInternalLoopSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      List<AbstractState> pOtherStates) throws CPATransferException;

}
