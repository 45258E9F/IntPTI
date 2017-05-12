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
package org.sosy_lab.cpachecker.cpa.reachdef;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils.VariableExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


public class ReachingDefTransferRelation implements TransferRelation {

  private Map<FunctionEntryNode, Set<String>> localVariablesPerFunction;

  private CFANode main;

  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdownNotifier;

  public ReachingDefTransferRelation(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
  }

  public void provideLocalVariablesOfFunctions(Map<FunctionEntryNode, Set<String>> localVars) {
    localVariablesPerFunction = localVars;
  }

  public void setMainFunctionNode(CFANode pMain) {
    main = pMain;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState,
      List<AbstractState> otherStates,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    List<CFANode> nodes = ReachingDefUtils.getAllNodesFromCFA();
    if (nodes == null) {
      throw new CPATransferException("CPA not properly initialized.");
    }
    List<AbstractState> successors = new ArrayList<>();
    List<CFAEdge> definitions = new ArrayList<>();
    for (CFANode node : nodes) {
      for (CFAEdge cfaedge : CFAUtils.leavingEdges(node)) {
        shutdownNotifier.shutdownIfNecessary();

        if (!(cfaedge.getEdgeType() == CFAEdgeType.FunctionReturnEdge)) {
          if (cfaedge.getEdgeType() == CFAEdgeType.StatementEdge
              || cfaedge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
            definitions.add(cfaedge);
          } else {
            successors.addAll(getAbstractSuccessors0(pState, cfaedge));
          }
        }
      }
    }
    for (CFAEdge edge : definitions) {
      successors.addAll(getAbstractSuccessors0(pState, edge));
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pCfaEdge);
    return getAbstractSuccessors0(pState, pCfaEdge);
  }

  private Collection<? extends AbstractState> getAbstractSuccessors0(
      AbstractState pState,
      CFAEdge pCfaEdge) throws CPATransferException {

    logger.log(Level.FINE, "Compute succesor for ", pState, "along edge", pCfaEdge);

    if (localVariablesPerFunction == null) {
      throw new CPATransferException(
          "Incorrect initialization of reaching definition transfer relation.");
    }

    if (!(pState instanceof ReachingDefState)) {
      throw new CPATransferException(
          "Unexpected type of abstract state. The transfer relation is not defined for this type");
    }

    if (pCfaEdge == null) {
      throw new CPATransferException(
          "Expected an edge along which the successors should be computed");
    }

    if (pState == ReachingDefState.topElement) {
      return Collections.singleton(pState);
    }

    ReachingDefState result;

    switch (pCfaEdge.getEdgeType()) {
      case StatementEdge: {
        result = handleStatementEdge((ReachingDefState) pState, (CStatementEdge) pCfaEdge);
        break;
      }
      case DeclarationEdge: {
        result = handleDeclarationEdge((ReachingDefState) pState, (CDeclarationEdge) pCfaEdge);
        break;
      }
      case FunctionCallEdge: {
        result = handleCallEdge((ReachingDefState) pState, (CFunctionCallEdge) pCfaEdge);
        break;
      }
      case FunctionReturnEdge: {
        result = handleReturnEdge((ReachingDefState) pState);
        break;
      }
      case MultiEdge: {
        result = handleMultiEdge((ReachingDefState) pState, (MultiEdge) pCfaEdge);
        break;
      }
      case BlankEdge:
        // TODO still correct?
        // special case entering the main method for the first time (no local variables known)
        logger.log(Level.FINE, "Start of main function. ",
            "Add undefined position for local variables of main function. ",
            "Add definition of parameters of main function.");
        if (pCfaEdge.getPredecessor() == main
            && ((ReachingDefState) pState).getLocalReachingDefinitions().size() == 0) {
          result = ((ReachingDefState) pState)
              .initVariables(localVariablesPerFunction.get(pCfaEdge.getPredecessor()),
                  ImmutableSet.copyOf(
                      ((FunctionEntryNode) pCfaEdge.getPredecessor()).getFunctionParameterNames()),
                  pCfaEdge.getPredecessor(), pCfaEdge.getSuccessor());
          break;
        }
        //$FALL-THROUGH$
      case AssumeEdge:
      case CallToReturnEdge:
      case ReturnStatementEdge:
        logger.log(Level.FINE, "Reaching definition not affected by edge. ",
            "Keep reaching definition unchanged.");
        result = (ReachingDefState) pState;
        break;
      default:
        throw new CPATransferException("Unknown CFA edge type.");
    }

    return Collections.singleton(result);
  }

  /*
   * Note that currently it is not dealt with aliasing.
   * Thus, if two variables s1 and s2 of non basic type point to same element and
   * variable s1 is used to update the element,
   * only the reaching definition of s1 will be updated.
   */
  private ReachingDefState handleStatementEdge(ReachingDefState pState, CStatementEdge edge)
      throws CPATransferException {
    CStatement statement = edge.getStatement();
    CExpression left;
    if (statement instanceof CExpressionAssignmentStatement) {
      left = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
    } else if (statement instanceof CFunctionCallAssignmentStatement) {
      // handle function call on right hand side to external method
      left = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();
      logger.logOnce(Level.WARNING,
          "Analysis may be unsound if external method redefines global variables",
          "or considers extra global variables.");
    } else {
      return pState;
    }

    // if some array element is changed the whole array is considered to be changed
    /* if a field is changed the whole variable the field is associated with is considered to be changed,
     * e.g. a.p.c = 110, then a should be considered
     */
    VariableExtractor varExtractor = new VariableExtractor(edge);
    varExtractor.resetWarning();
    String var = left.accept(varExtractor);
    if (varExtractor.getWarning() != null) {
      logger.logOnce(Level.WARNING, varExtractor.getWarning());
    }

    if (var == null) {
      return pState;
    }

    logger.log(Level.FINE, "Edge provided a new definition of variable ", var,
        ". Update reaching definition.");
    if (pState.getGlobalReachingDefinitions().containsKey(var)) {
      return pState.addGlobalReachDef(var, edge.getPredecessor(), edge.getSuccessor());
    } else {
      assert (pState.getLocalReachingDefinitions().containsKey(var));
      return pState.addLocalReachDef(var, edge.getPredecessor(), edge.getSuccessor());
    }
  }

  private ReachingDefState handleDeclarationEdge(ReachingDefState pState, CDeclarationEdge edge) {
    if (edge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration dec = (CVariableDeclaration) edge.getDeclaration();
      // check if initial value is known (declaration + definition)
      if (dec.getInitializer() != null) {
        if (dec.isGlobal()) {
          return pState
              .addGlobalReachDef(dec.getName(), edge.getPredecessor(), edge.getSuccessor());
        } else {
          return pState.addLocalReachDef(dec.getName(), edge.getPredecessor(), edge.getSuccessor());
        }
      }
    }
    return pState;
  }

  private ReachingDefState handleCallEdge(ReachingDefState pState, CFunctionCallEdge pCfaEdge) {
    logger.log(Level.FINE, "New internal function called. ",
        "Add undefined position for local variables. ",
        "Add definition of parameters.");
    return pState.initVariables(localVariablesPerFunction.get(pCfaEdge.getSuccessor()),
        ImmutableSet
            .copyOf(((FunctionEntryNode) pCfaEdge.getSuccessor()).getFunctionParameterNames()),
        pCfaEdge.getPredecessor(), pCfaEdge.getSuccessor());
  }

  private ReachingDefState handleReturnEdge(ReachingDefState pState) {
    logger.log(Level.FINE, "Return from internal function call. ",
        "Remove local variables and parameters of function from reaching definition.");
    return pState.pop();
  }

  private ReachingDefState handleMultiEdge(ReachingDefState pState, MultiEdge edge)
      throws CPATransferException {
    for (CFAEdge simpleEdge : edge.getEdges()) {

      switch (simpleEdge.getEdgeType()) {
        case StatementEdge: {
          pState = handleStatementEdge(pState, (CStatementEdge) simpleEdge);
          break;
        }
        case DeclarationEdge: {
          pState = handleDeclarationEdge(pState, (CDeclarationEdge) simpleEdge);
          break;
        }
        case BlankEdge:
          break;
        case ReturnStatementEdge:
          break;
        default:
          throw new CPATransferException("Unknown CFA edge type incorporated in MultiEdge.");
      }
    }
    return pState;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    // TODO consider information from alias analysis
    return null;
  }

}
