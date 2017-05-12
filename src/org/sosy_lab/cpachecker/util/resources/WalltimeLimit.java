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
package org.sosy_lab.cpachecker.util.resources;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.common.time.TimeSpan;

import java.util.concurrent.TimeUnit;

/**
 * A limit that measures the elapsed time as returned by {@link System#nanoTime()}.
 */
public class WalltimeLimit implements ResourceLimit {

  private final long duration;
  private final long endTime;

  private WalltimeLimit(long pDuration) {
    duration = pDuration;
    endTime = getCurrentValue() + pDuration;
  }

  public static WalltimeLimit fromNowOn(TimeSpan timeSpan) {
    return fromNowOn(timeSpan.asNanos(), TimeUnit.NANOSECONDS);
  }

  public static WalltimeLimit fromNowOn(long time, TimeUnit unit) {
    checkArgument(time > 0);
    long nanoDuration = TimeUnit.NANOSECONDS.convert(time, unit);
    return new WalltimeLimit(nanoDuration);
  }

  @Override
  public long getCurrentValue() {
    return System.nanoTime();
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    return pCurrentValue >= endTime;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    return endTime - pCurrentValue;
  }

  @Override
  public String getName() {
    return "walltime limit of " + TimeUnit.NANOSECONDS.toSeconds(duration) + "s";
  }
}