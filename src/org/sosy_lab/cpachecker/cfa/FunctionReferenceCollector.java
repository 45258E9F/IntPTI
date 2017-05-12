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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.util.CFATraversal;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * This Visitor collects all function referenced as function pointer.
 *
 * For instance:
 *
 * fp = &f
 *
 * f is collected
 *
 * It should visit the CFA of each functions BEFORE creating super-edges (functioncall- and
 * return-edges).
 */
public class FunctionReferenceCollector extends CFATraversal.DefaultCFAVisitor
    implements CSimpleDeclarationVisitor<Void, Exception>,
               CInitializerVisitor<Void, Exception>,
               CStatementVisitor<Void, Exception> {

  private final Set<String> referredFuns = Sets.newHashSet();

  public Collection<String> getFunctionCalls() {
    return Collections.unmodifiableCollection(referredFuns);
  }

  @Override
  public CFATraversal.TraversalProcess visitEdge(final CFAEdge pEdge) {
    switch (pEdge.getEdgeType()) {
      case DeclarationEdge: {
        final CDeclarationEdge edge = (CDeclarationEdge) pEdge;
        try {
          edge.getDeclaration().accept(this);
        } catch (Exception e1) {
        }
        break;
      }

      case StatementEdge: {
        final CStatementEdge edge = (CStatementEdge) pEdge;
        try {
          edge.getStatement().accept(this);
        } catch (Exception e) {
        }
        break;
      }

      case FunctionCallEdge:
      case FunctionReturnEdge:
      case CallToReturnEdge:
        throw new AssertionError("functioncall- and return-edges should not exist at this time.");
      default:
        // nothing to do
    }
    return CFATraversal.TraversalProcess.CONTINUE;
  }

  private void check(CExpression pExpression) {
    CIdExpressionCollectorVisitor visitor = new CIdExpressionCollectorVisitor();
    pExpression.accept(visitor);
    for (CIdExpression id : visitor.getReferencedIdExpressions()) {
      if (id.getDeclaration() instanceof CFunctionDeclaration) {
        referredFuns.add(id.getName());
      }
    }
  }

  @Override
  public Void visit(CInitializerExpression pInitializerExpression) throws Exception {
    check(pInitializerExpression.getExpression());
    return null;
  }

  @Override
  public Void visit(CInitializerList pInitializerList) throws Exception {
    for (CInitializer init : pInitializerList.getInitializers()) {
      init.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CDesignatedInitializer pCStructInitializerPart) throws Exception {
    pCStructInitializerPart.getRightHandSide().accept(this);
    return null;
  }

  @Override
  public Void visit(CExpressionStatement pIastExpressionStatement) throws Exception {
    check(pIastExpressionStatement.getExpression());
    return null;
  }

  @Override
  public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws Exception {
    check(pIastExpressionAssignmentStatement.getRightHandSide());
    return null;
  }

  @Override
  public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws Exception {
    check(pIastFunctionCallAssignmentStatement.getFunctionCallExpression()
        .getFunctionNameExpression());
    return null;
  }

  @Override
  public Void visit(CFunctionCallStatement pIastFunctionCallStatement) throws Exception {
    check(pIastFunctionCallStatement.getFunctionCallExpression().getFunctionNameExpression());
    return null;
  }

  @Override
  public Void visit(CFunctionDeclaration pDecl) throws Exception {
    // ignore the declaration
    return null;
  }

  @Override
  public Void visit(CComplexTypeDeclaration pDecl) throws Exception {
    // ignore the declaration
    return null;
  }

  @Override
  public Void visit(CTypeDeclaration pDecl) throws Exception {
    // ignore the declaration
    return null;
  }

  @Override
  public Void visit(CVariableDeclaration pDecl) throws Exception {
    pDecl.getInitializer().accept(this);
    return null;
  }

  @Override
  public Void visit(CParameterDeclaration pDecl) throws Exception {
    return null;
  }

  @Override
  public Void visit(CEnumerator pDecl) throws Exception {
    return null;
  }
}
