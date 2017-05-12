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

import org.sosy_lab.cpachecker.util.Pair;

/**
 * This interface is designed for abstract domains that require changes when skipping
 * CFA edges in controlled CPA algorithm (e.g. skip entering a function body when
 * depth threshold arrives). CPAs that should implement this interface include ARG, Composite,
 * Location, value analyses such as interval, octagon, etc.
 */
public interface Skippable {

  // TODO: This function should accept a table recording accessible variables for each functions.
  public Pair<AbstractState, Precision> skip();

}
