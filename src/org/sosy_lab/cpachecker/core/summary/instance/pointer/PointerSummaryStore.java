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

import org.sosy_lab.cpachecker.core.summary.manage.FunctionSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.LoopSummaryStore;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by landq on 12/8/16.
 */
public class PointerSummaryStore implements FunctionSummaryStore<PointerFunctionInstance>,
                                            LoopSummaryStore<PointerLoopInstance> {

  private static Map<String, PointerFunctionInstance> functionInstances;
  private static Map<Loop, PointerLoopInstance> loopInstances;

  public PointerSummaryStore() {
    functionInstances = new HashMap<>();
    loopInstances = new HashMap<>();
  }

  public void addFunctionInstance(String funcName, PointerFunctionInstance functionInstance) {
    functionInstances.put(funcName, functionInstance);
  }

  public void addLoopInstance(Loop loop, PointerLoopInstance loopInstance) {
    loopInstances.put(loop, loopInstance);
  }

  @Override
  public PointerFunctionInstance query(String function) {
    return functionInstances.containsKey(function) ? functionInstances.get(function) : null;
  }

  @Override
  public PointerLoopInstance query(Loop loop) {
    return loopInstances.containsKey(loop) ? loopInstances.get(loop) : null;

  }
}
