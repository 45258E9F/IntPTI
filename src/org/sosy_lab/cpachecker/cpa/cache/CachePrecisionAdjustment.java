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

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashMap;
import java.util.Map;

/*
 * CAUTION: The cache for precision adjustment is only correct for CPAs that do
 * _NOT_ depend on the reached set when performing prec.
 */
public class CachePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment mCachedPrecisionAdjustment;

  private final Map<Precision, Map<AbstractState, Optional<PrecisionAdjustmentResult>>> mCache;

  public CachePrecisionAdjustment(PrecisionAdjustment pCachedPrecisionAdjustment) {
    mCachedPrecisionAdjustment = pCachedPrecisionAdjustment;
    //mCache = new HashMap<AbstractState, Map<Precision, Triple<AbstractState, Precision, Action>>>();
    mCache = new HashMap<>();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    Map<AbstractState, Optional<PrecisionAdjustmentResult>> lCache = mCache.get(pPrecision);

    if (lCache == null) {
      lCache = new HashMap<>();
      mCache.put(pPrecision, lCache);
    }

    Optional<PrecisionAdjustmentResult> lResult = lCache.get(pElement);

    if (lResult == null) {
      lResult = mCachedPrecisionAdjustment.prec(
          pElement, pPrecision, pElements, projection, fullState);
      lCache.put(pElement, lResult);
    }

    return lResult;
  }

}
