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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;

/**
 * Interface for a specific class of conditions which limit single paths
 * depending on some condition like its length.
 * Implementations of this interface can be used with the {@link PathConditionsCPA}.
 * For this to work, they need to have a public constructor with two parameters
 * of types {@link org.sosy_lab.common.configuration.Configuration} and
 * {@link org.sosy_lab.common.log.LogManager}, respectively.
 *
 * In order to cut off a path, conditions need to return an element from
 * {@link PathCondition#getAbstractSuccessor(AbstractState, CFAEdge)} whose
 * {@link AvoidanceReportingState#mustDumpAssumptionForAvoidance()} method
 * returns true.
 * Note that this will have an effect only if the
 * {@link org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA}
 * is present.
 */
public interface PathCondition {

  /**
   * Get the initial element.
   *
   * @see ConfigurableProgramAnalysis#getInitialState(CFANode, StateSpacePartition)
   */
  AvoidanceReportingState getInitialState(CFANode pNode);

  /**
   * Get the successor state for an edge.
   *
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#getAbstractSuccessorsForEdge(AbstractState,
   * java.util.List, Precision, CFAEdge)
   */
  AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge);

  /**
   * Adjust the precision of this condition, i.e., by increasing a threshold value.
   *
   * @see org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA#adjustPrecision()
   */
  boolean adjustPrecision();

  Reducer getReducer();
}
