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

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.objects.DummyAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGSingleLinkedListTest {

  @Test
  public void basicsTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(prototype, 2, 4);

    Assert.assertTrue(sll.isAbstract());
    Assert.assertEquals(4, sll.getLength());
    Assert.assertEquals(16, sll.getSize());
    Assert.assertEquals(2, sll.getOffset());
  }

  @Test
  public void matchGenericShapeTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 0, 4);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 0, 7);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(prototype, 8, 4);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    Assert.assertFalse(sll1.matchGenericShape(dummy));
    Assert.assertTrue(sll1.matchGenericShape(sll2));
    Assert.assertTrue(sll2.matchGenericShape(sll3));
    Assert.assertTrue(sll1.matchGenericShape(sll3));
  }

  @Test
  public void matchSpecificShapeTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 0, 4);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 0, 7);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(prototype, 8, 4);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    Assert.assertFalse(sll1.matchSpecificShape(dummy));
    Assert.assertTrue(sll1.matchSpecificShape(sll2));
    Assert.assertFalse(sll2.matchSpecificShape(sll3));
    Assert.assertFalse(sll1.matchSpecificShape(sll3));
  }
}
