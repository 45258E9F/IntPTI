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
package org.sosy_lab.cpachecker.core.summary.instance.access;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.summary.manage.FunctionSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.LoopSummaryStore;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Map;

/**
 * AccessSummaryStore to record all the access summary
 * Notes: currently, we only have functionAccessSummary
 */
public class AccessSummaryStore implements FunctionSummaryStore<AccessFunctionInstance>,
                                           LoopSummaryStore<AccessLoopInstance> {

  private Map<String, AccessFunctionInstance> functionAccessSummary;
  private Map<Loop, AccessLoopInstance> loopAccessSummary;

  /*
   *  construction
   */
  public AccessSummaryStore() {
    functionAccessSummary = Maps.newHashMap();
    loopAccessSummary = Maps.newHashMap();
  }


  /*
   * update/query
   */
  public void updateLoopSummary(Loop pLoop, AccessLoopInstance instance) {
    loopAccessSummary.put(pLoop, instance);
  }

  public void updateFunctionSummary(String pFunctionName, AccessFunctionInstance instance) {
    functionAccessSummary.put(pFunctionName, instance);
  }

  public void updateFunctionSummary(FunctionEntryNode node, AccessFunctionInstance instance) {
    updateFunctionSummary(node.getFunctionName(), instance);
  }

  @Override
  public AccessLoopInstance query(Loop pLoop) {
    return loopAccessSummary.containsKey(pLoop) ? loopAccessSummary.get(pLoop) : null;
  }

  @Override
  public AccessFunctionInstance query(String pFunction) {
    return functionAccessSummary.containsKey(pFunction) ? functionAccessSummary.get(pFunction)
                                                        : null;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Finally \n");
    buffer.append("Function \n");
    for (String name : functionAccessSummary.keySet()) {
      buffer.append(name + " -> " + functionAccessSummary.get(name) + "\n\n");
    }
    buffer.append("Loop \n");
    for (Loop loop : loopAccessSummary.keySet()) {
      buffer.append(loop + " -> \n" + loopAccessSummary.get(loop) + "\n\n");
    }
    return buffer.toString();
  }

}
