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

import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

public final class SGResource extends SGObject {

  private final String creator;

  public SGResource(String pLabel, String pCreator) {
    // the size of FILE is OS-dependent, thus its size remains unknown in general
    super(UnknownValue.getInstance(), pLabel);
    creator = pCreator;
  }

  private SGResource(SGResource pOther) {
    super(pOther);
    creator = pOther.creator;
  }

  public String getCreator() {
    return creator;
  }

  @Override
  public String toString() {
    return "FILE( " + getLabel() + ", " + creator + ")";
  }

  @Override
  public SGObject copy() {
    return new SGResource(this);
  }

  @Override
  public SGObject join(SGObject pOther) {
    if (pOther instanceof SGResource) {
      SGResource that = (SGResource) pOther;
      if (size.equals(that.size) && label.equals(that.label) &&
          zeroInitialized == that.zeroInitialized && creator.equals(that.creator)) {
        return this;
      }
    }
    return SGAbstract.merge(this, pOther);
  }
}
