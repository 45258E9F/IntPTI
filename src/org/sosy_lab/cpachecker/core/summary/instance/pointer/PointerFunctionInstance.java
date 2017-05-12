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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.summary.apply.AbstractFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.PointerFunctionSummary;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class PointerFunctionInstance extends AbstractFunctionSummaryInstance<PointerResult> {

  private PointerFunctionSummary functionSummary;

  public PointerFunctionInstance(CFANode entryNode) {
    super(entryNode.getFunctionName());
    functionSummary = new PointerFunctionSummary(entryNode);
  }

  public PointerFunctionInstance(PointerFunctionSummary pFunctionSummary) {
    super(pFunctionSummary.getFunctionName());
    functionSummary = new PointerFunctionSummary(pFunctionSummary);
  }

  @Override
  public boolean isEqualTo(SummaryInstance that) {
    return functionSummary.equals(((PointerFunctionInstance) that).getFunctionSummary());
  }

  public PointerFunctionSummary getFunctionSummary() {
    return functionSummary;
  }

  @Override
  public PointerResult apply() {
    return new PointerResult(getPointerResult(functionSummary));
  }

  public Map<MemoryLocation, Set<MemoryLocation>> getPointerResult(PointerFunctionSummary pFunctionSummary) {
    Map<MemoryLocation, Set<MemoryLocation>> map = new HashMap<>();
    SortedMap<MemoryLocation, LocationSet> pointsToMap = pFunctionSummary.getChangedGlobals();
    for (MemoryLocation mem : pointsToMap.keySet()) {
      LocationSet locationSet = pointsToMap.get(mem);
      if (locationSet instanceof LocationSetBot) {
        map.put(mem, new HashSet<MemoryLocation>());
      } else if (locationSet instanceof ExplicitLocationSet) {
        map.put(mem, ((ExplicitLocationSet) locationSet).getExplicitSet());
      } else if (locationSet instanceof LocationSetTop) {
        map.put(mem, pFunctionSummary.getKnownLocations());
      }
    }
    return map;
  }
}
