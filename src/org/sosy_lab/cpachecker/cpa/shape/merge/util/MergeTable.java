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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import org.sosy_lab.cpachecker.cpa.shape.util.EquivalenceRelation;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

public final class MergeTable {

  private Table<Long, Long, Long> mergeTable = TreeBasedTable.create();

  private EquivalenceRelation<Long> eqRel1;
  private EquivalenceRelation<Long> eqRel2;

  private EquivalenceRelation<Long> mergeRel;

  private Table<Long, Long, KnownExplicitValue> explicitTable = TreeBasedTable.create();
  private BiMap<Long, KnownExplicitValue> newExplicitRelation = HashBiMap.create();

  /**
   * Create a quick merge table using equivalence relations from two states to be merged.
   */
  public MergeTable(EquivalenceRelation<Long> r1, EquivalenceRelation<Long> r2) {
    eqRel1 = r1;
    eqRel2 = r2;
    mergeRel = r1.merge(r2);
    // Invariant: for (v1, v2) = v where v is not a fresh value, then there does not exist
    // another (v1',v2') such that (v1',v2') = v and (v1,v2) \= (v1',v2').
    Collection<Long> tos = mergeRel.getRepresentatives();
    for (Long to : tos) {
      Long from1 = r1.getRepresentative(to);
      Long from2 = r2.getRepresentative(to);
      mergeTable.put(from1, from2, to);
    }
  }

  @Nullable
  public Long merge(Long from1, Long from2) {
    if (from1.equals(from2)) {
      return from1;
    }
    return mergeTable.get(from1, from2);
  }

  public EquivalenceRelation<Long> getMergedEq() {
    return mergeRel;
  }

  public Set<Long> getEquivalentValuesFromRel1ToRel2(Long v) {
    Set<Long> eqVS = eqRel1.getEquivalentValues(v);
    ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
    for (Long eqV : eqVS) {
      builder.add(eqRel2.getRepresentative(eqV));
    }
    return builder.build();
  }

  public Set<Long> getEquivalentValuesFromRel2ToRel1(Long v) {
    Set<Long> eqVs = eqRel2.getEquivalentValues(v);
    ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
    for (Long eqV : eqVs) {
      builder.add(eqRel1.getRepresentative(eqV));
    }
    return builder.build();
  }

  public Long getRepresentativeForRel1(Long v) {
    return eqRel1.getRepresentative(v);
  }

  public Long getRepresentativeForRel2(Long v) {
    return eqRel2.getRepresentative(v);
  }

  /* ******************** */
  /* explicit value query */
  /* ******************** */

  public void putExplicitEquality(Long v1, Long v2, KnownExplicitValue expValue) {
    explicitTable.put(v1, v2, expValue);
  }

  @Nullable
  public KnownExplicitValue getExplicitEquality(Long v1, Long v2) {
    return explicitTable.get(v1, v2);
  }

  public void addExplicitRelation(Long symValue, KnownExplicitValue expValue) {
    newExplicitRelation.put(symValue, expValue);
  }

  @Nullable
  public Long getAssociatedSymbolic(KnownExplicitValue expValue) {
    return newExplicitRelation.inverse().get(expValue);
  }

  public BiMap<Long, KnownExplicitValue> getNewExplicitRelation() {
    return newExplicitRelation;
  }

  /**
   * Query for equality of two symbolic values.
   */
  public boolean isEq(Long v1, Long v2) {
    if (v1.equals(v2)) {
      return true;
    }
    if (mergeTable.get(v1, v2) != null) {
      return true;
    }
    if (explicitTable.get(v1, v2) != null) {
      return true;
    }
    return false;
  }

}
