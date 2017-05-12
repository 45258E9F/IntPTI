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
package org.sosy_lab.cpachecker.core.summary.instance.range;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.summary.manage.FunctionSummaryStore;

import java.util.Map;

public class RangeFunctionStore implements FunctionSummaryStore<RangeFunctionInstance> {

  private Map<String, RangeFunctionInstance> summaryMap;

  public RangeFunctionStore() {
    summaryMap = Maps.newHashMap();
  }

  public void updateSummary(String pFunctionName, RangeFunctionInstance pInstance) {
    summaryMap.put(pFunctionName, pInstance);
  }

  public void updateSummary(FunctionEntryNode pEntry, RangeFunctionInstance pInstance) {
    summaryMap.put(pEntry.getFunctionName(), pInstance);
  }

  @Override
  public RangeFunctionInstance query(String function) {
    return summaryMap.get(function);
  }
}
