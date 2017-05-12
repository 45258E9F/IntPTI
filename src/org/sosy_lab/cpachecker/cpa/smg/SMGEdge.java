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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;


public abstract class SMGEdge {
  final protected int value;
  final protected SMGObject object;

  SMGEdge(int pValue, SMGObject pObject) {
    value = pValue;
    object = pObject;
  }

  public int getValue() {
    return value;
  }

  public SMGObject getObject() {
    return object;
  }

  public abstract boolean isConsistentWith(SMGEdge pOther_edge);

  @Override
  public int hashCode() {
    return Objects.hashCode(object, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof SMGEdge)) {
      return false;
    }
    SMGEdge other = (SMGEdge) obj;
    return value == other.value
        && Objects.equal(object, other.object);
  }
}
