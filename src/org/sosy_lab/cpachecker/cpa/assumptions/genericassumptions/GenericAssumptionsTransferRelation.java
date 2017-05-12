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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Transfer relation for the generic assumption generator.
 */
public class GenericAssumptionsTransferRelation extends SingleEdgeTransferRelation {

  /**
   * List of interfaces used to build the default
   * assumptions made by the model checker for
   * program operations.
   *
   * Modify this to register new kind of assumptions.
   */
  private final List<GenericAssumptionBuilder> assumptionBuilders =
      ImmutableList.<GenericAssumptionBuilder>of(
          new ArithmeticOverflowAssumptionBuilder());

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState el, List<AbstractState> otherStates, Precision p, CFAEdge edge)
      throws CPATransferException {

    List<CExpression> allAssumptions = Lists.newArrayList();
    for (GenericAssumptionBuilder b : assumptionBuilders) {
      allAssumptions.addAll(b.assumptionsForEdge(edge));
    }

    return Collections.singleton(new GenericAssumptionsState(allAssumptions));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState el, List<AbstractState> otherElements,
      CFAEdge edge, Precision p)
      throws CPATransferException {
    // TODO Improve strengthening for assumptions so that they
    //      may be discharged online
    return null;
  }

}
