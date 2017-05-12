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
package org.sosy_lab.cpachecker.cpa.smg.objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SMGRegionTest {

  @Before
  public void setUp() {
  }

  @Test
  public void testIsAbstract() {
    SMGRegion region = new SMGRegion(8, "region");
    Assert.assertFalse(region.isAbstract());
  }

  @Test
  public void testJoin() {
    SMGRegion region = new SMGRegion(8, "region");
    SMGRegion region_same = new SMGRegion(8, "region");
    SMGObject objectJoint = region.join(region_same, false);
    Assert.assertTrue(objectJoint instanceof SMGRegion);
    SMGRegion regionJoint = (SMGRegion) objectJoint;

    Assert.assertEquals(8, regionJoint.getSize());
    Assert.assertEquals("region", regionJoint.getLabel());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testJoinDiffSize() {
    SMGRegion region = new SMGRegion(8, "region");
    SMGRegion regionDiff = new SMGRegion(10, "region");
    region.join(regionDiff, false);
  }

  @Test
  public void testPropertiesEqual() {
    SMGRegion one = new SMGRegion(8, "region");
    SMGRegion two = new SMGRegion(8, "region");
    SMGRegion three = new SMGRegion(10, "region");
    SMGRegion four = new SMGRegion(8, "REGION");

    Assert.assertTrue(one.propertiesEqual(two));
    Assert.assertFalse(one.propertiesEqual(three));
    Assert.assertFalse(one.propertiesEqual(four));
  }

}
