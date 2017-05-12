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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.shape.graphs.ShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.BasicValuePair;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An abstract class for value abstraction. It contains simple implementation of basic operations
 * such as value merging and structural pruning.
 *
 * @param <T> the type of value abstraction pair
 */
public abstract class BasicAbstraction<T extends AbstractValue> implements GeneralAbstraction<T> {

  PersistentMap<Long, AbstractionPair<T>> refineMap;
  PersistentMap<String, String> aliasMap;

  BasicAbstraction() {
    refineMap = PathCopyingPersistentTreeMap.of();
    aliasMap = PathCopyingPersistentTreeMap.of();
  }

  @Override
  public void addNameAlias(String mainName, String hiddenName) {
    aliasMap = aliasMap.putAndCopy(mainName, hiddenName);
  }

  @Override
  public Collection<String> getAlias(String name) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    Set<String> unvisited = new HashSet<>();
    unvisited.add(name);
    do {
      Set<String> children = new HashSet<>();
      for (String uv : unvisited) {
        String subAlias = aliasMap.get(uv);
        if (subAlias != null) {
          children.add(subAlias);
        }
      }
      builder.addAll(unvisited);
      unvisited.clear();
      unvisited.addAll(children);
    } while (!unvisited.isEmpty());

    return builder.build();
  }

  @Override
  public Collection<T> interpret(Long v) {
    return null;
  }

  public void putAll(BasicAbstraction<T> that) {
    for (Entry<Long, AbstractionPair<T>> pair : that.refineMap.entrySet()) {
      refineMap = refineMap.putAndCopy(pair.getKey(), pair.getValue());
    }
    for (Entry<String, String> pair : that.aliasMap.entrySet()) {
      aliasMap = aliasMap.putAndCopy(pair.getKey(), pair.getValue());
    }
  }

  /* ************* */
  /* miscellaneous */
  /* ************* */

  /**
   * Prune abstractions involving unused symbolic values.
   */
  public void pruneValues(final Set<Long> pUnusedValues, Set<BasicValuePair> pNewMerges) {
    Set<Entry<Long, AbstractionPair<T>>> entrySet = new HashSet<>(refineMap.entrySet());
    for (Entry<Long, AbstractionPair<T>> entry : entrySet) {
      Long key = entry.getKey();
      Long con1 = entry.getValue().getLeftValue();
      Long con2 = entry.getValue().getRightValue();
      if (pUnusedValues.contains(key)) {
        refineMap = refineMap.removeAndCopy(key);
      } else {
        boolean pruneLeft = pUnusedValues.contains(con1);
        boolean pruneRight = pUnusedValues.contains(con2);
        if (pruneLeft || pruneRight) {
          refineMap = refineMap.removeAndCopy(key);
          if (!pruneLeft) {
            // only the right value is pruned
            pNewMerges.add(BasicValuePair.of(key, con1));
          } else if (!pruneRight) {
            pNewMerges.add(BasicValuePair.of(key, con2));
          }
        }
      }
    }
  }

  /**
   * Prune the abstraction of the single specified symbolic value.
   */
  public void pruneValue(final Long pValue) {
    if (refineMap.containsKey(pValue)) {
      refineMap = refineMap.removeAndCopy(pValue);
    }
  }

  /**
   * Prune name alias involving removed objects.
   */
  public void pruneObject(SGObject pObject) {
    aliasMap = aliasMap.removeAndCopy(pObject.getLabel());
  }

  /**
   * Merge symbolic values in abstraction. Value merge may introduce new value pairs to be merged.
   */
  public void mergeValues(long newValue, long oldValue, Set<BasicValuePair> pNewMerges) {
    if (newValue == oldValue) {
      return;
    }
    // Invariant: if (x,y) = z, then it is impossible that (z,*) = x. More intuitively, each
    // value should appear in the abstraction tree for only once.
    if (refineMap.containsKey(oldValue)) {
      AbstractionPair<T> oldPair = refineMap.get(oldValue);
      refineMap = refineMap.removeAndCopy(oldValue);
      Long oldLeft = oldPair.getLeftValue();
      Long oldRight = oldPair.getRightValue();
      if (oldLeft == newValue) {
        // Assume the old value is n and the new value is m. If we have (m,x) = n and (*,*) = m,
        // then we merge x and n with m. The abstraction of m is independent with the abstraction
        // of n.
        pNewMerges.add(BasicValuePair.of(oldRight, newValue));
      } else if (oldRight == newValue) {
        pNewMerges.add(BasicValuePair.of(oldLeft, newValue));
      } else {
        if (refineMap.containsKey(newValue)) {
          AbstractionPair<T> newPair = refineMap.get(newValue);
          if (!oldPair.equals(newPair)) {
            refineMap = refineMap.removeAndCopy(newValue);
          }
          // Otherwise, there already exists the target abstraction.
        } else {
          refineMap = refineMap.putAndCopy(newValue, oldPair);
        }
      }
    }
    // copy the entry set to prevent issues in iteration
    Set<Entry<Long, AbstractionPair<T>>> entrySet = new HashSet<>(refineMap.entrySet());
    for (Entry<Long, AbstractionPair<T>> entry : entrySet) {
      Long key = entry.getKey();
      AbstractionPair<T> pair = entry.getValue();
      Long left = pair.getLeftValue();
      Long right = pair.getRightValue();
      if (left == oldValue) {
        if (right == newValue) {
          refineMap = refineMap.removeAndCopy(key);
          pNewMerges.add(BasicValuePair.of(key, newValue));
        } else {
          pair.updateLeftValue(newValue);
          refineMap = refineMap.putAndCopy(key, pair);
        }
      } else if (right == oldValue) {
        if (left == newValue) {
          refineMap = refineMap.removeAndCopy(key);
          pNewMerges.add(BasicValuePair.of(key, newValue));
        } else {
          pair.updateRightValue(newValue);
          refineMap = refineMap.putAndCopy(key, pair);
        }
      }
    }
  }

  /**
   * Merge two abstractions.
   *
   * @param pNewMerges        new value merges introduced after state merging
   * @param mergedAbstraction initially merged abstraction derived from raw merging (state merge
   *                          without the prior abstraction knowledge). This is used to prevent
   *                          duplicated abstraction information to be added
   * @return the merged abstraction
   */
  public abstract BasicAbstraction<T> merge(
      ShapeGraph thisGraph,
      BasicAbstraction<T> thatAbstraction,
      ShapeGraph thatGraph,
      Set<BasicValuePair> pNewMerges,
      BasicAbstraction<T> mergedAbstraction);

  public Map<Long, BasicValuePair> getValueAbstraction() {
    ImmutableMap.Builder<Long, BasicValuePair> builder = ImmutableMap.builder();
    for (Entry<Long, AbstractionPair<T>> entry : refineMap.entrySet()) {
      AbstractionPair<T> pair = entry.getValue();
      builder.put(entry.getKey(), BasicValuePair.of(pair.getLeftValue(), pair.getRightValue()));
    }
    return builder.build();
  }

  /**
   * Check whether the value v2 is abstracted from the value v1.
   * Note: v1 is more concrete while v2 is more abstract.
   */
  public boolean abstractFromTo(Long v1, Long v2) {
    Deque<Long> workList = new ArrayDeque<>();
    workList.add(v2);
    while (!workList.isEmpty()) {
      Long absV = workList.poll();
      if (absV.equals(v1)) {
        return true;
      }
      AbstractionPair<T> pair = refineMap.get(absV);
      if (pair != null) {
        workList.add(pair.getLeftValue());
        workList.add(pair.getRightValue());
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return refineMap.isEmpty();
  }

}
