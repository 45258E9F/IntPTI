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
package org.sosy_lab.cpachecker.cpa.defuse;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DefUseTransferRelation extends SingleEdgeTransferRelation {
  private DefUseState handleExpression(
      DefUseState defUseState,
      CStatement expression,
      CFAEdge cfaEdge) {
    if (expression instanceof CAssignment) {
      CAssignment assignExpression = (CAssignment) expression;

      String lParam = assignExpression.getLeftHandSide().toASTString();
      // String lParam2 = binaryExpression.getOperand2 ().getRawSignature ();

      DefUseDefinition definition = new DefUseDefinition(lParam, cfaEdge);
      defUseState = new DefUseState(defUseState, definition);
    }
    return defUseState;
  }

  private DefUseState handleDeclaration(DefUseState defUseState, CDeclarationEdge cfaEdge) {
    if (cfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration) cfaEdge.getDeclaration();
      CInitializer initializer = decl.getInitializer();
      if (initializer != null) {
        String varName = decl.getName();
        DefUseDefinition definition = new DefUseDefinition(varName, cfaEdge);

        defUseState = new DefUseState(defUseState, definition);
      }
    }
    return defUseState;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState element, List<AbstractState> otherStates, Precision prec, CFAEdge cfaEdge)
      throws CPATransferException {
    DefUseState defUseState = (DefUseState) element;

    switch (cfaEdge.getEdgeType()) {
      case StatementEdge: {
        CStatementEdge statementEdge = (CStatementEdge) cfaEdge;
        CStatement expression = statementEdge.getStatement();
        defUseState = handleExpression(defUseState, expression, cfaEdge);
        break;
      }
      case DeclarationEdge: {
        CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
        defUseState = handleDeclaration(defUseState, declarationEdge);
        break;
      }
      default:
        // not relevant for def-use
        break;
    }

    return Collections.singleton(defUseState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
  }
}
