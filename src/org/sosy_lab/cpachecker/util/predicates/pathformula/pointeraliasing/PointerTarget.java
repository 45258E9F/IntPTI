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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.io.Serializable;

public class PointerTarget implements Serializable {

  /**
   * This constructor is for variables of simple types (e.g. long, char etc.)
   */
  PointerTarget(String base) {
    this.base = base;
    this.containerType = null;
    this.properOffset = 0;
    this.containerOffset = 0;
  }

  /**
   * This constructor is for structure fields (e.g. s->f) and array elements (e.g. p[5])
   * NOTE: The container (structure or array) must not be contained in any other structure or array
   */
  PointerTarget(String base, CType containerType, int properOffset) {
    this.base = base;
    this.containerType = containerType;
    this.properOffset = properOffset;
    this.containerOffset = 0;
  }

  /**
   * This constructor is for fields of nested structures and arrays
   */
  public PointerTarget(String base, CType containerType, int properOffset, int containerOffset) {
    this.base = base;
    this.containerType = containerType;
    this.properOffset = properOffset;
    this.containerOffset = containerOffset;
  }

  public String getBase() {
    return base;
  }

  public String getBaseName() {
    return PointerTargetSet.getBaseName(base);
  }

  public int getOffset() {
    return containerOffset + properOffset;
  }

  public int getProperOffset() {
    assert containerType != null : "The target's offset is ill-defined";
    return properOffset;
  }

  public boolean isBase() {
    return containerType == null;
  }

  public CType getContainerType() {
    return containerType;
  }

  public int getContainerOffset() {
    assert containerType != null : "The target's container offset is ill-defined";
    return containerOffset;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof PointerTarget)) {
      return false;
    } else {
      final PointerTarget o = (PointerTarget) other;
      return properOffset == o.properOffset &&
          containerOffset == o.containerOffset &&
          base.equals(o.base) &&
          (containerType != null ?
           o.containerType != null && containerType.getCanonicalType()
               .equals(o.containerType.getCanonicalType()) :
           o.containerType == null);
    }
  }

  @Override
  public int hashCode() {
    return 31 * base.hashCode() + 17 * containerOffset + properOffset;
  }

  @Override
  public String toString() {
    return String
        .format("(Base: %s, type: %s, prop. offset: %d, cont. offset: %d)", base, containerType,
            properOffset, containerOffset);
  }

  final String base;
  final CType containerType;
  final int properOffset;
  final int containerOffset;

  private static final long serialVersionUID = -1258065871533686442L;
}
