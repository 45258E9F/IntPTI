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
package org.sosy_lab.cpachecker.cpa.shape.visitors.value;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * A type visitor for deriving the size of a given type. This visitor is designed for general
 * purpose excluding declaration processing.
 */
public class CSizeofVisitor extends BaseSizeofVisitor {

  private final CFAEdge cfaEdge;
  private final ShapeState readableState;
  private final List<AbstractState> otherStates;

  /**
   * This expression works only when it is a left-hand-side value.
   * A constraint: the type of this expression should be the type to be visited at the outermost
   * level.
   */
  private final CExpression expression;

  private boolean containsVLA = false;


  public CSizeofVisitor(
      MachineModel pMachineModel, CFAEdge pEdge, ShapeState pState,
      List<AbstractState> pOtherStates) {
    super(pMachineModel);
    cfaEdge = pEdge;
    readableState = pState;
    otherStates = pOtherStates;
    expression = null;
  }

  public CSizeofVisitor(
      MachineModel pMachineModel, CFAEdge pEdge, ShapeState pState,
      List<AbstractState> pOtherStates, CExpression pE) {
    super(pMachineModel);
    cfaEdge = pEdge;
    readableState = pState;
    otherStates = pOtherStates;
    expression = pE;
  }

  public boolean containsVLA() {
    return containsVLA;
  }

  @Override
  public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {
    int length;
    CExpression arrayLength = pArrayType.getLength();
    if (arrayLength instanceof CIntegerLiteralExpression) {
      BigInteger origLength = ((CIntegerLiteralExpression) arrayLength).getValue();
      length = origLength.intValue();
    } else {
      containsVLA = true;
      if (expression instanceof CLeftHandSide) {
        List<AddressAndState> addresses;
        try {
          addresses = CoreShapeAdapter.getInstance().evaluateAddress(readableState, otherStates,
              cfaEdge, (CLeftHandSide) expression);
        } catch (CPATransferException ex) {
          addresses = new ArrayList<>();
        }
        if (addresses.size() > 0) {
          Address address = addresses.get(0).getObject();
          if (!address.isUnknown()) {
            SGObject arrayObject = address.getObject();
            ShapeExplicitValue arraySize = arrayObject.getSize();
            int arrayOffset = address.getOffset().getAsInt();
            if (!arraySize.isUnknown()) {
              return arraySize.getAsInt() - arrayOffset;
            }
            // array size is unknown
          }
          // address is unknown (we cannot locate the memory object of this array)
        }
        // no address is derived
      }
      // the expression is not a left-hand-side (i.e. we cannot locate its memory object)
      return super.visit(pArrayType);
    }
    // We have an explicit array length, then we derive the size of array element.
    int elementSize = pArrayType.getType().accept(this);
    return elementSize * length;
  }

}
