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
package org.sosy_lab.cpachecker.cpa.edgeexclusion;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * This is the singleton abstract state of the edge exclusion CPA. As the CPA
 * does not track any information, this state can be interpreted as top.
 */
enum EdgeExclusionState implements AbstractState {

  TOP;

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
