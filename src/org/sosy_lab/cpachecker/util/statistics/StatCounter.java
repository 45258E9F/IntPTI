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
package org.sosy_lab.cpachecker.util.statistics;


public class StatCounter extends AbstractStatValue {

  private int counter = 0;

  public StatCounter(String pTitle) {
    super(StatKind.SUM, pTitle);
  }

  public void inc() {
    counter++;
  }

  public int getValue() {
    return counter;
  }

  @Override
  public int getUpdateCount() {
    return counter;
  }

  @Override
  public String toString() {
    return String.format("%8d", counter);
  }

}