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
package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * This alias creator only allows the identity as a valid alias.
 */
public class IdentityAliasCreator implements AliasCreator {

  @Override
  public Set<Environment> getPossibleAliases(
      final Collection<? extends SymbolicValue> pFirstValues,
      final Collection<? extends SymbolicValue> pSecondValues
  ) {

    Collection<? extends SymbolicValue> biggerState;
    Collection<? extends SymbolicValue> smallerState;

    if (pFirstValues.size() > pSecondValues.size()) {
      biggerState = pFirstValues;
      smallerState = pSecondValues;
    } else {
      biggerState = pSecondValues;
      smallerState = pFirstValues;
    }

    for (SymbolicValue v : smallerState) {
      if (!biggerState.contains(v)) {
        return Collections.emptySet();
      }
    }

    return Collections.singleton(buildEnvironment(smallerState));
  }

  private Environment buildEnvironment(Collection<? extends SymbolicValue> pValues) {
    Environment e = new Environment();

    Collection<SymbolicIdentifier> allIds = SymbolicValues.getContainedSymbolicIdentifiers(pValues);

    for (SymbolicIdentifier i : allIds) {
      e.addAlias(i, i);
    }

    for (SymbolicValue v : pValues) {
      e.addCounterpart(v, v);
    }

    return e;
  }
}
