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
package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Collections;
import java.util.Set;

public class CFAPathTopState implements CFAPathState {

  private static final CFAPathTopState sInstance = new CFAPathTopState();
  private static final Set<CFAPathTopState> sSingleton = Collections.singleton(sInstance);

  public static CFAPathTopState getInstance() {
    return sInstance;
  }

  public static Set<CFAPathTopState> getSingleton() {
    return sSingleton;
  }

  private CFAPathTopState() {

  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
