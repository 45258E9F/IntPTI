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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.graphs.ShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.BasicValuePair;
import org.sosy_lab.cpachecker.util.collections.map.PersistentMaps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class GuardedAbstraction extends BasicAbstraction<GuardedValue> {

  private GuardedAbstraction() {
    super();
  }

  public void copy(GuardedAbstraction pAbstraction) {
    refineMap = refineMap.empty();
    refineMap = PersistentMaps.copy(pAbstraction.refineMap);
    aliasMap = aliasMap.empty();
    aliasMap = PersistentMaps.copy(pAbstraction.aliasMap);
  }

  public static GuardedAbstraction of() {
    return new GuardedAbstraction();
  }

  @Override
  public void addAbstraction(
      Long abstractValue, ShapeGraph graph1, Long v1,
      ShapeGraph graph2, Long v2) {
    Set<ConstraintRepresentation> condition1 = graph1.getAfterBranchCondition();
    Set<ConstraintRepresentation> condition2 = graph2.getAfterBranchCondition();
    refineMap = refineMap.putAndCopy(abstractValue, GuardedValuePair.of(v1, v2, condition1,
        condition2));
  }

  @Override
  public Collection<GuardedValue> interpret(Long v) {
    List<GuardedValue> interpretation = new ArrayList<>();
    Deque<GuardedValue> waitList = new ArrayDeque<>();
    GuardedValuePair pair = (GuardedValuePair) refineMap.get(v);
    if (pair != null) {
      waitList.add(pair.getLeft());
      waitList.add(pair.getRight());
    }
    while (!waitList.isEmpty()) {
      GuardedValue waitValue = waitList.poll();
      GuardedValuePair newPair = (GuardedValuePair) refineMap.get(waitValue.getValue());
      if (newPair == null) {
        interpretation.add(waitValue);
      } else {
        // the new interpretation inherits the current constraints
        waitList.add(newPair.getLeft().inherit(waitValue));
        waitList.add(newPair.getRight().inherit(waitValue));
      }
    }
    return Collections.unmodifiableList(interpretation);
  }

  @Override
  public GuardedAbstraction merge(
      ShapeGraph thisGraph, BasicAbstraction<GuardedValue> pThatAbstraction, ShapeGraph
      thatGraph, Set<BasicValuePair> newMerges, BasicAbstraction<GuardedValue> rawMerged) {
    GuardedAbstraction that = (GuardedAbstraction) pThatAbstraction;
    if (this.equals(that)) {
      return this;
    }
    // STEP 1: merge value abstraction
    GuardedAbstraction merged = new GuardedAbstraction();
    Set<Long> keySet1 = refineMap.keySet();
    Set<Long> keySet2 = that.refineMap.keySet();
    Set<Long> commonKeys = Sets.intersection(keySet1, keySet2);
    for (Long commonKey : commonKeys) {
      merged.refineMap = merged.refineMap.putAndCopy(commonKey, refineMap.get(commonKey));
    }
    // traverse the complementary key set to find the same abstraction to the different
    // abstracted value
    Set<Long> cKeySet1 = new TreeSet<>(Sets.difference(keySet1, commonKeys));
    Set<Long> cKeySet2 = new TreeSet<>(Sets.difference(keySet2, commonKeys));
    Iterator<Long> it1 = cKeySet1.iterator();
    Iterator<Long> it2;
    while (it1.hasNext()) {
      Long key1 = it1.next();
      it2 = cKeySet2.iterator();
      while (it2.hasNext()) {
        Long key2 = it2.next();
        if (refineMap.get(key1).equals(that.refineMap.get(key2))) {
          it1.remove();
          it2.remove();
          newMerges.add(BasicValuePair.of(key1, key2));
        }
      }
    }
    for (BasicValuePair pair : newMerges) {
      Long v1 = pair.getLeft();
      Long v2 = pair.getRight();
      Long kept = Math.min(v1, v2);
      merged.refineMap = merged.refineMap.putAndCopy(kept, refineMap.get(v1));
    }
    // for the remaining unshared abstractions, we directly add them into the merged abstraction
    // UPDATE: consider the following occasion where two states S1 and S2 are to be merged. The
    // abstraction of S1 is empty and the abstraction of S2 is x -> (y | C1, z | C2). The common
    // variable r in S1 and S2 has the value w and x respectively. Now we should perform
    // abstraction for r and thus generate a new abstraction u -> (w | C1', x | C2'). Note that
    // it is unnecessary to add the branch-specific condition of S2 to the abstraction of x,
    // since x is further abstracted into u.
    Set<Long> rawAbstracted = new TreeSet<>();
    for (Entry<Long, AbstractionPair<GuardedValue>> entry : merged.refineMap.entrySet()) {
      AbstractionPair<GuardedValue> value = entry.getValue();
      rawAbstracted.add(value.getLeftValue());
      rawAbstracted.add(value.getRightValue());
    }
    Set<ConstraintRepresentation> branchConditions1 = thisGraph.getAfterBranchCondition();
    for (Long cKey1 : cKeySet1) {
      GuardedValuePair pair = (GuardedValuePair) refineMap.get(cKey1);
      if (!rawAbstracted.contains(cKey1)) {
        // If the new total guards are unsatisfiable, then the certain branch would be unreachable.
        // Hence, the total guards must be satisfiable.
        pair.addMoreGuards(branchConditions1);
      }
      merged.refineMap = merged.refineMap.putAndCopy(cKey1, pair);
    }
    Set<ConstraintRepresentation> branchConditions2 = thatGraph.getAfterBranchCondition();
    for (Long cKey2 : cKeySet2) {
      GuardedValuePair pair = (GuardedValuePair) that.refineMap.get(cKey2);
      if (!rawAbstracted.contains(cKey2)) {
        pair.addMoreGuards(branchConditions2);
      }
      merged.refineMap = merged.refineMap.putAndCopy(cKey2, pair);
    }
    // STEP 2: merge alias mappings
    for (Entry<String, String> entry : aliasMap.entrySet()) {
      merged.aliasMap = merged.aliasMap.putAndCopy(entry.getKey(), entry.getValue());
    }
    for (Entry<String, String> entry : that.aliasMap.entrySet()) {
      merged.aliasMap = merged.aliasMap.putAndCopy(entry.getKey(), entry.getValue());
    }
    return merged;
  }

  /**
   * Replace the concrete values in the abstraction tree by the replacement mappings, which are
   * given by merging target objects.
   */
  public void replaceConcreteValues(Map<Long, Long> leftReplaces, Map<Long, Long> rightReplaces) {
    for (Entry<Long, AbstractionPair<GuardedValue>> entry : refineMap.entrySet()) {
      Long key = entry.getKey();
      GuardedValuePair pair = (GuardedValuePair) entry.getValue();
      GuardedValue l = pair.getLeft();
      GuardedValue r = pair.getRight();
      GuardedValue newL = l.replaceValue(leftReplaces);
      GuardedValue newR = r.replaceValue(rightReplaces);
      if (l != newL || r != newR) {
        GuardedValuePair newPair = GuardedValuePair.of(newL, newR);
        refineMap = refineMap.putAndCopy(key, newPair);
      }
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(refineMap, aliasMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof GuardedAbstraction)) {
      return false;
    }
    GuardedAbstraction that = (GuardedAbstraction) obj;
    return Objects.equal(refineMap, that.refineMap) && Objects.equal(aliasMap, that.aliasMap);
  }
}
