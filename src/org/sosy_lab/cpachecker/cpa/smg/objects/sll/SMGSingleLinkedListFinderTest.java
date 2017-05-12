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

import com.google.common.collect.Iterables;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Set;


public class SMGSingleLinkedListFinderTest {
  @Test
  public void simpleListTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 5, 16, 8, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(1);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(1, candidates.size());
    SMGAbstractionCandidate candidate = Iterables.getOnlyElement(candidates);
    Assert.assertTrue(candidate instanceof SMGSingleLinkedListCandidate);
    SMGSingleLinkedListCandidate sllCandidate = (SMGSingleLinkedListCandidate) candidate;
    Assert.assertEquals(5, sllCandidate.getLength());
    Assert.assertEquals(8, sllCandidate.getOffset());
    SMGRegion expectedStart = (SMGRegion) smg.getPointer(root.getValue()).getObject();
    Assert.assertSame(expectedStart, sllCandidate.getStart());
  }

  @Test
  public void nullifiedPointerInferenceTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    TestHelpers.createGlobalList(smg, 2, 16, 8, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(1);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(1, candidates.size());
  }

  @Test
  public void listWithInboundPointersTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    Integer tail = TestHelpers.createList(smg, 4, 16, 8, "tail");

    SMGEdgeHasValue head = TestHelpers.createGlobalList(smg, 3, 16, 8, "head");

    SMGObject inside = new SMGRegion(16, "pointed_at");
    SMGEdgeHasValue tailConnection =
        new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 8, inside, tail);

    Integer addressOfInside = SMGValueFactory.getNewValue();
    SMGEdgePointsTo insidePT = new SMGEdgePointsTo(addressOfInside, inside, 0);
    SMGRegion inboundPointer = new SMGRegion(8, "inbound_pointer");
    SMGEdgeHasValue inboundPointerConnection =
        new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 0, inboundPointer, addressOfInside);

    SMGObject lastFromHead = smg.getPointer(head.getValue()).getObject();
    SMGEdgeHasValue connection = null;
    do {
      SMGEdgeHasValueFilter filter =
          SMGEdgeHasValueFilter.objectFilter(lastFromHead).filterAtOffset(8);
      Set<SMGEdgeHasValue> connections = smg.getHVEdges(filter);
      connection = null;
      if (connections.size() > 0) {
        connection = Iterables.getOnlyElement(connections);
        lastFromHead = smg.getPointer(connection.getValue()).getObject();
      }
    } while (connection != null);

    for (SMGEdgeHasValue hv : smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(lastFromHead))) {
      smg.removeHasValueEdge(hv);
    }

    SMGEdgeHasValue headConnection =
        new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 8, lastFromHead, addressOfInside);

    SMGRegion tailPointer = new SMGRegion(8, "tail_pointer");
    SMGEdgeHasValue tailPointerConnection =
        new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 0, tailPointer, tail);

    smg.addGlobalObject(tailPointer);
    smg.addHasValueEdge(tailPointerConnection);

    smg.addHeapObject(inside);
    smg.addValue(addressOfInside);
    smg.addPointsToEdge(insidePT);

    smg.addGlobalObject(inboundPointer);
    smg.addHasValueEdge(inboundPointerConnection);

    smg.addHasValueEdge(tailConnection);
    smg.addHasValueEdge(headConnection);

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(1);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(2, candidates.size());

    boolean sawHead = false;
    boolean sawTail = false;
    for (SMGAbstractionCandidate candidate : candidates) {
      SMGSingleLinkedListCandidate sllCandidate = (SMGSingleLinkedListCandidate) candidate;
      if (sllCandidate.getLength() == 3) {
        Assert.assertSame(smg.getPointer(head.getValue()).getObject(), sllCandidate.getStart());
        Assert.assertFalse(sawHead);
        sawHead = true;
      } else if (sllCandidate.getLength() == 4) {
        Assert.assertSame(smg.getPointer(tail).getObject(), sllCandidate.getStart());
        Assert.assertFalse(sawTail);
      } else {
        Assert.fail("We should not see any candidates with length other than 3 or 4");
      }
    }
  }
}
