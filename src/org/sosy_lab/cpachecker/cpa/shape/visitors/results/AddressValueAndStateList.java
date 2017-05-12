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
package org.sosy_lab.cpachecker.cpa.shape.visitors.results;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;

import java.util.List;

public class AddressValueAndStateList extends SymbolicValueAndStateList {

  private AddressValueAndStateList(List<AddressValueAndState> pList) {
    super(ImmutableList.copyOf(pList));
  }

  private AddressValueAndStateList(AddressValueAndState pA) {
    super(pA);
  }

  public List<AddressAndState> asAddressAndStateList() {
    return FluentIterable.from(asSymbolicValueAndStateList()).transform(
        new Function<SymbolicValueAndState, AddressAndState>() {
          @Override
          public AddressAndState apply(SymbolicValueAndState pSymbolicValueAndState) {
            AddressValueAndState addressValueAndState = (AddressValueAndState)
                pSymbolicValueAndState;
            ShapeAddressValue addressValue = addressValueAndState.getObject();
            ShapeState newState = addressValueAndState.getShapeState();
            if (addressValue.isUnknown()) {
              return AddressAndState.of(newState);
            }
            return AddressAndState.of(newState, addressValue.getAddress());
          }
        }).toList();
  }

  public List<AddressValueAndState> asAddressValueAndStateList() {
    return FluentIterable.from(asSymbolicValueAndStateList()).transform(
        new Function<SymbolicValueAndState, AddressValueAndState>() {
          @Override
          public AddressValueAndState apply(SymbolicValueAndState pSymbolicValueAndState) {
            return (AddressValueAndState) pSymbolicValueAndState;
          }
        }).toList();
  }

  public static AddressValueAndStateList of(AddressValueAndState pA) {
    return new AddressValueAndStateList(pA);
  }

  public static AddressValueAndStateList of(ShapeState pState) {
    return of(AddressValueAndState.of(pState));
  }

  /**
   * Due to type erasure, we have to specify a new name for this method.
   */
  public static AddressValueAndStateList copyOfAddressList(List<AddressValueAndState> pL) {
    return new AddressValueAndStateList(pL);
  }
}
