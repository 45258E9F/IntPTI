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
package org.sosy_lab.cpachecker.cpa.cache;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashMap;
import java.util.Map;

public class CacheMergeOperator implements MergeOperator {

  private final MergeOperator mCachedMergeOperator;
  private final Map<Precision, Map<AbstractState, Map<AbstractState, AbstractState>>> mCache;

  public CacheMergeOperator(MergeOperator pCachedMergeOperator) {
    mCachedMergeOperator = pCachedMergeOperator;
    mCache = new HashMap<>();
  }

  @Override
  public AbstractState merge(
      AbstractState pElement1,
      AbstractState pElement2, Precision pPrecision) throws CPAException, InterruptedException {

    Map<AbstractState, Map<AbstractState, AbstractState>> lCache1 = mCache.get(pPrecision);

    if (lCache1 == null) {
      lCache1 = new HashMap<>();
      mCache.put(pPrecision, lCache1);
    }

    Map<AbstractState, AbstractState> lCache2 = lCache1.get(pElement2);

    if (lCache2 == null) {
      lCache2 = new HashMap<>();
      lCache1.put(pElement2, lCache2);
    }

    AbstractState lMergedElement = lCache2.get(pElement1);

    if (lMergedElement == null) {
      lMergedElement = mCachedMergeOperator.merge(pElement1, pElement2, pPrecision);
      lCache2.put(pElement1, lMergedElement);
    }

    return lMergedElement;
  }

}
