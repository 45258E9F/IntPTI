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
package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;

import javax.annotation.Nonnull;

/**
 * This interface is provided as a shortcut, so that other CPAs' strengthen
 * operator can check whether one abstract state represents some kind of
 * "target" or "error" abstract state without needing to know more about the state
 * (especially without knowing its actual type).
 */
public interface Targetable {

  public boolean isTarget();

  /**
   * Return a human-readable description of the violated property.
   * Example: "assert statement in line X"
   *
   * @return A non-null String, may be empty if no description is available.
   * @throws IllegalStateException if {@link #isTarget()} returns false
   */
  public
  @Nonnull
  Set<Property> getViolatedProperties() throws IllegalStateException;
}
