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
package org.sosy_lab.cpachecker.cpa.reachdef;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class MergeIgnoringCallstack implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException {
    if (pState1 instanceof ReachingDefState && pState2 instanceof ReachingDefState) {
      ReachingDefState e1 = (ReachingDefState) pState1;
      ReachingDefState e2 = (ReachingDefState) pState2;
      Map<String, Set<DefinitionPoint>> local = unionMaps(e1.getLocalReachingDefinitions(),
          e2.getLocalReachingDefinitions());
      Map<String, Set<DefinitionPoint>> global = unionMaps(e1.getGlobalReachingDefinitions(),
          e2.getGlobalReachingDefinitions());
      if (local != e2.getLocalReachingDefinitions() || global != e2
          .getGlobalReachingDefinitions()) {
        return new ReachingDefState(local, global, null);
      }
    }
    return pState2;
  }


  private Map<String, Set<DefinitionPoint>> unionMaps(
      Map<String, Set<DefinitionPoint>> map1,
      Map<String, Set<DefinitionPoint>> map2) {
    Map<String, Set<DefinitionPoint>> newMap = new HashMap<>();
    HashSet<String> vars = new HashSet<>();
    vars.addAll(map1.keySet());
    vars.addAll(map2.keySet());

    HashSet<DefinitionPoint> unionResult;
    boolean changed = false;
    if (map1 == map2) {
      return map2;
    }
    for (String var : vars) {
      // decrease merge time, avoid building union if unnecessary
      if (map1.get(var) == map2.get(var)) {
        newMap.put(var, map2.get(var));
        continue;
      }

      if (map1.get(var) == null) {
        newMap.put(var, map2.get(var));
      } else if (map2.get(var) == null) {
        newMap.put(var, map1.get(var));
        changed = true;
      } else {
        unionResult = new HashSet<>();
        for (DefinitionPoint p : map1.get(var)) {
          unionResult.add(p);
        }
        for (DefinitionPoint p : map2.get(var)) {
          unionResult.add(p);
        }
        if (unionResult.size() != map2.get(var).size()) {
          changed = true;
        }
        newMap.put(var, unionResult);
      }
    }
    if (changed) {
      return newMap;
    }
    return map2;
  }


}
