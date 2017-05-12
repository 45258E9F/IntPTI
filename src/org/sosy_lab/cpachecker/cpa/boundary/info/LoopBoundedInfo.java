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
package org.sosy_lab.cpachecker.cpa.boundary.info;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collection;

public class LoopBoundedInfo implements BoundedInfo<Loop> {

  private Loop loop;
  private CFAEdge enterEdge;

  private LoopOutOfBoundReason reason;

  private LoopBoundedInfo(Loop pLoop, CFAEdge pEdge, LoopOutOfBoundReason pReason) {
    loop = pLoop;
    enterEdge = pEdge;
    reason = pReason;
  }

  public static BoundedInfo<?> of(Loop pLoop, CFANode pLoopEntry, LoopOutOfBoundReason pReason) {
    if (pLoop == null) {
      return EmptyBoundedInfo.EMPTY;
    }
    // check if the given entry node is associated with one incoming edge
    Iterable<CFAEdge> incomingEdges = Iterables.filter(pLoop.getIncomingEdges(), not(instanceOf
        (CFunctionReturnEdge.class)));
    CFAEdge incoming = null;
    for (CFAEdge incomingEdge : incomingEdges) {
      if (pLoopEntry.equals(incomingEdge.getSuccessor())) {
        incoming = incomingEdge;
        break;
      }
    }
    if (incoming == null) {
      return EmptyBoundedInfo.EMPTY;
    }
    return new LoopBoundedInfo(pLoop, incoming, pReason);
  }

  @Override
  public Loop getBoundedObject() {
    return loop;
  }

  @Override
  public CFAEdge getEntry() {
    return enterEdge;
  }

  public LoopOutOfBoundReason getReason() {
    return reason;
  }

  @Override
  public Collection<CFAEdge> getExit() {
    Iterable<CFAEdge> outgoingEdges = Iterables.filter(loop.getOutgoingEdges(), not(instanceOf
        (CFunctionCallEdge.class)));
    return ImmutableSet.copyOf(outgoingEdges);
  }

  public enum LoopOutOfBoundReason {

    MAX_DEPTH_REACHED,
    MAX_ITERATION_REACHED,
    N_A

  }

}
