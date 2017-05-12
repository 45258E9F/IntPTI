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
package org.sosy_lab.cpachecker.cpa.shape.visitors.results;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

public class AddressValueAndState extends SymbolicValueAndState {

  private AddressValueAndState(ShapeState pState, ShapeAddressValue pValue) {
    super(pState, pValue);
  }

  @Override
  public ShapeAddressValue getObject() {
    return (ShapeAddressValue) super.getObject();
  }

  public static AddressValueAndState of(ShapeState pState, ShapeAddressValue pValue) {
    return new AddressValueAndState(pState, pValue);
  }

  public static AddressValueAndState of(ShapeState pState) {
    return new AddressValueAndState(pState, UnknownValue.getInstance());
  }

  public AddressAndState asAddressAndState() {
    ShapeAddressValue addressValue = getObject();
    ShapeState shapeState = getShapeState();
    if (addressValue.isUnknown()) {
      return AddressAndState.of(shapeState);
    }
    return AddressAndState.of(shapeState, addressValue.getAddress());
  }

}
