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
package org.sosy_lab.cpachecker.core.phase.entry;

import static org.sosy_lab.cpachecker.cpa.boundary.BoundaryState.BoundaryFlag.FUNCTION;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryState;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryState.BoundaryFlag;
import org.sosy_lab.cpachecker.util.AbstractStates;

import javax.annotation.Nullable;

public class BoundedStrategy implements DynamicEntryStrategy {

  @Nullable
  @Override
  public CFANode getEntry(AbstractState state) {
    BoundaryState boundaryState = AbstractStates.extractStateByType(state, BoundaryState.class);
    if (boundaryState == null) {
      return null;
    }
    BoundaryFlag flag = boundaryState.getFlag();
    if (flag == FUNCTION) {
      return AbstractStates.extractLocation(state);
    }
    return null;
  }
}
