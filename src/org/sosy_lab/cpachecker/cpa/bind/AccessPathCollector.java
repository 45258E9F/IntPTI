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

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.MergeAdvisor;

import java.util.List;

import javax.annotation.Nonnull;

public class AccessPathCollector
    extends
    DefaultCExpressionVisitor<PathCopyingPersistentTree<String, Integer>, UnrecognizedCCodeException>
    implements
    CRightHandSideVisitor<PathCopyingPersistentTree<String, Integer>, UnrecognizedCCodeException> {

  private final List<AbstractState> otherStates;

  public AccessPathCollector(List<AbstractState> pOtherStates) {
    otherStates = pOtherStates;
  }

  public static MergeAdvisor<String, Integer> defaultMerger = new MergeAdvisor<String, Integer>() {
    @Override
    public Pair<Integer, Boolean> merge(
        @Nonnull Integer a, @Nonnull Integer b) {
      return Pair.of(a, true);
    }
  };

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CArraySubscriptExpression e)
      throws UnrecognizedCCodeException {
    CExpression array = e.getArrayExpression();
    CExpression subscript = e.getSubscriptExpression();
    PathCopyingPersistentTree<String, Integer> arrayTree = array.accept(this);
    PathCopyingPersistentTree<String, Integer> indexTree = subscript.accept(this);
    arrayTree = PathCopyingPersistentTree.merge(arrayTree, indexTree, defaultMerger);
    AccessPath arrayPath = e.accept(new AccessPathExtractorForLHS(otherStates));
    if (arrayPath != null) {
      arrayTree = arrayTree.setElementAndCopy(AccessPath.toStrList(arrayPath), 0);
    }
    return arrayTree;
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CBinaryExpression e)
      throws UnrecognizedCCodeException {
    CExpression operand1 = e.getOperand1();
    CExpression operand2 = e.getOperand2();
    PathCopyingPersistentTree<String, Integer> tree1 = operand1.accept(this);
    PathCopyingPersistentTree<String, Integer> tree2 = operand2.accept(this);
    tree1 = PathCopyingPersistentTree.merge(tree1, tree2, defaultMerger);
    return tree1;
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CCastExpression e)
      throws UnrecognizedCCodeException {
    CExpression operand = e.getOperand();
    return operand.accept(this);
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CFieldReference e)
      throws UnrecognizedCCodeException {
    CExpression owner = e.getFieldOwner();
    PathCopyingPersistentTree<String, Integer> ownerTree = owner.accept(this);
    AccessPath fieldPath = e.accept(new AccessPathExtractorForLHS(otherStates));
    if (fieldPath != null) {
      ownerTree = ownerTree.setElementAndCopy(AccessPath.toStrList(fieldPath), 0);
    }
    return ownerTree;
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CIdExpression e)
      throws UnrecognizedCCodeException {
    CSimpleDeclaration declaration = e.getDeclaration();
    if (declaration instanceof CVariableDeclaration ||
        declaration instanceof CParameterDeclaration) {
      PathCopyingPersistentTree<String, Integer> tree = new PathCopyingPersistentTree<>();
      AccessPath idPath = new AccessPath(declaration);
      tree = tree.setElementAndCopy(AccessPath.toStrList(idPath), 0);
      return tree;
    }
    // otherwise, we discard this declaration
    return PathCopyingPersistentTree.of();
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CUnaryExpression e)
      throws UnrecognizedCCodeException {
    CExpression operand = e.getOperand();
    return operand.accept(this);
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CPointerExpression e)
      throws UnrecognizedCCodeException {
    CExpression operand = e.getOperand();
    PathCopyingPersistentTree<String, Integer> tree = operand.accept(this);
    AccessPath ptrPath = e.accept(new AccessPathExtractorForLHS(otherStates));
    if (ptrPath != null) {
      tree = tree.setElementAndCopy(AccessPath.toStrList(ptrPath), 0);
    }
    return tree;
  }

  @Override
  public PathCopyingPersistentTree<String, Integer> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    CExpression function = pIastFunctionCallExpression.getFunctionNameExpression();
    List<CExpression> args = pIastFunctionCallExpression.getParameterExpressions();
    PathCopyingPersistentTree<String, Integer> total = new PathCopyingPersistentTree<>();
    total = PathCopyingPersistentTree.merge(total, function.accept(this), defaultMerger);
    for (CExpression arg : args) {
      total = PathCopyingPersistentTree.merge(total, arg.accept(this), defaultMerger);
    }
    return total;
  }

  @Override
  protected PathCopyingPersistentTree<String, Integer> visitDefault(CExpression exp)
      throws UnrecognizedCCodeException {
    return PathCopyingPersistentTree.of();
  }
}
