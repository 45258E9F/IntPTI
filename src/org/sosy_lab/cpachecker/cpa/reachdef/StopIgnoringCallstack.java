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
package org.sosy_lab.cpachecker.cpa.reachdef;

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class StopIgnoringCallstack implements StopOperator {

  @Override
  public boolean stop(
      AbstractState pState,
      Collection<AbstractState> pReached,
      Precision pPrecision)
      throws CPAException {
    try {
      ReachingDefState e1 = (ReachingDefState) pState;
      ReachingDefState e2;
      for (AbstractState p : pReached) {
        e2 = (ReachingDefState) p;
        if (isSubsetOf(e1.getLocalReachingDefinitions(), e2.getLocalReachingDefinitions())
            && isSubsetOf(e1.getGlobalReachingDefinitions(), e2.getGlobalReachingDefinitions())) {
          return true;
        }
      }
    } catch (ClassCastException e) {
    }
    return false;
  }

  private boolean isSubsetOf(
      Map<String, Set<DefinitionPoint>> subset,
      Map<String, Set<DefinitionPoint>> superset) {
    Set<DefinitionPoint> setSub, setSuper;
    if (subset == superset) {
      return true;
    }
    for (Entry<String, Set<DefinitionPoint>> entry : subset.entrySet()) {
      setSub = entry.getValue();
      setSuper = superset.get(entry.getKey());
      if (setSub == setSuper) {
        continue;
      }
      if (setSuper == null || Sets.intersection(setSub, setSuper).size() != setSub.size()) {
        return false;
      }
    }
    return true;
  }

}
