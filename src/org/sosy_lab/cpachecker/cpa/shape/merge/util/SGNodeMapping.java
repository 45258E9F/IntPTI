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
package org.sosy_lab.cpachecker.cpa.shape.merge.util;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class SGNodeMapping {

  private final Map<SGObject, SGObject> objectMap = new HashMap<>();

  private final Set<Long> waitSet = new HashSet<>();

  /**
   * This map stores the mapping relation from the old address to the new address. This applies
   * when we intend to pend an address of a re-allocated object.
   */
  private final Map<Long, Long> weirdMap = new HashMap<>();

  /**
   * This map stores the mapping relation from the original address and the merged address. It is
   * possible that the merged value and the original one are not equal literally.
   */
  private final Map<Long, Long> mergeMap = new HashMap<>();

  public SGNodeMapping() {
  }


  /* ************** */
  /* map operations */
  /* ************** */

  @Nullable
  public SGObject get(SGObject keyObject) {
    return objectMap.get(keyObject);
  }

  public void put(SGObject key, SGObject value) {
    if (key != CShapeGraph.getNullObject()) {
      objectMap.put(key, value);
    }
  }

  public boolean containsKey(SGObject key) {
    return objectMap.containsKey(key);
  }

  public boolean containsValue(SGObject value) {
    return objectMap.containsValue(value);
  }

  public Set<SGObject> getMappedObjects() {
    return objectMap.keySet();
  }

  public void put(Long oldValue, Long newValue) {
    if (!oldValue.equals(newValue)) {
      mergeMap.put(oldValue, newValue);
    }
  }

  @Nullable
  public Long getReplacement(Long key) {
    Long result = weirdMap.get(key);
    return result != null ? result : mergeMap.get(key);
  }

  public Map<Long, Long> getReplacement() {
    return ImmutableMap.copyOf(weirdMap);
  }

  /* ************** */
  /* set operations */
  /* ************** */

  public void addPending(Long pAddress) {
    // Invariant: pending set should not contain known pointer (i.e. null address)
    if (!pAddress.equals(CShapeGraph.getNullAddress())) {
      waitSet.add(pAddress);
    }
  }

  public void addPending(Long pOldAddress, Long pNewAddress) {
    if (!pOldAddress.equals(CShapeGraph.getNullAddress()) &&
        !pNewAddress.equals(CShapeGraph.getNullAddress())) {
      waitSet.add(pOldAddress);
      weirdMap.put(pOldAddress, pNewAddress);
    }
  }

  public void removePending(Long pAddress) {
    waitSet.remove(pAddress);
  }

  public void resetPending() {
    waitSet.clear();
  }

  public Set<Long> getPendings() {
    return Collections.unmodifiableSet(waitSet);
  }

  public boolean isPending(Long pV) {
    return waitSet.contains(pV);
  }

  /* ********* */
  /* overrides */
  /* ********* */

  @Override
  public int hashCode() {
    return Objects.hashCode(objectMap, waitSet, weirdMap, mergeMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof SGNodeMapping)) {
      return false;
    }
    SGNodeMapping other = (SGNodeMapping) obj;
    return Objects.equal(objectMap, other.objectMap) &&
        Objects.equal(waitSet, other.waitSet) &&
        Objects.equal(weirdMap, other.weirdMap) &&
        Objects.equal(mergeMap, other.mergeMap);
  }

}
