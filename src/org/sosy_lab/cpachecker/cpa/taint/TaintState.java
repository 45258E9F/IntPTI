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
package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.annotation.Nullable;


public class TaintState implements LatticeAbstractState<TaintState>, SwitchableGraphable {

  private TaintState stateOnLastFunctionCall;

  private TaintTree localState;
  private TaintTree globalState;

  /**
   * Taint status of an access path.
   */
  enum Taint {
    /**
     * clean access path should not be recorded in the tree
     */
    CLEAN,
    TAINT
  }

  public TaintState() {
    this.stateOnLastFunctionCall = null;
    this.localState = new TaintTree();
    this.globalState = new TaintTree();
  }

  public TaintState(
      TaintState pStateOnLastFunctionCall,
      TaintTree pLocalState, TaintTree pGlobalState) {
    this.stateOnLastFunctionCall = pStateOnLastFunctionCall;
    this.localState = pLocalState;
    this.globalState = pGlobalState;
  }

  public static TaintState copyOf(TaintState pState) {
    return new TaintState(pState.stateOnLastFunctionCall, pState.localState, pState.globalState);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(localState, globalState, stateOnLastFunctionCall);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof TaintState)) {
      return false;
    }
    TaintState that = (TaintState) obj;
    return Objects.equal(localState, that.localState) && Objects.equal(globalState, that
        .globalState) && Objects.equal(stateOnLastFunctionCall, that.stateOnLastFunctionCall);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  public Taint evaluateTaint(CRightHandSide exp, List<AbstractState> otherStates) {
    Taint taint;
    try {
      taint = exp.accept(new ExpressionTaintVisitor(this, otherStates));
    } catch (UnrecognizedCCodeException ex) {
      // if the expression cannot be correctly recognized, the result should be CLEAN
      // ASSUMPTION: taint sources are always correctly recognized
      taint = Taint.CLEAN;
    }
    return taint;
  }

  public Taint queryTaint(@Nullable AccessPath path) {
    if (path == null) {
      return Taint.CLEAN;
    }
    if (path.startFromGlobal()) {
      return globalState.queryTaintInfo(path);
    } else {
      return localState.queryTaintInfo(path);
    }
  }

  private TaintState addLocalTaint(AccessPath path, Taint pTaint) {
    return new TaintState(
        this.stateOnLastFunctionCall,
        this.localState.setTaint(path, pTaint, true),
        this.globalState
    );
  }

  private TaintState addGlobalTaint(AccessPath path, Taint pTaint) {
    return new TaintState(
        this.stateOnLastFunctionCall,
        this.localState,
        this.globalState.setTaint(path, pTaint, true)
    );
  }

  public TaintTree getLocalState() {
    return localState;
  }

  public TaintState pushCallStack(
      List<CParameterDeclaration> parameters, List<CExpression>
      arguments, List<AbstractState> pOtherStates) {
    assert (parameters.size() == arguments.size());
    TaintTree newLocalState = new TaintTree();
    for (int i = 0; i < parameters.size(); i++) {
      CParameterDeclaration param = parameters.get(i);
      CExpression argument = arguments.get(i);
      AccessPath path = new AccessPath(param);
      Taint taint = evaluateTaint(argument, pOtherStates);
      newLocalState = newLocalState.setTaint(path, taint, true);
    }
    return new TaintState(this, newLocalState, this.globalState);
  }

  /**
   * Parameters of entry function are taint values because we do not know specification on them
   * (out of our analysis scope)
   */
  public TaintState pushEntryCallStack(List<CParameterDeclaration> parameters) {
    TaintTree newLocalState = new TaintTree();
    for (CParameterDeclaration cpd : parameters) {
      newLocalState = newLocalState.setTaint(new AccessPath(cpd), Taint.TAINT, true);
    }
    return new TaintState(this, newLocalState, this.globalState);
  }

  public TaintState popCallStack() {
    Preconditions.checkNotNull(stateOnLastFunctionCall);
    return new TaintState(stateOnLastFunctionCall.stateOnLastFunctionCall,
        stateOnLastFunctionCall.localState, globalState);
  }

  public TaintState updateTaint(AccessPath path, Taint pTaint) {
    if (path == null) {
      return this;
    }
    if (path.startFromGlobal()) {
      return addGlobalTaint(path, pTaint);
    } else {
      return addLocalTaint(path, pTaint);
    }
  }

  @Override
  public TaintState join(TaintState other) {
    throw new RuntimeException("Taint analysis is not supposed to perform join");
  }

  @Override
  public boolean isLessOrEqual(TaintState other) throws CPAException, InterruptedException {
    return false;
  }

  @Override
  public boolean getActiveStatus() {
    return true;
  }

  @Override
  public String toDOTLabel() {
    final StringBuilder builder = new StringBuilder();
    final List<String> collector = new ArrayList<>();
    TreeVisitor<String, Taint> visitor = new TreeVisitor<String, Taint>() {
      @Override
      public TreeVisitStrategy visit(
          Stack<String> path, Taint element, boolean isLeaf) {
        if (element != null) {
          collector.add(Joiner.on('.').join(path) + " <-- " +
              (element == Taint.TAINT ? "TAINT" : "CLEAN"));
        }
        return TreeVisitStrategy.CONTINUE;
      }
    };
    builder.append("\n<<< LOCAL >>>\n");
    localState.traverse(visitor);
    builder.append(Joiner.on('\n').join(collector));
    collector.clear();
    builder.append("\n<<< GLOBAL >>>\n");
    globalState.traverse(visitor);
    builder.append(Joiner.on('\n').join(collector));
    return builder.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
