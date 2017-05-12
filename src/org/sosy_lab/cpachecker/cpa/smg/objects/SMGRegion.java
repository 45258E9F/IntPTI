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
package org.sosy_lab.cpachecker.cpa.smg.objects;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;

import java.util.Map;

public final class SMGRegion extends SMGObject implements SMGObjectTemplate {

  public SMGRegion(int pSize, String pLabel) {
    super(pSize, pLabel);
  }

  public SMGRegion(SMGRegion pOther) {
    super(pOther);
  }

  public SMGRegion(int pSize, String pLabel, int pLevel) {
    super(pSize, pLabel, pLevel);
  }

  @Override
  public String toString() {
    return "REGION( " + getLabel() + ", " + getSize() + "b)";
  }

  public boolean propertiesEqual(SMGRegion pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null) {
      return false;
    }

    if (getLabel() == null) {
      if (pOther.getLabel() != null) {
        return false;
      }
    } else if (!getLabel().equals(pOther.getLabel())) {
      return false;
    }

    if (getSize() != pOther.getSize()) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public void accept(SMGObjectVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public SMGObject join(SMGObject pOther, boolean increaseLevel) {
    if (pOther.isAbstract()) {
      // I am concrete, and the other is abstract: the abstraction should
      // know how to join with me
      return pOther.join(this, increaseLevel);
    } else if (getSize() == pOther.getSize()) {
      if (increaseLevel) {
        return new SMGRegion(this.getSize(), this.getLabel(), getLevel() + 1);
      } else {
        return this;
      }
    }
    throw new UnsupportedOperationException("join() called on incompatible SMGObjects");
  }

  @Override
  public SMGRegion createConcreteObject(Map<Integer, Integer> pAbstractToConcretePointerMap) {
    return new SMGRegion(getSize(), getLabel() + " ID " + SMGValueFactory.getNewValue());
  }

  @Override
  public SMGObject copy() {
    return new SMGRegion(this);
  }
}