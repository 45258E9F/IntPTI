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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

/**
 * Implementation of a property checker which does not accept abstract states representing some kind
 * of "target" or "error" abstract state. Accepts every abstract state which is not a target
 * abstract state and every set of states which does not contain a target abstract state.
 */
public class NoTargetStateChecker extends PerElementPropertyChecker {

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    return (!(pElemToCheck instanceof Targetable) || !((Targetable) pElemToCheck).isTarget());
  }

}
