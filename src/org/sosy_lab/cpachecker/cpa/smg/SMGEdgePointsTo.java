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

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGEdgePointsTo extends SMGEdge {
  private final int offset;
  private final SMGTargetSpecifier tg;

  public SMGEdgePointsTo(int pValue, SMGObject pObject, int pOffset) {
    super(pValue, pObject);
    offset = pOffset;

    if (pObject instanceof SMGRegion) {
      tg = SMGTargetSpecifier.REGION;
    } else {
      tg = SMGTargetSpecifier.UNKNOWN;
    }
  }

  public SMGEdgePointsTo(int pValue, SMGObject pObject, int pOffset, SMGTargetSpecifier pTg) {
    super(pValue, pObject);
    offset = pOffset;
    tg = pTg;
  }

  @Override
  public String toString() {
    return value + "->" + object.getLabel() + "+" + offset + 'b';
  }

  public int getOffset() {
    return offset;
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    /*
     * different value- > different place
     * same value -> same place
     */
    if (!(other instanceof SMGEdgePointsTo)) {
      return false;
    }

    if (value != other.value) {
      if (offset == ((SMGEdgePointsTo) other).offset && object == other.object) {
        return false;
      }
    } else if (offset != ((SMGEdgePointsTo) other).offset || object != other.object) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + (offset + tg.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SMGEdgePointsTo)) {
      return false;
    }
    SMGEdgePointsTo other = (SMGEdgePointsTo) obj;
    return super.equals(obj)
        && offset == other.offset && tg == other.tg;
  }

  public SMGTargetSpecifier getTargetSpecifier() {
    return tg;
  }
}