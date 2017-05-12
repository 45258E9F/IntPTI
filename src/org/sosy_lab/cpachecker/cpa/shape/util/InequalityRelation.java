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
package org.sosy_lab.cpachecker.cpa.shape.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import java.util.List;
import java.util.Set;


/**
 * Store inequality relations in symbolic values.
 */
public class InequalityRelation {

  private final SetMultimap<Long, Long> values = HashMultimap.create();

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof InequalityRelation)) {
      return false;
    }
    InequalityRelation other = (InequalityRelation) obj;
    return values.equals(other.values);
  }

  public void addRelation(Long pV1, Long pV2) {
    if (pV1.longValue() == pV2.longValue()) {
      return;
    }
    values.put(pV1, pV2);
    values.put(pV2, pV1);
  }

  public void putAll(InequalityRelation iNeq) {
    values.putAll(iNeq.values);
  }

  public boolean relationExists(Long pV1, Long pV2) {
    return values.containsEntry(pV1, pV2);
  }

  private void removeValue(Long pV) {
    // firstly remove (*, pV)
    for (Long v2 : values.get(pV)) {
      values.get(v2).remove(pV);
    }
    // then remove (pV, *)
    values.removeAll(pV);
  }

  public void mergeValues(Long pV1, Long pV2) {
    List<Long> toBeRemoved = ImmutableList.copyOf(values.get(pV2));
    removeValue(pV2);
    for (Long val : toBeRemoved) {
      addRelation(pV1, val);
    }
  }

  public void prune(final Set<Long> pValues) {
    for (Long value : pValues) {
      removeValue(value);
    }
  }

  @Override
  public String toString() {
    return "Inequality = " + values.toString();
  }

  /**
   * Obtain the complete inequality relation.
   * Note: this method is available only when performing state merging.
   */
  public Multimap<Long, Long> getNeq() {
    Builder<Long, Long> builder = ImmutableMultimap.builder();
    builder.putAll(values);
    return builder.build();
  }
}
