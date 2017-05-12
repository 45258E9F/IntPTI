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
package org.sosy_lab.cpachecker.cpa.programcounter;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssignmentEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class ProgramCounterTransferRelation extends SingleEdgeTransferRelation {

  static final TransferRelation INSTANCE = new ProgramCounterTransferRelation();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ProgramCounterState state = (ProgramCounterState) pState;

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        if (pCfaEdge instanceof ADeclarationEdge) {
          ADeclarationEdge edge = (ADeclarationEdge) pCfaEdge;
          if (edge.getDeclaration() instanceof AVariableDeclaration) {
            AVariableDeclaration declaration = (AVariableDeclaration) edge.getDeclaration();
            if (declaration.getQualifiedName()
                .equals(CFASingleLoopTransformation.PROGRAM_COUNTER_VAR_NAME)) {
              if (declaration.getInitializer() instanceof AInitializerExpression) {
                AExpression expression =
                    ((AInitializerExpression) declaration.getInitializer()).getExpression();
                if (expression instanceof AIntegerLiteralExpression) {
                  BigInteger pcValue = ((AIntegerLiteralExpression) expression).getValue();
                  state = ProgramCounterState.getStateForValue(pcValue);
                }
              }
            }
          }
        }
        break;
      case AssumeEdge:
        if (pCfaEdge instanceof ProgramCounterValueAssumeEdge) {
          ProgramCounterValueAssumeEdge edge = (ProgramCounterValueAssumeEdge) pCfaEdge;
          BigInteger value = BigInteger.valueOf(edge.getProgramCounterValue());
          if (edge.getTruthAssumption()) {
            if (state.containsValue(value)) {
              state = ProgramCounterState.getStateForValue(value);
            } else {
              return Collections.emptySet();
            }
          } else {
            state = state.remove(value);
            if (state.isBottom()) {
              return Collections.emptySet();
            }
          }
        }
        break;
      case StatementEdge:
        if (pCfaEdge instanceof ProgramCounterValueAssignmentEdge) {
          ProgramCounterValueAssignmentEdge edge = (ProgramCounterValueAssignmentEdge) pCfaEdge;
          state = ProgramCounterState
              .getStateForValue(BigInteger.valueOf(edge.getProgramCounterValue()));
        }
        break;
      default:
        // Program counter variable does not occur in other edges.
        break;
    }
    if (state == null || state.isBottom()) {
      return Collections.emptySet();
    }
    return Collections.singleton(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
