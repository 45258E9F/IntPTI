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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public final class ARGPathCounter {

  private static long counter = 0;

  public static void inc() {
    counter++;
  }

  public static void dec(int delta) {
    counter -= delta;
    if (counter < 0) {
      counter = 0;
    }
  }

  public static void reset() {
    counter = 0;
  }

  public static long getCounter() {
    return counter;
  }

  /**
   * Calculate the index of elements to be kept after trim operation.
   * If the returned value is null, then trim operation is not performed.
   */
  @Nullable
  static List<Integer> getTrimmedIndex(int successorSize, long maxSize) {
    if (maxSize <= 0 || counter <= maxSize || successorSize <= 1) {
      return null;
    }
    long delta = counter - maxSize;
    int kept;
    if (delta < successorSize) {
      counter -= delta;
      kept = (int) (successorSize - delta);
    } else {
      counter -= (successorSize - 1);
      kept = 1;
    }
    List<Integer> indexList = new ArrayList<>();
    int start = successorSize % kept;
    int step = successorSize / kept;
    for (int i = start; i < successorSize; i += step) {
      indexList.add(i);
    }
    return indexList;
  }

}
