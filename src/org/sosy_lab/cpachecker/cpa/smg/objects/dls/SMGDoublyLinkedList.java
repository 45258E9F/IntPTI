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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


/**
 *
 */
public class SMGDoublyLinkedList extends SMGObject implements SMGAbstractObject {

  private final int minimumLength;

  private final int hfo;
  private final int nfo;
  private final int pfo;

  public SMGDoublyLinkedList(
      int pSize, int pHfo, int pNfo, int pPfo,
      int pMinLength, int level) {
    super(pSize, "__dls +" + pMinLength, level);

    hfo = pHfo;
    nfo = pNfo;
    pfo = pPfo;
    minimumLength = pMinLength;
  }

  public SMGDoublyLinkedList(SMGDoublyLinkedList other) {
    super(other.getSize(), other.getLabel(), other.getLevel());

    hfo = other.hfo;
    nfo = other.nfo;
    pfo = other.pfo;
    minimumLength = other.minimumLength;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject#matchGenericShape(org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject)
   */
  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return matchSpecificShape(pOther);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject#matchSpecificShape(org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject)
   */
  @Override
  public boolean matchSpecificShape(SMGAbstractObject obj) {

    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGDoublyLinkedList other = (SMGDoublyLinkedList) obj;
    if (hfo != other.hfo) {
      return false;
    }
    if (nfo != other.nfo) {
      return false;
    }
    if (pfo != other.pfo) {
      return false;
    }
    return true;
  }

  public int getMinimumLength() {
    return minimumLength;
  }

  @Override
  public boolean isMoreGeneral(SMGObject pOther) {
    super.isMoreGeneral(pOther);

    if (pOther instanceof SMGDoublyLinkedList) {
      return minimumLength < ((SMGDoublyLinkedList) pOther).getMinimumLength();
    }

    if (pOther instanceof SMGRegion) {
      return minimumLength > 0;
    }

    return false;
  }

  public int getHfo() {
    return hfo;
  }

  public int getNfo() {
    return nfo;
  }

  public int getPfo() {
    return pfo;
  }

  @Override
  public SMGObject join(SMGObject pOther, boolean pIncreaseLevel) {

    int maxLevel = Math.max(getLevel(), pOther.getLevel());

    if (pOther instanceof SMGDoublyLinkedList) {

      SMGDoublyLinkedList otherLinkedList = (SMGDoublyLinkedList) pOther;
      assert getSize() == otherLinkedList.getSize();
      assert getHfo() == otherLinkedList.getHfo();
      assert getNfo() == otherLinkedList.getNfo();
      assert getPfo() == otherLinkedList.getPfo();

      int minlength = Math.min(getMinimumLength(), otherLinkedList.getMinimumLength());


      if (pIncreaseLevel) {
        return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
            maxLevel + 1);
      } else {

        if (minimumLength == minlength && maxLevel == getLevel()) {
          return this;
        } else {
          return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), minlength,
              maxLevel);
        }
      }

    } else if (pOther instanceof SMGRegion) {
      assert getSize() == pOther.getSize();

      if (pIncreaseLevel) {
        return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), 0, maxLevel + 1);
      } else {
        if (minimumLength == 0 && maxLevel == getLevel()) {
          return this;
        } else {
          return new SMGDoublyLinkedList(getSize(), getHfo(), getNfo(), getPfo(), 0,
              maxLevel);
        }
      }

    } else {
      throw new AssertionError();
    }
  }

  @Override
  public SMGObject copy() {
    return null;
  }
}