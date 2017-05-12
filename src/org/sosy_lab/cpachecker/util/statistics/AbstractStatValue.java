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


public abstract class AbstractStatValue {

  private final String title;
  private StatKind mainStatisticKind;

  public AbstractStatValue(StatKind pMainStatisticKind, String pTitle) {
    this.title = pTitle;
    this.mainStatisticKind = pMainStatisticKind;
  }

  public String getTitle() {
    return title;
  }

  /**
   * How many times was this statistical value updated.
   *
   * @return A nonnegative number.
   */
  public abstract int getUpdateCount();


  public StatKind getMainStatisticKind() {
    return mainStatisticKind;
  }
}
