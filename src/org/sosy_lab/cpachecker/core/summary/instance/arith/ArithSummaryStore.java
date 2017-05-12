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
package org.sosy_lab.cpachecker.core.summary.instance.arith;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.summary.manage.FunctionSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.LoopSummaryStore;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Map;

public class ArithSummaryStore implements FunctionSummaryStore<ArithFunctionSummaryInstance>,
                                          LoopSummaryStore<ArithLoopSummaryInstance> {

  private Map<String, ArithFunctionSummaryInstance> functionArithSummarys;
  private Map<Loop, ArithLoopSummaryInstance> loopArithSummarys;

  public ArithSummaryStore() {
    functionArithSummarys = Maps.newHashMap();
    loopArithSummarys = Maps.newHashMap();
  }

  /*
   * update/query
   */
  public void updateLoopSummary(Loop pLoop, ArithLoopSummaryInstance instance) {
    loopArithSummarys.put(pLoop, instance);
  }

  public void updateFunctionSummary(String pFunctionName, ArithFunctionSummaryInstance instance) {
    functionArithSummarys.put(pFunctionName, instance);
  }

  public void updateFunctionSummary(FunctionEntryNode node, ArithFunctionSummaryInstance instance) {
    updateFunctionSummary(node.getFunctionName(), instance);
  }

  @Override
  public ArithFunctionSummaryInstance query(String pFunction) {
    return functionArithSummarys.containsKey(pFunction) ? functionArithSummarys.get(pFunction)
                                                        : null;

  }

  @Override
  public ArithLoopSummaryInstance query(Loop pLoop) {
    return loopArithSummarys.containsKey(pLoop) ? loopArithSummarys.get(pLoop) : null;
  }

}
