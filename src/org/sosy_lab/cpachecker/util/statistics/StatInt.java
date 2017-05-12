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


public class StatInt extends AbstractStatValue {

  private int maxValue = Integer.MIN_VALUE;
  private int minValue = Integer.MAX_VALUE;
  private int valueCount = 0;
  private int valueSum = 0;

  public StatInt(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public void setNextValue(int newValue) {
    valueSum += newValue;
    valueCount += 1;
    maxValue = Math.max(newValue, maxValue);
    minValue = Math.min(newValue, minValue);
  }

  public int getMaxValue() {
    return valueCount == 0 ? 0 : maxValue;
  }

  public int getMinValue() {
    return valueCount == 0 ? 0 : minValue;
  }

  public int getValueCount() {
    return valueCount;
  }

  public int getValueSum() {
    return valueSum;
  }

  public float getAverage() {
    if (valueCount > 0) {
      return (float) valueSum / (float) valueCount;
    } else {
      return 0;
    }
  }

  public int getMax() {
    return maxValue;
  }

  public int getMin() {
    return minValue;
  }

  @Override
  public int getUpdateCount() {
    return valueCount;
  }

  @Override
  public String toString() {
    return String.format("%8d (count: %d, min: %d, max: %d, avg: %.2f)",
        valueSum, valueCount, getMinValue(), getMaxValue(), getAverage());
  }

}