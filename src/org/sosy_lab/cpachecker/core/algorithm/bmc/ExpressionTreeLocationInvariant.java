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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Map;
import java.util.Objects;


public class ExpressionTreeLocationInvariant extends AbstractLocationFormulaInvariant
    implements ExpressionTreeCandidateInvariant {

  private final ExpressionTree<AExpression> expressionTree;

  private final CFANode location;

  private final String groupId;

  private final Map<ManagerKey, ToFormulaVisitor> visitorCache;

  public ExpressionTreeLocationInvariant(
      String pGroupId, CFANode pLocation, ExpressionTree<AExpression> pExpressionTree) {
    this(pGroupId, pLocation, pExpressionTree, null);
  }

  public ExpressionTreeLocationInvariant(
      String pGroupId,
      CFANode pLocation,
      ExpressionTree<AExpression> pExpressionTree,
      Map<ManagerKey, ToFormulaVisitor> pVisitorCache) {
    super(pLocation);
    groupId = Objects.requireNonNull(pGroupId);
    location = Objects.requireNonNull(pLocation);
    expressionTree = Objects.requireNonNull(pExpressionTree);
    visitorCache = pVisitorCache;
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    ManagerKey key = null;
    PathFormula clearContext = pContext == null ? null : pPFMGR.makeEmptyPathFormula(pContext);
    ToFormulaVisitor toFormulaVisitor = null;
    if (visitorCache != null) {
      key = new ManagerKey(pFMGR, pPFMGR, clearContext);
      toFormulaVisitor = visitorCache.get(key);
    }
    if (toFormulaVisitor == null) {
      toFormulaVisitor = new ToFormulaVisitor(pFMGR, pPFMGR, clearContext);
      if (visitorCache != null) {
        visitorCache.put(key, toFormulaVisitor);
      }
    }
    try {
      return expressionTree.accept(toFormulaVisitor);
    } catch (ToFormulaException e) {
      if (e.isInterruptedException()) {
        throw e.asInterruptedException();
      }
      throw e.asTransferException();
    }
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    if (expressionTree.equals(ExpressionTrees.getFalse())) {
      Iterable<AbstractState> infeasibleStates =
          AbstractStates.filterLocations(pReachedSet, getLocations()).toList();
      pReachedSet.removeAll(infeasibleStates);
      for (ARGState s : from(infeasibleStates).filter(ARGState.class)) {
        s.removeFromARG();
      }
    }
  }

  @Override
  public ExpressionTree<Object> asExpressionTree() {
    return ExpressionTrees.cast(expressionTree);
  }

  public String getGroupId() {
    return groupId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, location, expressionTree, System.identityHashCode(visitorCache));
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof ExpressionTreeLocationInvariant) {
      ExpressionTreeLocationInvariant other = (ExpressionTreeLocationInvariant) pObj;
      return groupId.equals(other.groupId)
          && location.equals(other.location)
          && expressionTree.equals(other.expressionTree)
          && visitorCache == other.visitorCache;
    }
    return false;
  }

  @Override
  public String toString() {
    return groupId + " at " + location + ": " + expressionTree.toString();
  }

  public static class ManagerKey {

    private final FormulaManagerView formulaManagerView;

    private final PathFormulaManager pathFormulaManager;

    private final PathFormula clearContext;

    public ManagerKey(
        FormulaManagerView pFormulaManagerView,
        PathFormulaManager pPathFormulaManager,
        PathFormula pClearContext) {
      formulaManagerView = Objects.requireNonNull(pFormulaManagerView);
      pathFormulaManager = Objects.requireNonNull(pPathFormulaManager);
      clearContext = pClearContext;
    }

    @Override
    public int hashCode() {
      return Objects.hash(formulaManagerView, pathFormulaManager);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof ManagerKey) {
        ManagerKey other = (ManagerKey) pObj;
        return formulaManagerView == other.formulaManagerView
            && pathFormulaManager == other.pathFormulaManager
            && Objects.equals(clearContext, other.clearContext);
      }
      return false;
    }

  }

  public CFANode getLocation() {
    return location;
  }

}
