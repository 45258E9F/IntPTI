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
package org.sosy_lab.cpachecker.core.bugfix;

import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;

public interface FixInformation {

  BugCategory getCategory();

  /**
   * Reset the temporary data for the next analysis entry.
   */
  void reset();

}
