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

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGEdgePointsToTest {

  @Test
  public void testSMGEdgePointsTo() {
    Integer val = Integer.valueOf(6);
    SMGObject obj = new SMGRegion(8, "object");
    SMGEdgePointsTo edge = new SMGEdgePointsTo(val, obj, 0);

    Assert.assertEquals(val.intValue(), edge.getValue());
    Assert.assertEquals(obj, edge.getObject());
    Assert.assertEquals(0, edge.getOffset());
  }

  @Test
  public void testIsConsistentWith() {
    Integer val1 = Integer.valueOf(1);
    Integer val2 = Integer.valueOf(2);
    SMGObject obj = new SMGRegion(8, "object");
    SMGObject obj2 = new SMGRegion(8, "object2");

    SMGEdgePointsTo edge1 = new SMGEdgePointsTo(val1, obj, 0);
    SMGEdgePointsTo edge2 = new SMGEdgePointsTo(val2, obj, 0);
    SMGEdgePointsTo edge3 = new SMGEdgePointsTo(val1, obj, 4);
    SMGEdgePointsTo edge4 = new SMGEdgePointsTo(val1, obj2, 0);

    // An edge is consistent with itself
    Assert.assertTrue(edge1.isConsistentWith(edge1));

    // Different vals pointing to same place: violates "injective"
    Assert.assertFalse(edge1.isConsistentWith(edge2));

    // Same val pointing to different offsets
    Assert.assertFalse(edge1.isConsistentWith(edge3));

    // Same val pointing to different objects
    Assert.assertFalse(edge1.isConsistentWith(edge4));
  }
}
