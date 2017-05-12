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

import org.sosy_lab.cpachecker.core.summary.manage.LoopSummaryStore;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Map;

public class RangeInternalLoopStore implements LoopSummaryStore<RangeInternalLoopInstance> {

  private Map<Loop, RangeInternalLoopInstance> summaryMap;

  public RangeInternalLoopStore() {
    summaryMap = Maps.newHashMap();
  }

  public void updateSummary(Loop pLoop, RangeInternalLoopInstance pInstance) {
    summaryMap.put(pLoop, pInstance);
  }

  @Override
  public RangeInternalLoopInstance query(Loop loop) {
    return summaryMap.get(loop);
  }
}
