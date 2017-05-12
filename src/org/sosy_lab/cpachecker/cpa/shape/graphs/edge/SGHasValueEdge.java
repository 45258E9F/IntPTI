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
package org.sosy_lab.cpachecker.cpa.shape.graphs.edge;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.UnknownTypes;

import java.util.Objects;

public class SGHasValueEdge extends SGEdge {

  /**
   * How to interpret the specified memory block?
   */
  private final CType type;
  /**
   * The offset in the pointed-to memory block
   */
  private final int offset;

  public SGHasValueEdge(CType pType, int pOffset, SGObject pObject, long pValue) {
    super(pValue, pObject);
    type = pType;
    offset = pOffset;
  }

  public SGHasValueEdge(int pSizeInBytes, int pOffset, SGObject pObject, long pValue) {
    super(pValue, pObject);
    type = UnknownTypes.createTypeWithLength(pSizeInBytes);
    offset = pOffset;
  }

  @Override
  public String toString() {
    return "sizeof(" + type.toString() + ")b @ " + object.getLabel() + "+" + offset + "b has "
        + "value " + value;
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

  public boolean overlapsWith(SGHasValueEdge other, MachineModel model) {
    if (object != other.object) {
      // certainly they could not overlap
      return false;
    }
    int otherStart = other.getOffset();
    int otherEnd = otherStart + model.getSizeof(other.getType());
    return overlapsWith(otherStart, otherEnd, model);
  }

  public boolean overlapsWith(int pOtherStart, int pOtherEnd, MachineModel model) {
    int myStart = offset;
    int myEnd = myStart + model.getSizeof(type);
    if (myStart < pOtherStart) {
      return (myEnd > pOtherStart);
    } else if (pOtherStart < myStart) {
      return (pOtherEnd > myStart);
    }
    return true;
  }

  /**
   * Check whether current has-value edge is within the specified scope.
   */
  public boolean within(int pOtherStart, int pOtherEnd, MachineModel model) {
    int myStart = offset;
    int myEnd = myStart + model.getSizeof(type);
    if (pOtherStart <= myStart && pOtherEnd >= myEnd) {
      return true;
    }
    return false;
  }

  public boolean isCompatibleFieldOnTheSameObject(SGHasValueEdge other, MachineModel model) {
    return (model.getSizeof(type) == model.getSizeof(other.type)) &&
        (offset == other.offset) && (object == other.object);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(type, offset);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SGHasValueEdge)) {
      return false;
    }
    SGHasValueEdge other = (SGHasValueEdge) obj;
    return super.equals(obj) && offset == other.offset &&
        type.getCanonicalType().equals(other.type.getCanonicalType());
  }
}
