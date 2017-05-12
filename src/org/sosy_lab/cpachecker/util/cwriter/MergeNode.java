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
package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MergeNode {

  private final int stateId;
  private final Map<Integer, Pair<Boolean, Boolean>> branchesMap;
  private final List<FunctionBody> incomingState;

  public MergeNode(int pElementId) {
    stateId = pElementId;
    branchesMap = new HashMap<>();
    incomingState = new ArrayList<>();
  }

  public int addBranch(FunctionBody currentFunction) {
    incomingState.add(currentFunction);
    Set<Integer> processedConditions = new HashSet<>();

    for (BasicBlock elementInStack : currentFunction) {
      int idOfElementInStack = elementInStack.getStateId();
      boolean nextConditionValue = elementInStack.isCondition();
      boolean isClosedBefore = elementInStack.isClosedBefore();

      // if we already have a value for the same initial node of the condition
      if (branchesMap.containsKey(idOfElementInStack)) {
        // if it was closed earlier somewhere else
        Pair<Boolean, Boolean> conditionPair = branchesMap.get(idOfElementInStack);
        boolean firstConditionValue = conditionPair.getFirst();
        boolean secondConditionValue = conditionPair.getSecond();
        // if this is the end of the branch
        if (isClosedBefore || secondConditionValue ||
            (firstConditionValue ^ nextConditionValue)) {
//          elementInStack.setClosedBefore(true);
          processedConditions.add(idOfElementInStack);
        }
        // else do nothing
      } else {
        // create the first entry in the map
        branchesMap.put(idOfElementInStack, Pair.of(nextConditionValue, isClosedBefore));
      }
    }

    setProcessedStates(processedConditions);

    return incomingState.size();
  }

  private void setProcessedStates(Set<Integer> pProcessedConditions) {
    for (FunctionBody stack : incomingState) {
      for (BasicBlock elem : stack) {
        if (pProcessedConditions.contains(elem.getStateId())) {
          elem.setClosedBefore(true);
        }
      }
    }
  }

  public List<FunctionBody> getIncomingStates() {
    return incomingState;
  }

  @Override
  public String toString() {
    return "id: " + stateId + " >> " + branchesMap;
  }
}