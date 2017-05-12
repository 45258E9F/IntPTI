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
package org.sosy_lab.cpachecker.cpa.pointer2.summary;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

public class PointerLoopSummary implements Summary<PointerLoopSummary> {
  private SortedMap<MemoryLocation, LocationSet> changedVars;
  private Set<CFAEdge> calledFunctions;
  private Stack<Loop> stack;

  public PointerLoopSummary mergeChangedVars(
      PointerLoopSummary merged,
      PointerFunctionSummary other) {
    for (MemoryLocation mem : other.getChangedGlobals().keySet()) {
      merged.addChangedVars(mem, other.getChangedGlobals().get(mem));
    }
    return merged;
  }

  public PointerLoopSummary(Stack<Loop> pStack, Loop pLoop) {
    changedVars = new TreeMap<>();
    calledFunctions = new HashSet<>();
    stack = (Stack<Loop>) pStack.clone();
    stack.push(pLoop);
  }

  public PointerLoopSummary(PointerLoopSummary pLoopSummary) {
    changedVars = new TreeMap<>();
    changedVars.putAll(pLoopSummary.getChangedVars());
    calledFunctions = new HashSet<>();
    calledFunctions.addAll(pLoopSummary.getCalledFunctions());
    stack = (Stack<Loop>) pLoopSummary.getStack().clone();
  }

  public void addChangedVars(MemoryLocation pMemoryLocation, LocationSet pLocationSet) {
    if (changedVars.containsKey(pMemoryLocation)) {
      if (!changedVars.get(pMemoryLocation).containsAll(pLocationSet)) {
        pLocationSet.addElements(changedVars.get(pMemoryLocation));
        changedVars.put(pMemoryLocation, pLocationSet);
      }
    } else {
      changedVars.put(pMemoryLocation, pLocationSet);
    }
  }

  public SortedMap<MemoryLocation, LocationSet> getChangedVars() {
    return changedVars;
  }

  public void addCalledFunction(CFAEdge calledFunction) {
    calledFunctions.add(calledFunction);
  }

  public Stack<Loop> getStack() {
    return stack;
  }

  @Override
  public Set<CFAEdge> getCalledFunctions() {
    return calledFunctions;
  }

  @Override
  public String getFunctionName() {
    return stack.peek().getLoopNodes().first().getFunctionName();
  }

  @Override
  public void addChanged(
      MemoryLocation pMemoryLocation, LocationSet pLocationSet) {
    addChangedVars(pMemoryLocation, pLocationSet);
  }

  @Override
  public PointerLoopSummary copyOf() {
    return new PointerLoopSummary(this);
  }
}
