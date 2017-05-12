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

import com.google.common.collect.Iterables;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Set;


public class SMGSingleLinkedListCandidateTest {

  @Test
  public void basicTest() {
    SMGObject object = new SMGRegion(8, "object");
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(object, 4, 2);

    Assert.assertSame(object, candidate.getStart());
    Assert.assertEquals(4, candidate.getOffset());
    Assert.assertEquals(2, candidate.getLength());

    candidate.addLength(4);
    Assert.assertEquals(4, candidate.getOffset());
    Assert.assertEquals(6, candidate.getLength());
  }

  @Test
  public void isCompatibleWithTest() {
    SMGObject object8_1 = new SMGRegion(8, "object 1");
    SMGObject object8_2 = new SMGRegion(8, "object 2");
    SMGObject object16 = new SMGRegion(16, "object 3");

    SMGSingleLinkedListCandidate candidate8_1 = new SMGSingleLinkedListCandidate(object8_1, 4, 2);
    SMGSingleLinkedListCandidate candidate8_2 = new SMGSingleLinkedListCandidate(object8_2, 4, 8);
    SMGSingleLinkedListCandidate candidate16 = new SMGSingleLinkedListCandidate(object16, 4, 2);

    Assert.assertTrue(candidate8_1.isCompatibleWith(candidate8_2));
    Assert.assertTrue(candidate8_2.isCompatibleWith(candidate8_1));
    Assert.assertFalse(candidate16.isCompatibleWith(candidate8_1));
    Assert.assertFalse(candidate8_1.isCompatibleWith(candidate16));

    candidate8_2 = new SMGSingleLinkedListCandidate(object8_2, 6, 2);
    Assert.assertFalse(candidate8_1.isCompatibleWith(candidate8_2));
  }

  @Test
  public void executeOnSimpleList() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    int NODE_SIZE = 8;
    int SEGMENT_LENGTH = 4;
    int OFFSET = 0;

    SMGEdgeHasValue root =
        TestHelpers.createGlobalList(smg, SEGMENT_LENGTH + 1, NODE_SIZE, OFFSET, "pointer");
    Integer value = root.getValue();

    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate =
        new SMGSingleLinkedListCandidate(startObject, OFFSET, SEGMENT_LENGTH);

    CLangSMG abstractedSmg = candidate.execute(smg);
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(3, heap.size());
    SMGObject pointedObject = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(pointedObject instanceof SMGSingleLinkedList);
    Assert.assertTrue(pointedObject.isAbstract());
    SMGSingleLinkedList segment = (SMGSingleLinkedList) pointedObject;
    Assert.assertEquals(NODE_SIZE, segment.getSize());
    Assert.assertEquals(SEGMENT_LENGTH, segment.getLength());
    Assert.assertEquals(OFFSET, segment.getOffset());
    Set<SMGEdgeHasValue> outboundEdges =
        abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(segment));
    Assert.assertEquals(1, outboundEdges.size());
    SMGEdgeHasValue onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(OFFSET, onlyOutboundEdge.getOffset());
    Assert.assertSame(CPointerType.POINTER_TO_VOID, onlyOutboundEdge.getType());

    SMGObject stopper = abstractedSmg.getPointer(onlyOutboundEdge.getValue()).getObject();
    Assert.assertTrue(stopper instanceof SMGRegion);
    SMGRegion stopperRegion = (SMGRegion) stopper;
    Assert.assertEquals(NODE_SIZE, stopperRegion.getSize());
    outboundEdges = abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(stopperRegion));
    Assert.assertEquals(1, outboundEdges.size());
    onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(0, onlyOutboundEdge.getValue());
    Assert.assertEquals(0, onlyOutboundEdge.getOffset());
    Assert
        .assertEquals(NODE_SIZE, onlyOutboundEdge.getSizeInBytes(abstractedSmg.getMachineModel()));
  }

  @Test
  public void executeOnNullTerminatedList() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 2, 16, 8, "pointer");

    Integer value = root.getValue();
    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(startObject, 8, 2);
    CLangSMG abstractedSmg = candidate.execute(smg);
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(2, heap.size());

    SMGObject sll = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(sll.isAbstract());
    Assert.assertTrue(sll instanceof SMGSingleLinkedList);
    SMGSingleLinkedList realSll = (SMGSingleLinkedList) sll;
    Assert.assertEquals(2, realSll.getLength());
    Set<SMGEdgeHasValue> outboundEdges =
        abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(realSll));
    Assert.assertEquals(1, outboundEdges.size());
    SMGEdgeHasValue outbound = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(8, outbound.getOffset());
    Assert.assertEquals(8, outbound.getSizeInBytes(abstractedSmg.getMachineModel()));
    Assert.assertEquals(0, outbound.getValue());
  }
}
