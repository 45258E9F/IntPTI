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
package org.sosy_lab.cpachecker.cpa.validvars;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class ValidVarsTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ValidVarsState state = (ValidVarsState) pState;
    ValidVars validVariables = state.getValidVariables();

    switch (pCfaEdge.getEdgeType()) {
      case MultiEdge:
        Collection<AbstractState> predecessors, successors;
        predecessors = Collections.singleton(pState);

        for (CFAEdge edge : ((MultiEdge) pCfaEdge).getEdges()) {
          successors = new ArrayList<>();
          for (AbstractState predState : predecessors) {
            successors
                .addAll(getAbstractSuccessorsForEdge(predState, otherStates, pPrecision, edge));
          }

          predecessors = successors;
        }

        return predecessors;
      case BlankEdge:
        if (pCfaEdge.getDescription().equals("Function start dummy edge") && !(pCfaEdge
            .getPredecessor() instanceof FunctionEntryNode)) {
          validVariables =
              validVariables.extendLocalVarsFunctionCall(pCfaEdge.getSuccessor().getFunctionName(),
                  ImmutableSet.<String>of());
        }
        if (pCfaEdge.getSuccessor() instanceof FunctionExitNode) {
          validVariables =
              validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
        }
        break;
      case FunctionCallEdge:
        validVariables =
            validVariables.extendLocalVarsFunctionCall(pCfaEdge.getSuccessor().getFunctionName(),
                ((FunctionEntryNode) pCfaEdge.getSuccessor()).getFunctionParameterNames());
        break;
      case DeclarationEdge:
        CDeclaration declaration = ((CDeclarationEdge) pCfaEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          if (declaration.isGlobal()) {
            validVariables = validVariables.extendGlobalVars(declaration.getName());
          } else {
            validVariables =
                validVariables.extendLocalVars(pCfaEdge.getPredecessor().getFunctionName(),
                    declaration.getName());
          }
        }
        break;
      case ReturnStatementEdge:
        validVariables =
            validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
        break;
      default:
        break;
    }

    if (state.getValidVariables() == validVariables) {
      return Collections.singleton(state);
    }
    return Collections.singleton(new ValidVarsState(validVariables));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
