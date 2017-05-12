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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

/**
 * Abstract base class for {@link TransferRelation},
 * which should be used by most CPAs.
 *
 * It eliminates the need to implement a stub for
 * {@link TransferRelation#getAbstractSuccessors(AbstractState, List, Precision)}.
 */
public abstract class SingleEdgeTransferRelation implements TransferRelation {

  @Override
  public final Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState,
      List<AbstractState> otherStates,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    throw new UnsupportedOperationException(
        "The " + this.getClass().getSimpleName()
            + " expects to be called with a CFA edge supplied"
            + " and does not support configuration where it needs to"
            + " return abstract states for any CFA edge.");
  }
}
