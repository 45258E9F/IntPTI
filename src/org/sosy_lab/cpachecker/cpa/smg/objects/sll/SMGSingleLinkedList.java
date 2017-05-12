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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectVisitor;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public final class SMGSingleLinkedList extends SMGObject implements SMGAbstractObject {
  private int length;

  //TODO: Binding is likely to be more complicated later
  private int bindingOffset;

  public SMGSingleLinkedList(SMGRegion pPrototype, int pOffset, int pLength) {
    super(pPrototype.getSize(), "SLL");
    bindingOffset = pOffset;
    length = pLength;
  }

  public SMGSingleLinkedList(SMGSingleLinkedList pOriginal) {
    super(pOriginal);
    bindingOffset = pOriginal.bindingOffset;
    length = pOriginal.length;
  }

  //TODO: Abstract interface???
  public int getLength() {
    return length;
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  public int getOffset() {
    return bindingOffset;
  }

  @Override
  public String toString() {
    return "SLL(size=" + getSize() + ", bindingOffset=" + bindingOffset + ", len=" + length + ")";
  }

  @Override
  public void accept(SMGObjectVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther instanceof SMGSingleLinkedList;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    if (!matchGenericShape(pOther)) {
      return false;
    }
    return bindingOffset == ((SMGSingleLinkedList) pOther).bindingOffset;
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    super.isMoreGeneral(pOther);
    if (!pOther.isAbstract()) {
      return true;
    }
    if (!matchSpecificShape((SMGAbstractObject) pOther)) {
      throw new IllegalArgumentException("isMoreGeneral called on incompatible abstract objects");
    }
    return length < ((SMGSingleLinkedList) pOther).length;
  }

  @Override
  public SMGObject join(SMGObject pOther, boolean increaseLevel) {
    if (!pOther.isAbstract()) {
      return new SMGSingleLinkedList(this);
    }

    if (matchSpecificShape((SMGAbstractObject) pOther)) {
      SMGSingleLinkedList otherSll = (SMGSingleLinkedList) pOther;
      if (getLength() < otherSll.getLength()) {
        return new SMGSingleLinkedList(this);
      } else {
        return new SMGSingleLinkedList(otherSll);
      }
    }

    throw new UnsupportedOperationException("join() called on incompatible abstract objects");
  }

  @Override
  public SMGObject copy() {
    return new SMGSingleLinkedList(this);
  }
}
