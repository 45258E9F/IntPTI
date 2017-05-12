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

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

public abstract class SGEdge {

  protected final long value;
  protected final SGObject object;

  SGEdge(long pValue, SGObject pObject) {
    value = pValue;
    object = pObject;
  }

  public long getValue() {
    return value;
  }

  public SGObject getObject() {
    return object;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(object, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof SGEdge)) {
      return false;
    }
    SGEdge other = (SGEdge) obj;
    return value == other.value && Objects.equal(object, other.object);
  }
}
