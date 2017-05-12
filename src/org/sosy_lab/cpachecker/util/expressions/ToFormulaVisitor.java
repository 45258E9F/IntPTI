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
package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ToFormulaVisitor
    extends CachingVisitor<AExpression, BooleanFormula, ToFormulaException> {

  private static final CFANode DUMMY_NODE = new CFANode("dummy");

  private final FormulaManagerView formulaManagerView;

  private final PathFormulaManager pathFormulaManager;

  private final PathFormula context;

  public ToFormulaVisitor(
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPathFormulaManager,
      PathFormula pClearContext) {
    formulaManagerView = Objects.requireNonNull(pFormulaManagerView);
    pathFormulaManager = Objects.requireNonNull(pPathFormulaManager);
    context = pClearContext;
  }

  @Override
  protected BooleanFormula cacheMissAnd(And<AExpression> pAnd) throws ToFormulaException {
    List<BooleanFormula> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pAnd) {
      elements.add(element.accept(this));
    }
    return formulaManagerView.getBooleanFormulaManager().and(elements);
  }

  @Override
  protected BooleanFormula cacheMissOr(Or<AExpression> pOr) throws ToFormulaException {
    List<BooleanFormula> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pOr) {
      elements.add(element.accept(this));
    }
    return formulaManagerView.getBooleanFormulaManager().or(elements);
  }

  @Override
  protected BooleanFormula cacheMissLeaf(LeafExpression<AExpression> pLeafExpression)
      throws ToFormulaException {
    AExpression expression = pLeafExpression.getExpression();
    final CFAEdge edge;
    if (expression instanceof CExpression) {
      edge =
          new CAssumeEdge("", FileLocation.DUMMY, DUMMY_NODE, DUMMY_NODE, (CExpression) expression,
              pLeafExpression.assumeTruth());
    } else if (expression instanceof JExpression) {
      edge =
          new JAssumeEdge("", FileLocation.DUMMY, DUMMY_NODE, DUMMY_NODE, (JExpression) expression,
              pLeafExpression.assumeTruth());
    } else {
      throw new AssertionError("Unsupported expression type.");
    }
    PathFormula invariantPathFormula;
    try {
      if (context == null) {
        invariantPathFormula =
            pathFormulaManager.makeFormulaForPath(Collections.<CFAEdge>singletonList(edge));
      } else {
        PathFormula clearContext = pathFormulaManager.makeEmptyPathFormula(context);
        invariantPathFormula = pathFormulaManager.makeAnd(clearContext, edge);
      }
    } catch (CPATransferException e) {
      throw new ToFormulaException(e);
    } catch (InterruptedException e) {
      throw new ToFormulaException(e);
    }
    return formulaManagerView.uninstantiate(invariantPathFormula.getFormula());
  }

  @Override
  protected BooleanFormula cacheMissTrue() {
    return formulaManagerView.getBooleanFormulaManager().makeBoolean(true);
  }

  @Override
  protected BooleanFormula cacheMissFalse() {
    return formulaManagerView.getBooleanFormulaManager().makeBoolean(false);
  }

  public static class ToFormulaException extends Exception {

    private static final long serialVersionUID = -3849941975554955994L;

    private final CPATransferException transferException;

    private final InterruptedException interruptedException;

    private ToFormulaException(CPATransferException pTransferException) {
      super(pTransferException);
      this.transferException = Objects.requireNonNull(pTransferException);
      this.interruptedException = null;
    }

    private ToFormulaException(InterruptedException pInterruptedException) {
      super(pInterruptedException);
      this.transferException = null;
      this.interruptedException = Objects.requireNonNull(pInterruptedException);
    }

    public boolean isTransferException() {
      return transferException != null;
    }

    public boolean isInterruptedException() {
      return interruptedException != null;
    }

    public CPATransferException asTransferException() {
      Preconditions.checkState(isTransferException());
      return transferException;
    }

    public InterruptedException asInterruptedException() {
      Preconditions.checkState(isInterruptedException());
      return interruptedException;
    }

  }

}
