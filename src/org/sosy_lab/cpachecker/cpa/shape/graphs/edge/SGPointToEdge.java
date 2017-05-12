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
package org.sosy_lab.cpachecker.cpa.shape.graphs.edge;

import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;

public class SGPointToEdge extends SGEdge {

  private final int offset;

  public SGPointToEdge(long pValue, SGObject pObject, int pOffset) {
    super(pValue, pObject);
    offset = pOffset;
  }

  @Override
  public String toString() {
    return value + "->" + object.getLabel() + "+" + offset + "b";
  }

  public int getOffset() {
    return offset;
  }

  public Address getAddress() {
    return Address.valueOf(object, offset);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + offset;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SGPointToEdge)) {
      return false;
    }
    SGPointToEdge other = (SGPointToEdge) obj;
    return super.equals(obj) && offset == other.offset;
  }
}
