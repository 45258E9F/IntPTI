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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;

import java.util.Collections;
import java.util.List;
import java.util.Set;

final class NeqRelation {

  /**
   * The Multimap is used as Bi-Map, i.e. each pair (K,V) is also
   * inserted as pair (V,K). We avoid self-references like (A,A).
   */
  private final SetMultimap<Integer, Integer> smgValues = HashMultimap.create();

  @Override
  public int hashCode() {
    return smgValues.hashCode();
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    return Collections.unmodifiableSet(smgValues.get(pV));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NeqRelation other = (NeqRelation) obj;
    return other.smgValues != null && smgValues.equals(other.smgValues);
  }

  public void add_relation(Integer pOne, Integer pTwo) {

    // we do not want self-references
    if (pOne.intValue() == pTwo.intValue()) {
      return;
    }

    smgValues.put(pOne, pTwo);
    smgValues.put(pTwo, pOne);
  }

  public void putAll(NeqRelation pNeq) {
    smgValues.putAll(pNeq.smgValues);
  }

  public void remove_relation(Integer pOne, Integer pTwo) {
    smgValues.remove(pOne, pTwo);
    smgValues.remove(pTwo, pOne);
  }

  public boolean neq_exists(Integer pOne, Integer pTwo) {
    return smgValues.containsEntry(pOne, pTwo);
  }

  public void removeValue(Integer pOne) {
    for (Integer other : smgValues.get(pOne)) {
      smgValues.get(other).remove(pOne);
    }
    smgValues.removeAll(pOne);
  }

  /**
   * transform all relations from (A->C) towards (A->B) and delete C
   */
  public void mergeValues(Integer pB, Integer pC) {
    List<Integer> values = ImmutableList.copyOf(smgValues.get(pC));
    removeValue(pC);
    for (Integer value : values) {
      add_relation(pB, value);
    }
  }

  @Override
  public String toString() {
    return "neq_rel=" + smgValues.toString();
  }
}