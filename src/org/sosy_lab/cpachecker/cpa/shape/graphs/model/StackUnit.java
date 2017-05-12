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
package org.sosy_lab.cpachecker.cpa.shape.graphs.model;

import java.util.Objects;

public class StackUnit {

  private final String label;

  public StackUnit(String pLabel) {
    label = pLabel;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public int hashCode() {
    return Objects.hash(label);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof StackUnit)) {
      return false;
    }
    StackUnit other = (StackUnit) obj;
    return label.equals(other.label);
  }

  @Override
  public String toString() {
    return "[" + label + "]";
  }

  public static final StackUnit BUBBLE_UNIT = new StackUnit("___bubble_unit");
}
