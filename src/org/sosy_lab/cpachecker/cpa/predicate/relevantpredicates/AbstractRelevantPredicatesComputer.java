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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import com.google.common.collect.Maps;
import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractRelevantPredicatesComputer<T> implements RelevantPredicatesComputer {

  protected final FormulaManagerView fmgr;

  protected final Map<Pair<T, AbstractionPredicate>, Boolean> relevantPredicates =
      Maps.newHashMap();

  protected AbstractRelevantPredicatesComputer(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  @Override
  public Set<AbstractionPredicate> getRelevantPredicates(
      Block context,
      Collection<AbstractionPredicate> predicates) {
    Set<AbstractionPredicate> result = new HashSet<>(predicates.size());

    T precomputeResult = precompute(context, predicates);

    for (AbstractionPredicate predicate : predicates) {
      if (isRelevant0(precomputeResult, predicate)) {
        result.add(predicate);
      }
    }
    return result;
  }

  private boolean isRelevant0(T pPrecomputeResult, AbstractionPredicate pPredicate) {

    // lookup in cache
    Pair<T, AbstractionPredicate> key = Pair.of(pPrecomputeResult, pPredicate);
    Boolean cacheResult = relevantPredicates.get(key);
    if (cacheResult != null) {
      return cacheResult;
    }

    boolean result;
    if (fmgr.getBooleanFormulaManager().isFalse(pPredicate.getSymbolicAtom())
        || fmgr.extractVariableNames(pPredicate.getSymbolicAtom()).isEmpty()) {
      result = true;
    } else {
      String predicateString = pPredicate.getSymbolicAtom().toString();
      if (predicateString.contains("false") || predicateString.contains("retval") || predicateString
          .contains("nondet")) {
        result = true;
      } else {
        result = isRelevant(pPrecomputeResult, pPredicate);
      }
    }

    relevantPredicates.put(key, result);
    return result;
  }

  protected abstract boolean isRelevant(T pPrecomputeResult, AbstractionPredicate pPredicate);

  @ForOverride
  protected abstract T precompute(Block pContext, Collection<AbstractionPredicate> pPredicates);

  @Override
  public Set<AbstractionPredicate> getIrrelevantPredicates(
      Block context,
      Collection<AbstractionPredicate> predicates) {

    Set<AbstractionPredicate> result = new HashSet<>(predicates);
    result.removeAll(getRelevantPredicates(context, predicates));

    return result;
  }
}
