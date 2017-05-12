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

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultipleAccessPathVisitorForTaint
    extends DefaultCExpressionVisitor<Set<AccessPath>, UnrecognizedCCodeException> {

  private List<AbstractState> otherStates = null;

  public MultipleAccessPathVisitorForTaint(List<AbstractState> pOtherStates) {
    otherStates = pOtherStates;
  }

  @Override
  public Set<AccessPath> visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    Set<AccessPath> paths = new HashSet<>();
    AccessPath path = e.accept(new AccessPathVisitorForTaint(otherStates));
    if (path != null) {
      paths.add(path);
    }
    return paths;
  }

  @Override
  public Set<AccessPath> visit(CBinaryExpression e) throws UnrecognizedCCodeException {
    CExpression op1 = e.getOperand1();
    CExpression op2 = e.getOperand2();
    Set<AccessPath> paths = new HashSet<>();
    paths.addAll(op1.accept(this));
    paths.addAll(op2.accept(this));
    return paths;
  }

  @Override
  public Set<AccessPath> visit(CCastExpression e) throws UnrecognizedCCodeException {
    Set<AccessPath> paths = new HashSet<>();
    paths.addAll(e.getOperand().accept(this));
    return paths;
  }

  @Override
  public Set<AccessPath> visit(CFieldReference e) throws UnrecognizedCCodeException {
    Set<AccessPath> paths = new HashSet<>();
    AccessPath path = e.accept(new AccessPathVisitorForTaint(otherStates));
    if (path != null) {
      paths.add(path);
    }
    return paths;
  }

  @Override
  public Set<AccessPath> visit(CIdExpression e) throws UnrecognizedCCodeException {
    Set<AccessPath> paths = new HashSet<>();
    paths.add(new AccessPath(e.getDeclaration()));
    return paths;
  }

  @Override
  public Set<AccessPath> visit(CUnaryExpression e) throws UnrecognizedCCodeException {
    Set<AccessPath> paths = new HashSet<>();
    paths.addAll(e.getOperand().accept(this));
    return paths;
  }

  @Override
  public Set<AccessPath> visit(CPointerExpression e) throws UnrecognizedCCodeException {
    Set<AccessPath> paths = new HashSet<>();
    AccessPath path = e.accept(new AccessPathVisitorForTaint(otherStates));
    if (path != null) {
      paths.add(path);
    }
    return paths;
  }

  @Override
  protected Set<AccessPath> visitDefault(CExpression exp) throws UnrecognizedCCodeException {
    return Sets.newHashSet();
  }
}
