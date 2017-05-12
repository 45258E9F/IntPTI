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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Store equality in symbolic values.
 * Note: The equality relation is incremental, thus no entry is removed unless the involved value
 * becomes unused after a certain point.
 * When two values v1, v2 are to be merged, several data structures can be changed:
 * (1) the explicit bidirectional mapping
 * (2) inequality relation
 * (3) constraint pool
 * After introducing equality relation structure, we add a constraint instead of performing
 * propagation on constraints for better state merge. However, propagation is still applicable
 * for explicit value mapping and inequality relation. When merging two states, we can replace
 * the symbol with any one in the same equality closure.
 */
public class EqualityRelation {

  private final EquivalenceRelation<Long> eqs = EquivalenceRelation.of();

  @Override
  public int hashCode() {
    return eqs.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof EqualityRelation)) {
      return false;
    }
    EqualityRelation that = (EqualityRelation) obj;
    return eqs.equals(that.eqs);
  }

  /**
   * Add equality relation between two symbolic values.
   *
   * @return whether the equality relation already exists.
   */
  public boolean addRelation(Long pV1, Long pV2) {
    return pV1.equals(pV2) || eqs.addRelation(pV1, pV2);
  }

  public void putAll(EqualityRelation iEq) {
    eqs.addAll(iEq.eqs);
  }

  public void putAll(EquivalenceRelation<Long> iEqs) {
    eqs.addAll(iEqs);
  }

  public Long getRepresentative(Long pKey) {
    return eqs.getRepresentative(pKey);
  }

  public EquivalenceRelation<Long> getEq() {
    return eqs;
  }

  /**
   * Derive the full equivalence closure for the specified values with respect to the
   * current equivalence relation.
   */
  public Set<Long> getClosures(Set<Long> values) {
    ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
    for (Long value : values) {
      builder.addAll(eqs.getEquivalentValues(value));
    }
    return builder.build();
  }

}
