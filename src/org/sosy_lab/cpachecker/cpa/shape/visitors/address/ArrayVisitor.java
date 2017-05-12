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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor evaluates expressions of array type as addresses.
 */
public class ArrayVisitor extends AddressVisitor
    implements CRightHandSideVisitor<List<AddressAndState>, CPATransferException> {

  public ArrayVisitor(CFAEdge pEdge, ShapeState pState, List<AbstractState> pOtherStates) {
    super(pEdge, pState, pOtherStates);
  }

  @Override
  public List<AddressAndState> visit(CUnaryExpression e) throws CPATransferException {
    // unary expression should not have array type
    return AddressAndState.listOf(getShapeState());
  }

  @Override
  public List<AddressAndState> visit(CBinaryExpression e) throws CPATransferException {
    // binary expression should not have array type either, e.g.
    // arr + 2 where arr is of T[] should have the type T*
    return AddressAndState.listOf(getShapeState());
  }

  @Override
  public List<AddressAndState> visit(CCastExpression e) throws CPATransferException {
    CExpression op = e.getOperand();
    if (op.getExpressionType() instanceof CArrayType) {
      return op.accept(this);
    } else {
      return AddressAndState.listOf(getShapeState());
    }
  }

  @Override
  public List<AddressAndState> visit(CIdExpression e) throws CPATransferException {
    // For the function foo(T[] arg) and the call foo(pArg), we create a memory object of type T*
    // for arg pointing to the first element of pArg. For arg[2], we should evaluate arg as a
    // symbolic value S (instead of as address) and then read the value of memory region pointed
    // by S.
    List<AddressAndState> addressAndStates = super.visit(e);
    if (e.getDeclaration() instanceof CParameterDeclaration) {
      CType type = CoreShapeAdapter.getType(e);
      assert (type instanceof CArrayType) : "array visitor is used only when the visited "
          + "expression has array type!";
      type = new CPointerType(type.isConst(), type.isVolatile(), ((CArrayType) type).getType());
      List<AddressAndState> newAddressAndStates = new ArrayList<>(addressAndStates.size());
      for (AddressAndState addressAndState : addressAndStates) {
        Address address = addressAndState.getObject();
        ShapeState newState = addressAndState.getShapeState();
        SymbolicValueAndStateList pointerValues = CoreShapeAdapter.getInstance().readValue(newState,
            otherStates, getEdge(), address.getObject(), address.getOffset(), type, e);
        for (SymbolicValueAndState pointerValue : pointerValues.asSymbolicValueAndStateList()) {
          AddressValueAndState actualAddresses = CoreShapeAdapter.getInstance()
              .getAddressFromSymbolicValue(pointerValue);
          newAddressAndStates.add(actualAddresses.asAddressAndState());
        }
      }
      return newAddressAndStates;
    }
    return addressAndStates;
  }

  @Override
  public List<AddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    return AddressAndState.listOf(getShapeState());
  }
}
