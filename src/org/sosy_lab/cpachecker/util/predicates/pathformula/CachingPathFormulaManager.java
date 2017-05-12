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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model.ValueAssignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link PathFormulaManager} that delegates to another
 * instance but caches results of some methods.
 */
public class CachingPathFormulaManager implements PathFormulaManager {

  public final Timer pathFormulaComputationTimer = new Timer();
  public int pathFormulaCacheHits = 0;

  public final PathFormulaManager delegate;

  private final Map<Pair<CFAEdge, PathFormula>, Pair<PathFormula, ErrorConditions>>
      andFormulaWithConditionsCache
      = new HashMap<>();
  private final Map<Pair<CFAEdge, PathFormula>, PathFormula> andFormulaCache
      = new HashMap<>();

  private final Map<Pair<PathFormula, PathFormula>, PathFormula> orFormulaCache
      = new HashMap<>();

  private final Map<PathFormula, PathFormula> emptyFormulaCache
      = new HashMap<>();

  private final PathFormula emptyFormula;

  public CachingPathFormulaManager(PathFormulaManager pDelegate) {
    delegate = pDelegate;
    emptyFormula = delegate.makeEmptyPathFormula();
  }

  @Override
  public Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(
      PathFormula pOldFormula,
      CFAEdge pEdge) throws CPATransferException, InterruptedException {

    final Pair<CFAEdge, PathFormula> formulaCacheKey = Pair.of(pEdge, pOldFormula);
    Pair<PathFormula, ErrorConditions> result = andFormulaWithConditionsCache.get(formulaCacheKey);
    if (result == null) {
      pathFormulaComputationTimer.start();
      // compute new pathFormula with the operation on the edge
      result = delegate.makeAndWithErrorConditions(pOldFormula, pEdge);
      pathFormulaComputationTimer.stop();
      andFormulaWithConditionsCache.put(formulaCacheKey, result);

    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    final Pair<CFAEdge, PathFormula> formulaCacheKey = Pair.of(pEdge, pOldFormula);
    PathFormula result = andFormulaCache.get(formulaCacheKey);
    if (result == null) {
      pathFormulaComputationTimer.start();
      // compute new pathFormula with the operation on the edge
      result = delegate.makeAnd(pOldFormula, pEdge);
      pathFormulaComputationTimer.stop();
      andFormulaCache.put(formulaCacheKey, result);

    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) throws InterruptedException {
    final Pair<PathFormula, PathFormula> formulaCacheKey = Pair.of(pF1, pF2);

    PathFormula result = orFormulaCache.get(formulaCacheKey);
    if (result == null) {
      // try again with other order
      result = orFormulaCache.get(Pair.of(pF2, pF1));
    }

    if (result == null) {
      result = delegate.makeOr(pF1, pF2);
      orFormulaCache.put(formulaCacheKey, result);
    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return emptyFormula;
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula pOldFormula) {
    if (pOldFormula.getFormula() == null) {
      return delegate.makeEmptyPathFormula(pOldFormula);
    }
    PathFormula result = emptyFormulaCache.get(pOldFormula);
    if (result == null) {
      result = delegate.makeEmptyPathFormula(pOldFormula);
      emptyFormulaCache.put(pOldFormula, result);
    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    return delegate.makeAnd(pPathFormula, pOtherFormula);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula pOldFormula, SSAMap pM) {
    return delegate.makeNewPathFormula(pOldFormula, pM);
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException {
    return delegate.makeFormulaForPath(pPath);
  }

  @Override
  public BooleanFormula buildBranchingFormula(Iterable<ARGState> pElementsOnPath)
      throws CPATransferException, InterruptedException {
    return delegate.buildBranchingFormula(pElementsOnPath);
  }

  @Override
  public Map<Integer, Boolean> getBranchingPredicateValuesFromModel(Iterable<ValueAssignment> pModel) {
    return delegate.getBranchingPredicateValuesFromModel(pModel);
  }

  @Override
  public Formula expressionToFormula(
      PathFormula pFormula, CIdExpression expr,
      CFAEdge edge) throws UnrecognizedCCodeException {
    return delegate.expressionToFormula(pFormula, expr, edge);
  }

  @Override
  public BooleanFormula buildImplicationTestAsUnsat(PathFormula pF1, PathFormula pF2)
      throws InterruptedException {
    return delegate.buildImplicationTestAsUnsat(pF1, pF2);
  }

}
