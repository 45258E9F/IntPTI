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
package org.sosy_lab.cpachecker.cpa.shape.visitors.address;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.List;

/**
 * This visitor evaluates the address of a left-hand-side expression.
 * Note on the difference between address and address value:
 * An integer variable could have an address without address value. An address represents the
 * memory location of this variable in shape graph, while address value is a special kind of
 * symbolic value which has address semantics. Obviously, integer variable only has arithmetic
 * semantics and therefore we cannot interpret this value as an address of memory location.
 *
 * For a pointer value which could have both address and address value, the address is the memory
 * location of this value and address value denotes the address of memory location that this
 * pointer value points to.
 *
 * For an array value, the address and address value are associated with the same memory object.
 */
public class LValueVisitor extends AddressVisitor {

  public LValueVisitor(CFAEdge pEdge, ShapeState pState, List<AbstractState> pOtherStates) {
    super(pEdge, pState, pOtherStates);
  }

  @Override
  public List<AddressAndState> visit(CUnaryExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CBinaryExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CCastExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CComplexCastExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CCharLiteralExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CImaginaryLiteralExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CFloatLiteralExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CIntegerLiteralExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CStringLiteralExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CTypeIdExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CAddressOfLabelExpression e) throws CPATransferException {
    throw new CPATransferException(e.toASTString() + " is not a l-value");
  }

  @Override
  public List<AddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    throw new CPATransferException(pIastFunctionCallExpression.toASTString() + " is not a "
        + "l-value");
  }
}
