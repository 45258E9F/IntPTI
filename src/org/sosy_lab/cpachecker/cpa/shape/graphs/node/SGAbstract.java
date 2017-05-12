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
package org.sosy_lab.cpachecker.cpa.shape.graphs.node;

import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

/**
 * This special kind of shape object represents abstracted objects. When such object is accessed,
 * we should query in the abstraction tree to perform refinement.
 */
public final class SGAbstract extends SGObject {

  private static final String nameTemplate = "ABSTRACT:%d";

  public SGAbstract(int pSize) {
    super(pSize, String.format(nameTemplate, SymbolicValueFactory.getNewValue()));
  }

  public SGAbstract(ShapeExplicitValue pSize) {
    super(pSize, String.format(nameTemplate, SymbolicValueFactory.getNewValue()));
  }

  private SGAbstract(SGAbstract pOther) {
    super(pOther);
  }

  public static SGAbstract merge(SGObject pObject1, SGObject pObject2) {
    ShapeExplicitValue size1 = pObject1.getSize();
    ShapeExplicitValue size2 = pObject2.getSize();
    if (size1.isUnknown() || size2.isUnknown()) {
      return new SGAbstract(UnknownValue.getInstance());
    }
    if (size1.getAsLong() < size2.getAsLong()) {
      return new SGAbstract(size2);
    } else {
      return new SGAbstract(size1);
    }
  }

  @Override
  public String toString() {
    return "ABSTRACT( " + getLabel() + ")";
  }

  @Override
  public SGObject copy() {
    return new SGAbstract(this);
  }

  @Override
  public SGObject join(SGObject pOther) {
    if (pOther instanceof SGAbstract) {
      SGAbstract that = (SGAbstract) pOther;
      if (size.equals(pOther.size) && label.equals(that.label) &&
          zeroInitialized == that.zeroInitialized) {
        return this;
      }
    }
    return SGAbstract.merge(this, pOther);
  }
}
