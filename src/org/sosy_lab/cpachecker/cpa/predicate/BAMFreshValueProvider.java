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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.annotations.VisibleForTesting;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.cpachecker.util.predicates.pathformula.FreshValueProvider;

public class BAMFreshValueProvider implements FreshValueProvider {

  private PersistentSortedMap<String, Integer> vars;

  @VisibleForTesting
  public BAMFreshValueProvider() {
    vars = PathCopyingPersistentTreeMap.of();
  }

  private BAMFreshValueProvider(final PersistentSortedMap<String, Integer> diffVars) {
    this.vars = diffVars;
  }

  @Override
  public int getFreshValue(String variable, int value) {
    if (vars.containsKey(variable) && value < vars.get(variable)) {
      value = vars.get(variable);
    }
    return value + DefaultFreshValueProvider.DEFAULT_INCREMENT; // increment for a new index
  }

  @Override
  public FreshValueProvider merge(final FreshValueProvider other) {
    if (other instanceof DefaultFreshValueProvider) {
      return this;
    } else if (other instanceof BAMFreshValueProvider) {
      PersistentSortedMap<String, Integer> vars =
          PersistentSortedMaps.merge(
              this.vars,
              ((BAMFreshValueProvider) other).vars,
              PersistentSortedMaps.<String, Integer>getMaximumMergeConflictHandler());
      return new BAMFreshValueProvider(vars);
    } else {
      throw new AssertionError("unhandled case for FreshValueProvider: " + other.getClass());
    }
  }

  public void put(String variable, int index) {
    vars = vars.putAndCopy(variable, index);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof BAMFreshValueProvider && this.vars
        .equals(((BAMFreshValueProvider) other).vars);
  }

  @Override
  public int hashCode() {
    return vars.hashCode();
  }
}
