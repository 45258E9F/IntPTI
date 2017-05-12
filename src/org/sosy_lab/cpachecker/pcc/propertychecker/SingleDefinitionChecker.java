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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils.VariableExtractor;

import java.util.Collection;
import java.util.Set;


/**
 * Checks if a certain variable is defined at most once by the program.
 */
public class SingleDefinitionChecker implements PropertyChecker {

  private final String varDefName;
  private ProgramDefinitionPoint point;

  public SingleDefinitionChecker(String varWithSingleDef) {
    varDefName = varWithSingleDef;
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    point = null;
    ReachingDefState rdState;

    for (AbstractState elem : pCertificate) {
      // check if variable is at most defined once
      rdState = AbstractStates.extractStateByType(elem, ReachingDefState.class);
      if (rdState == ReachingDefState.topElement
          || !(stillSingleDefinition(rdState.getGlobalReachingDefinitions().get(varDefName))
          && stillSingleDefinition(rdState
          .getLocalReachingDefinitions().get(varDefName)))) {
        return false;
      }
    }
    return true;
  }

  private boolean stillSingleDefinition(Set<DefinitionPoint> definitions) {
    if (definitions == null) {
      return true;
    }
    for (DefinitionPoint p : definitions) {
      if (p instanceof ProgramDefinitionPoint) {
        // check if there is another known definition
        if (point == null) {
          if (isDefinitionInProgram((ProgramDefinitionPoint) p)) {
            point = (ProgramDefinitionPoint) p;
          }
        } else if (!p.equals(point)) {
          // check if it is a real definition
          if (isDefinitionInProgram((ProgramDefinitionPoint) p)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean isDefinitionInProgram(ProgramDefinitionPoint pdp) {
    CFAEdge edge;
    CExpression left;
    if (pdp.getDefinitionEntryLocation().hasEdgeTo(pdp.getDefinitionExitLocation())) {
      edge = pdp.getDefinitionEntryLocation().getEdgeTo(pdp.getDefinitionExitLocation());
      if (edge instanceof CStatementEdge) {
        left = null;
        if (((CStatementEdge) edge).getStatement() instanceof CExpressionAssignmentStatement) {
          left = ((CExpressionAssignmentStatement) ((CStatementEdge) edge).getStatement())
              .getLeftHandSide();
        }
        if (((CStatementEdge) edge).getStatement() instanceof CFunctionCallAssignmentStatement) {
          left = ((CFunctionCallAssignmentStatement) ((CStatementEdge) edge).getStatement())
              .getLeftHandSide();
        }
        if (left != null) {
          VariableExtractor extractor = new VariableExtractor(edge);
          extractor.resetWarning();
          String var;
          try {
            var = left.accept(extractor);
          } catch (UnsupportedCCodeException e) {
            var = null;
          }
          if (var != null && var.equals(varDefName)) {
            return true;
          }
        }
      }
      if (edge instanceof CDeclarationEdge
          && ((CDeclarationEdge) edge).getDeclaration() instanceof CVariableDeclaration
          && ((CVariableDeclaration) ((CDeclarationEdge) edge).getDeclaration()).getInitializer()
          != null
          && ((CVariableDeclaration) ((CDeclarationEdge) edge).getDeclaration()).getName()
          .equals(varDefName)) {
        return true;
      }
    }
    return false;
  }

}
