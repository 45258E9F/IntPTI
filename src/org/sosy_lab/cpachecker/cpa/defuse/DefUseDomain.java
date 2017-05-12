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
package org.sosy_lab.cpachecker.cpa.defuse;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.HashSet;
import java.util.Set;

public class DefUseDomain implements AbstractDomain {
  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {
    DefUseState defUseState1 = (DefUseState) element1;
    DefUseState defUseState2 = (DefUseState) element2;

    return defUseState2.containsAllOf(defUseState1);
  }

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
    // Useless code, but helps to catch bugs by causing cast exceptions
    DefUseState defUseState1 = (DefUseState) element1;
    DefUseState defUseState2 = (DefUseState) element2;

    Set<DefUseDefinition> joined = new HashSet<>();
    for (DefUseDefinition definition : defUseState1) {
      joined.add(definition);
    }

    for (DefUseDefinition definition : defUseState2) {
      if (!joined.contains(definition)) {
        joined.add(definition);
      }
    }

    return new DefUseState(joined);
  }
}
