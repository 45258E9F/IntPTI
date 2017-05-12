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

import org.sosy_lab.cpachecker.cpa.range.RangeState;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

/**
 * Function precondition store for range analysis.
 * Precondition info can be used for correctly initialization of function argument.
 */
public final class RangeFunctionPrecondition {

  // collected precondition in one batch
  // for each function, the precondition state should be monotonically growing
  private static Map<String, RangeState> currentPreCond = Maps.newHashMap();

  // after one batch we should update the total precondition pool with derived preconditions in
  // the previous batch
  // this pool is used for querying precondition info, which can be used, typically, to correctly
  // initialize function arguments
  // for each function, the total precondition should be monotonically shrinking
  private static Map<String, RangeState> totalPreCond = Maps.newHashMap();

  public static void updatePrecondition(String pName, RangeState pState) {
    RangeState existCond = currentPreCond.get(pName);
    if(existCond == null) {
      currentPreCond.put(pName, pState);
    } else {
      existCond = existCond.join(pState);
      currentPreCond.put(pName, pState);
    }
  }

  public static boolean updateBatch() {
    boolean changed = false;
    for (Entry<String, RangeState> currentEntry : currentPreCond.entrySet()) {
      String name = currentEntry.getKey();
      RangeState newPreCond = currentEntry.getValue();
      RangeState existPreCond = totalPreCond.get(name);
      if(existPreCond == null) {
        changed = true;
        totalPreCond.put(name, newPreCond);
      } else {
        if(newPreCond.isLessOrEqual(existPreCond)) {
          changed = true;
          totalPreCond.put(name, newPreCond);
        }
        // otherwise, we keep the existing precondition
      }
    }
    return changed;
  }

  /**
   * If the precondition is NULL, then we initialize function arguments using type range visitor.
   */
  @Nullable
  public static RangeState getPrecondition(String pName) {
    return totalPreCond.get(pName);
  }

}
