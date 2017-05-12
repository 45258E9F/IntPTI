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
package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;


public class StatIntHist extends StatInt {

  private Multiset<Integer> hist = HashMultiset.create();

  public StatIntHist(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public int getTimesWithValue(Integer value) {
    return hist.count(value);
  }

  @Override
  public void setNextValue(int pNewValue) {
    super.setNextValue(pNewValue);
    hist.add(pNewValue);
  }

  @Override
  public String toString() {
    return super.toString() + " " + hist.toString();
  }

}