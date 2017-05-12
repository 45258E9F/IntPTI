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

import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.List;

/**
 * This visitor evaluates structure or union values as addresses.
 */
public class StructAndUnionVisitor extends AddressVisitor
    implements CRightHandSideVisitor<List<AddressAndState>, CPATransferException> {

  public StructAndUnionVisitor(CFAEdge pEdge, ShapeState pState, List<AbstractState> pOtherStates) {
    super(pEdge, pState, pOtherStates);
  }

  @Override
  public List<AddressAndState> visit(CCastExpression e) throws CPATransferException {
    CExpression operand = e.getOperand();
    CType type = CoreShapeAdapter.getType(operand);
    if (CoreShapeAdapter.isStructOrUnion(type)) {
      return operand.accept(this);
    } else {
      return AddressAndState.listOf(getShapeState());
    }
  }

  @Override
  public List<AddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    // if a library function returns structure or union, we can model its semantics by using
    // function adapter.
    return AddressAndState.listOf(getShapeState());
  }
}
