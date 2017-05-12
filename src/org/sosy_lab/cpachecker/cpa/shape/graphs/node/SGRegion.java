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
package org.sosy_lab.cpachecker.cpa.shape.graphs.node;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;

public final class SGRegion extends SGObject {

  private final CType type;
  private final boolean isDynamic;

  public static final boolean DYNAMIC = true;
  public static final boolean STATIC = false;

  /**
   * Constructor of {@link SGRegion}
   *
   * @param pLabel     the label of current region
   * @param pType      the type of current region
   * @param pSize      the size of memory region (NOTE: if the type is T*, then it can be the region
   *                   of a dynamic array whose length cannot statically determined)
   * @param pIsDynamic whether this region is dynamically allocated
   */
  public SGRegion(String pLabel, CType pType, ShapeExplicitValue pSize, boolean pIsDynamic) {
    super(pSize, pLabel);
    type = pType;
    isDynamic = pIsDynamic;
  }

  public SGRegion(
      String pLabel, CType pType, ShapeExplicitValue pSize, boolean pIsDynamic,
      boolean pZero) {
    super(pSize, pLabel, pZero);
    type = pType;
    isDynamic = pIsDynamic;
  }

  /**
   * This constructor is applicable when the size of memory block is certain.
   */
  public SGRegion(String pLabel, CType pType, int pSize, boolean pIsDynamic) {
    super(pSize, pLabel);
    type = pType;
    isDynamic = pIsDynamic;
  }

  private SGRegion(SGRegion pOther) {
    super(pOther);
    type = pOther.type;
    isDynamic = pOther.isDynamic;
  }


  public CType getType() {
    return type;
  }

  public boolean isDynamic() {
    return isDynamic;
  }

  @Override
  public String toString() {
    return "REGION( " + getLabel() + ", " + getSize().toString() + "b)";
  }

  @Override
  public SGObject copy() {
    return new SGRegion(this);
  }

  @Override
  public SGObject join(SGObject pOther) {
    if (pOther instanceof SGRegion) {
      SGRegion that = (SGRegion) pOther;
      // perform a semantic equality check (but we do not want to override equals() because each
      // instance of SGObject uniquely represent a memory object)
      // NOTE: the labels of two objects possibly differ, in general case
      if (size.equals(that.size) && zeroInitialized == that.zeroInitialized &&
          isDynamic == that.isDynamic) {
        return this;
      }
    }
    return SGAbstract.merge(this, pOther);
  }
}

