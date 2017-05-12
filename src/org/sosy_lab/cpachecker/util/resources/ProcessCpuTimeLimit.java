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
package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.common.time.TimeSpan;

import java.util.concurrent.TimeUnit;

import javax.management.JMException;

/**
 * A limit that measures the CPU time used by the current process
 * (if available on this JVM).
 */
public class ProcessCpuTimeLimit implements ResourceLimit {

  private final int processorCount = Runtime.getRuntime().availableProcessors();

  private final long duration;
  private final long endTime;

  private ProcessCpuTimeLimit(long pStart, long pLimit, TimeUnit pUnit) {
    checkArgument(pLimit > 0);
    duration = TimeUnit.NANOSECONDS.convert(pLimit, pUnit);
    endTime = pStart + duration;
  }

  public static ProcessCpuTimeLimit fromNowOn(TimeSpan timeSpan) throws JMException {
    return new ProcessCpuTimeLimit(ProcessCpuTime.read(), timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  public static ProcessCpuTimeLimit fromNowOn(long limit, TimeUnit unit) throws JMException {
    return new ProcessCpuTimeLimit(ProcessCpuTime.read(), limit, unit);
  }

  public static ProcessCpuTimeLimit sinceProcessStart(long time, TimeUnit unit) throws JMException {
    // Do a single read to trigger exceptions if ProcessCpuTime is not available.
    ProcessCpuTime.read();
    return new ProcessCpuTimeLimit(0, time, unit);
  }

  @Override
  public long getCurrentValue() {
    try {
      return ProcessCpuTime.read();
    } catch (JMException e) {
      return 0;
    }
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    if (pCurrentValue == 0) {
      // reading failed suddenly, we disable this limit
      return Long.MAX_VALUE;
    }
    return (endTime - pCurrentValue) / processorCount;
  }

  @Override
  public String getName() {
    return "CPU-time limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}