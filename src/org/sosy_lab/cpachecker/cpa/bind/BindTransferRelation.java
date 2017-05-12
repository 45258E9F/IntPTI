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
package org.sosy_lab.cpachecker.cpa.bind;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Binding Analysis Transfer Relation
 */
@Options(prefix = "cpa.bind")
public class BindTransferRelation
    extends ForwardingTransferRelation<BindState, BindState, Precision> {

  private CFANode main;
  private Map<FunctionEntryNode, Set<AccessPath>> localVariablesPerFunction;

  private final LogManagerWithoutDuplicates logger;

  public BindTransferRelation(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  public void provideLocalVariablesOfFunctions(Map<FunctionEntryNode, Set<AccessPath>> localVars) {
    localVariablesPerFunction = localVars;
  }

  public void setMainFunctionNode(CFANode pMain) {
    main = pMain;
  }

  /**
   * Everything is done in strengthen phase
   * (In order to prevent duplicated computation)
   */

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  protected BindState handleMultiEdge(MultiEdge cfaEdge) throws CPATransferException {
    for (final CFAEdge innerEdge : cfaEdge) {
      edge = innerEdge;
      final BindState intermediate = handleSimpleEdge(innerEdge);
      Preconditions.checkState(state.getClass().isAssignableFrom(intermediate.getClass()),
          "We assume equal types for input- and output-values. " +
              "Thus this implementation only works with exactly one input- and one output-state (and they should have same type)."
              +
              "If there are more successors during a MultiEdge, you need to override this method.");
      state = intermediate;
    }
    edge = cfaEdge;
    return state;
  }

  @Override
  protected BindState handleAssumption(
      CAssumeEdge cfaEdge,
      CExpression expression,
      boolean truthAssumption)
      throws CPATransferException {
    // assume edge does not change the binding relation
    return state;
  }

  @Override
  protected BindState handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    // 1. push call stack
    // 2. reserve binding entries for global variables
    // 3. reset binding entries for local variables
    return state.pushCallStack(
        parameters,
        localVariablesPerFunction.get(cfaEdge.getSuccessor()),
        cfaEdge,
        otherStates
    );
  }

  @Override
  protected BindState handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    if (summaryExpr instanceof CAssignment) {
      return handleAssignment((CAssignment) summaryExpr, state.popCallStack(), fnkCall);
    } else {
      return state.popCallStack();
    }
  }

  @Override
  protected BindState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl) {
    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      AccessPath ap = new AccessPath(vdecl);
      // check if initial value is known (declaration + definition)
      if (vdecl.getInitializer() != null) {
        if (vdecl.isGlobal()) {
          return state.addGlobalBinding(ap, edge, otherStates);
        } else {
          return state.addLocalBinding(ap, edge, otherStates);
        }
      }
    }
    return state;
  }

  @Override
  protected BindState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    // NOTE: Analysis may be unsound if
    // (1) address of some variable is passed to a function
    // (2) the address is modified in the function call
    // A possible workaround is to utilize 'access analysis' to
    // obtain a over approximated binding analysis result
    BindState newState;
    if (statement instanceof CAssignment) {
      newState = handleAssignment((CAssignment) statement, state, cfaEdge);
    } else {
      newState = state;
    }
    // if the statement contains function call expression, we should analyze its parameter carefully
    if (statement instanceof CFunctionCall) {
      CFunctionCallExpression callExpression = ((CFunctionCall) statement)
          .getFunctionCallExpression();
      List<CExpression> args = callExpression.getParameterExpressions();
      for (CExpression arg : args) {
        CExpression argExp = arg;
        if (argExp instanceof CStringLiteralExpression) {
          continue;
        }
        CPointerType pointerType;
        while ((pointerType = Types.extractPointerType(argExp.getExpressionType())) != null) {
          argExp = new CPointerExpression(FileLocation.DUMMY, pointerType.getType(), argExp);
        }
        if (argExp instanceof CLeftHandSide) {
          AccessPath path =
              ((CLeftHandSide) argExp).accept(new AccessPathExtractorForLHS(otherStates));
          if (path != null) {
            newState = newState.removeBinding(path);
          }
        }
      }
    }
    return newState;
  }

  private BindState handleAssignment(
      CAssignment statement,
      BindState pState, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    BindState newState = BindState.copyOf(pState);
    AccessPathExtractorForLHS ape = new AccessPathExtractorForLHS(otherStates);
    AccessPath ap = ape.visit(statement.getLeftHandSide());
    if (ap != null) {
      newState = newState.updateBinding(ap, cfaEdge, otherStates);
      // FIX: delete bindings whose right-hand-sides contain access `ap`
      newState = newState.removeBindingWithSuchWrite(ap);
    }
    return newState;
  }

  @Override
  protected BindState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getPredecessor() == main && state.getLocalBindings().isEmpty()) {
      logger.log(Level.FINE, "Start of main function. ",
          "Add undefined bindings for local variables of main function. ",
          "Add definition of parameters of main function.");
      // build parameter list
      List<CParameterDeclaration> parameters = new ArrayList<>();
      for (AParameterDeclaration pd : ((FunctionEntryNode) cfaEdge.getPredecessor())
          .getFunctionParameters()) {
        parameters.add((CParameterDeclaration) pd);
      }
      return state.pushCallStack(
          parameters,
          localVariablesPerFunction.get(cfaEdge.getPredecessor()),
          cfaEdge,
          otherStates);
    } else {
      return state;
    }
  }

  @Override
  protected BindState handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {
    // return variable possibly exists, but there is no need to record binding info. of this
    // variable
    return state;
  }

  @Override
  protected BindState handleFunctionSummaryEdge(CFunctionSummaryEdge summaryEdge)
      throws CPATransferException {
    return state;
  }

}