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
package org.sosy_lab.cpachecker.cpa.shape.values;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cpa.shape.graphs.ShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

import java.math.BigInteger;

public class KnownAddressValue extends KnownSymbolicValue implements ShapeAddressValue {

  private final Address address;

  public static final KnownAddressValue NULL = valueOf(ShapeGraph.getNullObject(), 0, 0);

  private KnownAddressValue(BigInteger pValue, Address pAddress) {
    super(pValue);
    address = Preconditions.checkNotNull(pAddress);
  }

  public static KnownAddressValue valueOf(
      SGObject pObject, KnownExplicitValue pOffset,
      KnownSymbolicValue pAddress) {
    return new KnownAddressValue(pAddress.getAsBigInteger(), Address.valueOf(pObject, pOffset));
  }

  public static KnownAddressValue valueOf(Address pAddress, BigInteger pValue) {
    return new KnownAddressValue(pValue, pAddress);
  }

  public static KnownAddressValue valueOf(Address pAddress, KnownSymbolicValue pValue) {
    return new KnownAddressValue(pValue.getAsBigInteger(), pAddress);
  }

  public static KnownAddressValue valueOf(Address pAddress, long pValue) {
    return new KnownAddressValue(BigInteger.valueOf(pValue), pAddress);
  }

  public static KnownAddressValue valueOf(SGObject pObject, int pOffset, long pValue) {
    return new KnownAddressValue(BigInteger.valueOf(pValue), Address.valueOf(pObject, pOffset));
  }

  @Override
  public Address getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "value: " + super.toString() + " " + address.toString();
  }

  @Override
  public ShapeExplicitValue getOffset() {
    return address.getOffset();
  }

  @Override
  public SGObject getObject() {
    return address.getObject();
  }

}
