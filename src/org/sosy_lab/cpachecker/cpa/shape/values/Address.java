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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

/**
 * An abstract address object contains memory object and the offset. Different from
 * {@link ShapeAddressValue}, this object does not contains symbolic value denoting the address
 * value (which is impossible to be derived statically in general).
 */
public class Address {

  private final SGObject object;

  private final ShapeExplicitValue offset;

  private Address() {
    object = null;
    offset = UnknownValue.getInstance();
  }

  protected Address(SGObject pObject, ShapeExplicitValue pOffset) {
    object = pObject;
    offset = Preconditions.checkNotNull(pOffset);
  }

  public final boolean isUnknown() {
    return object == null || offset.isUnknown();
  }

  public static final Address UNKNOWN = new Address();

  public static Address valueOf(SGObject pObject, int pOffset) {
    return new Address(pObject, KnownExplicitValue.valueOf(pOffset));
  }

  public static Address valueOf(SGObject pObject, ShapeExplicitValue pOffset) {
    return new Address(pObject, pOffset);
  }

  public ShapeExplicitValue getOffset() {
    return offset;
  }

  public SGObject getObject() {
    return object;
  }

  @Override
  public String toString() {
    if (isUnknown()) {
      return "Unknown";
    }
    return "Object: " + object.toString() + " Offset: " + offset.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Address)) {
      return false;
    }
    Address that = (Address) obj;
    return object.equals(that.object) && offset.equals(that.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(object, offset);
  }
}
