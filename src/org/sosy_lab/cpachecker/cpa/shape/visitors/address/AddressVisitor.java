/*
 * IntPTI: integer error fixing by proper-type inference
 * Copyright (c) 2017.
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

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.List;

/**
 * An abstract class for other address visitors.
 * Note: other address visitors are designed specifically for some expression types.
 */
public abstract class AddressVisitor
    extends DefaultCExpressionVisitor<List<AddressAndState>, CPATransferException>
    implements CRightHandSideVisitor<List<AddressAndState>, CPATransferException> {

  private final CFAEdge edge;
  private final ShapeState readableState;
  protected final List<AbstractState> otherStates;

  AddressVisitor(CFAEdge pEdge, ShapeState pState, List<AbstractState> pOtherStates) {
    edge = pEdge;
    readableState = pState;
    otherStates = pOtherStates;
  }

  public final CFAEdge getEdge() {
    return edge;
  }

  public final ShapeState getShapeState() {
    return readableState;
  }

  @Override
  protected List<AddressAndState> visitDefault(CExpression exp) throws CPATransferException {
    return AddressAndState.listOf(readableState);
  }

  @Override
  public List<AddressAndState> visit(CArraySubscriptExpression e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().evaluateArraySubscriptAddress(readableState,
        otherStates, edge, e);
  }

  @Override
  public List<AddressAndState> visit(CFieldReference e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().evaluateFieldAddress(readableState, otherStates, edge, e);
  }

  @Override
  public List<AddressAndState> visit(CIdExpression e) throws CPATransferException {
    // we do not use qualified name in shape graph
    SGObject object = readableState.getObjectForVisibleVariable(e.getName());
    return AddressAndState.listOf(readableState, Address.valueOf(object, KnownExplicitValue.ZERO));
  }

  @Override
  public List<AddressAndState> visit(CPointerExpression e) throws CPATransferException {
    // the address of *p is the value of p
    CExpression operand = e.getOperand();
    AddressValueAndStateList addressValueAndStates =
        CoreShapeAdapter.getInstance().evaluateAddressValue
            (readableState, otherStates, edge, operand);
    return addressValueAndStates.asAddressAndStateList();
  }
}
