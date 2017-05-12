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
package org.sosy_lab.cpachecker.cpa.pointer2.summary;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.visitor.PointerExpressionVisitor;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by landq on 11/28/16.
 */
public class MergeCalledFunctions implements MergeSummary {

  public static MergeCalledFunctions Instance = new MergeCalledFunctions();

  public MergeCalledFunctions() {
  }

  public Summary handleArgus(
      Summary merged, CFunctionCallExpression exp,
      PointerFunctionSummary calledFuncSummary)
      throws UnrecognizedCCodeException {
    Map<Integer, LocationSet> changedParasInCalledFunc =
        calledFuncSummary.getChangedFormalPointerParas();
    List<CExpression> actualArgus = exp.getParameterExpressions();
    Set<Integer> indexes = changedParasInCalledFunc.keySet();
    for (Integer i : indexes) {
      CExpression argu = actualArgus.get(i);
      if (argu instanceof CUnaryExpression &&
          ((CUnaryExpression) argu).getOperator().equals(UnaryOperator.AMPER) &&
          ((CUnaryExpression) argu).getOperand() instanceof CPointerExpression) {
        PointerExpressionVisitor pv = new PointerExpressionVisitor(merged.getFunctionName(),
            GlobalInfo.getInstance().getCFAInfo().get().getCFA().getMachineModel());
        MemoryLocation mem = ((CUnaryExpression) argu).getOperand().accept(pv);
        if (mem != null) {
          merged.addChanged(mem, LocationSetTop.INSTANCE);
        }
      }
    }
    return merged;
  }

  public Summary<PointerFunctionSummary> handleReturned(
      Summary merged, CLeftHandSide left,
      PointerFunctionSummary calledFuncSummary)
      throws UnrecognizedCCodeException {
    if (left != null && left instanceof CPointerExpression) {
      PointerExpressionVisitor pv = new PointerExpressionVisitor(merged.getFunctionName(),
          GlobalInfo.getInstance().getCFAInfo().get().getCFA().getMachineModel());
      MemoryLocation mem = left.accept(pv);
      LocationSet returned = calledFuncSummary.getReturnedSet();
      if (mem != null) {
        merged.addChanged(mem, returned);
      }
    }
    return merged;
  }

  public Summary mergeWithCalledFunc(
      Summary merged, CFAEdge edge, PointerFunctionSummary
      calledFuncSummary)
      throws UnrecognizedCCodeException {
    CFunctionCallExpression funcCallExp = null;
    CLeftHandSide left = null;
    if (edge instanceof CFunctionSummaryStatementEdge) {
      funcCallExp = ((CFunctionSummaryStatementEdge) edge).getFunctionCall()
          .getFunctionCallExpression();
    } else if (edge instanceof CStatementEdge) {
      funcCallExp = ((CFunctionCallAssignmentStatement) ((CStatementEdge) edge).getStatement())
          .getFunctionCallExpression();
      left = ((CAssignment) ((CStatementEdge) edge).getStatement()).getLeftHandSide();
    }
    if (merged instanceof PointerFunctionSummary) {
      merged = ((PointerFunctionSummary) merged)
          .mergeGlobal((PointerFunctionSummary) merged, calledFuncSummary);
    } else if (merged instanceof PointerLoopSummary) {
      merged = ((PointerLoopSummary) merged)
          .mergeChangedVars((PointerLoopSummary) merged, calledFuncSummary);
    }
    merged = handleArgus(merged, funcCallExp, calledFuncSummary);
    merged = handleReturned(merged, left, calledFuncSummary);
    return merged;
  }

  public CFAEdge getEdge(Summary caller, PointerFunctionSummary called) {
    String funcname = called.getFunctionName();
    for (Object object : caller.getCalledFunctions()) {
      CFAEdge edge = (CFAEdge) object;
      CFANode preNode = edge.getPredecessor();
      int outEdgeNum = preNode.getNumLeavingEdges();
      for (int i = 0; i < outEdgeNum; i++) {
        CFAEdge outEdge = preNode.getLeavingEdge(i);
        if (outEdge instanceof FunctionCallEdge) {
          if (outEdge.getSuccessor().getFunctionName().equals(funcname)) {
            return edge;
          }
        }
      }

    }
    return null;
  }

  @Override
  public Summary MergeSummary(
      PointerState state, Summary summary1, Summary summary2) {
    try {
      CFAEdge edge = getEdge(summary1, (PointerFunctionSummary) summary2);
      if (edge != null) {
        return mergeWithCalledFunc(summary1, edge, (PointerFunctionSummary) summary2);
      }
    } catch (UnrecognizedCCodeException pE) {
      pE.printStackTrace();
    }
    return null;
  }
}
