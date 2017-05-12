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
package org.sosy_lab.cpachecker.core.summary.instance.pointer;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.summary.apply.AbstractLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.apply.ApplicableExternalLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.PointerLoopSummary;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;

/**
 * Created by landq on 12/8/16.
 */
public class PointerLoopInstance extends AbstractLoopSummaryInstance
    implements ApplicableExternalLoopSummaryInstance<PointerResult> {

  private PointerLoopSummary loopSummary;

  public PointerLoopInstance(Stack<Loop> pStack, Loop pLoop) {
    super(pLoop);
    loopSummary = new PointerLoopSummary(pStack, pLoop);
  }

  public PointerLoopInstance(PointerLoopSummary pLoopSummary) {
    super(pLoopSummary.getStack().peek());
    loopSummary = new PointerLoopSummary(pLoopSummary);
  }

  @Override
  public boolean isEqualTo(SummaryInstance that) {
    return loopSummary.equals(((PointerLoopInstance) that).getLoopSummary());
  }

  public PointerLoopSummary getLoopSummary() {
    return loopSummary;
  }

  @Override
  public PointerResult apply(CFAEdge pEntering, CFAEdge pLeaving) {
    return new PointerResult(getPointerResult(loopSummary, pEntering
        .getSuccessor().getFunctionName()));
  }

  public Map<MemoryLocation, Set<MemoryLocation>> getPointerResult(
      PointerLoopSummary pLoopSummary,
      String funcName) {
    Map<MemoryLocation, Set<MemoryLocation>> map = new HashMap<>();
    SortedMap<MemoryLocation, LocationSet> pointsToMap = pLoopSummary.getChangedVars();
    for (MemoryLocation mem : pointsToMap.keySet()) {
      LocationSet locationSet = pointsToMap.get(mem);
      if (locationSet instanceof LocationSetBot) {
        map.put(mem, new HashSet<MemoryLocation>());
      } else if (locationSet instanceof ExplicitLocationSet) {
        map.put(mem, ((ExplicitLocationSet) locationSet).getExplicitSet());
      } else if (locationSet instanceof LocationSetTop) {
        map.put(mem, (new PointerSummaryStore()).query(funcName).getFunctionSummary()
            .getKnownLocations());
      }
    }
    return map;
  }
}
