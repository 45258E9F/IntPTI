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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;

import java.util.List;

public class AddressAndState extends ShapeObjectAndState<Address> {

  private AddressAndState(ShapeState pState, Address pAddress) {
    super(pState, pAddress);
  }

  public static List<AddressAndState> listOf(ShapeState pState, Address pAddress) {
    return ImmutableList.of(of(pState, pAddress));
  }

  public static List<AddressAndState> listOf(ShapeState pState) {
    return ImmutableList.of(of(pState));
  }

  public static AddressAndState of(ShapeState pState) {
    return new AddressAndState(pState, Address.UNKNOWN);
  }

  public static AddressAndState of(ShapeState pState, Address pAddress) {
    return new AddressAndState(pState, pAddress);
  }

}
