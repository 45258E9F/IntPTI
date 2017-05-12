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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.weakness.Weakness;

/**
 * The main interface for static analysis error report
 */
public interface ErrorReport {

  ErrorSpot getErrorSpot();

  Weakness getWeakness();

  Class<? extends AbstractState> getSourceStateClass();

}
