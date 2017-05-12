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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Objects;

public class SMGEdgeHasValue extends SMGEdge {
  final private CType type;
  final private int offset;

  public SMGEdgeHasValue(CType pType, int pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    type = pType;
    offset = pOffset;
  }

  public SMGEdgeHasValue(int pSizeInBytes, int pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    type = AnonymousTypes.createTypeWithLength(pSizeInBytes);
    offset = pOffset;
  }

  @Override
  public String toString() {
    return "sizeof(" + type.toASTString("foo") + ")b @ " + object.getLabel() + "+" + offset
        + "b has value " + value;
  }

  public int getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

  public int getSizeInBytes(MachineModel pMachineModel) {
    return pMachineModel.getSizeof(type);
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (!(other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if ((object == other.object) &&
        (offset == ((SMGEdgeHasValue) other).offset) &&
        (type == ((SMGEdgeHasValue) other).type)) {
      return (value == other.value);
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other, MachineModel pModel) {
    if (object != other.object) {
      throw new IllegalArgumentException(
          "Call of overlapsWith() on Has-Value edges pair not originating from the same object");
    }

    int otStart = other.getOffset();

    int otEnd = otStart + pModel.getSizeof(other.getType());

    return overlapsWith(otStart, otEnd, pModel);
  }

  public boolean overlapsWith(int pOtStart, int pOtEnd, MachineModel pModel) {

    int myStart = offset;

    int myEnd = myStart + pModel.getSizeof(type);

    if (myStart < pOtStart) {
      return (myEnd > pOtStart);

    } else if (pOtStart < myStart) {
      return (pOtEnd > myStart);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  public boolean isCompatibleField(SMGEdgeHasValue other) {
    return type.equals(other.type) && (offset == other.offset);
  }

  public boolean isCompatibleFieldOnSameObject(SMGEdgeHasValue other, MachineModel pModel) {
    // return (type.equals(other.type)) && (offset == other.offset) && (object == other.object);
    return pModel.getSizeof(type) == pModel.getSizeof(other.type) && (offset == other.offset)
        && object == other.object;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(type, offset);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SMGEdgeHasValue)) {
      return false;
    }
    SMGEdgeHasValue other = (SMGEdgeHasValue) obj;
    return super.equals(obj)
        && offset == other.offset
        && type.getCanonicalType().equals(other.type.getCanonicalType());
  }
}