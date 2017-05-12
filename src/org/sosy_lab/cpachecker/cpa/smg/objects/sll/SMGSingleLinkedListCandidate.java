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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Set;

public class SMGSingleLinkedListCandidate implements SMGAbstractionCandidate {
  private final SMGObject start;
  private final int offset;
  private int length;

  public SMGSingleLinkedListCandidate(SMGObject pStart, int pOffset, int pLength) {
    start = pStart;
    offset = pOffset;
    length = pLength;
  }

  @Override
  public CLangSMG execute(CLangSMG pSMG) {
    CLangSMG newSMG = new CLangSMG(pSMG);
    SMGSingleLinkedList sll;
    if (start instanceof SMGRegion) {
      sll = new SMGSingleLinkedList((SMGRegion) start, offset, length);
    } else {
      sll = new SMGSingleLinkedList((SMGSingleLinkedList) start);
    }
    newSMG.addHeapObject(sll);

    //TODO: Better filtering of the pointers!!!
    for (SMGEdgePointsTo pt : pSMG.getPTEdges().values()) {
      if (pt.getObject().equals(start)) {
        SMGEdgePointsTo newPt = new SMGEdgePointsTo(pt.getValue(), sll, pt.getOffset());
        newSMG.removePointsToEdge(pt.getValue());
        newSMG.addPointsToEdge(newPt);
      }
    }

    SMGObject node = start;
    Integer value = null;
    SMGEdgeHasValue edgeToFollow = null;
    for (int i = 0; i < length; i++) {
      if (value != null) {
        newSMG.removeValue(value);
        newSMG.removePointsToEdge(value);
      }

      Set<SMGEdgeHasValue> outboundEdges =
          newSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(node).filterAtOffset(offset));
      edgeToFollow = null;
      for (SMGEdgeHasValue outbound : outboundEdges) {
        CType fieldType = outbound.getType();
        if (fieldType instanceof CPointerType) {
          edgeToFollow = outbound;
          break;
        }
      }
      if (edgeToFollow == null) {
        edgeToFollow =
            new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, offset, node, newSMG.getNullValue());
      }

      value = edgeToFollow.getValue();
      newSMG.removeHeapObjectAndEdges(node);
      node = newSMG.getPointer(value).getObject();
    }
    SMGEdgeHasValue newOutbound = new SMGEdgeHasValue(edgeToFollow.getType(), offset, sll, value);
    newSMG.addHasValueEdge(newOutbound);

    return newSMG;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  public void addLength(int pLength) {
    length += pLength;
  }

  public boolean isCompatibleWith(SMGSingleLinkedListCandidate pOther) {
    return (offset == pOther.offset) && (start.getSize() == pOther.start.getSize());
  }

  public SMGObject getStart() {
    return start;
  }

  @Override
  public String toString() {
    return "SLL CANDIDATE(start=" + start + ", offset=" + offset + ", length=" + length + ")";
  }
}