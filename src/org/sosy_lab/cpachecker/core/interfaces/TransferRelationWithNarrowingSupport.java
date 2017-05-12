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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

public interface TransferRelationWithNarrowingSupport extends TransferRelation {

  Collection<? extends AbstractState> getAbstractSuccessorsUnderNarrowing(
      AbstractState state,
      List<AbstractState> otherStates,
      Precision precision)
      throws CPATransferException, InterruptedException;

  Collection<? extends AbstractState> getAbstractSuccessorsForEdgeUnderNarrowing(
      AbstractState state,
      List<AbstractState> otherStates,
      Precision precision,
      CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException;

}
