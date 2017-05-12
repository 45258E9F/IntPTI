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
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGJoinSubSMGsTest {

  SMGJoinSubSMGs jssDefined;

  @Before
  public void setUp() throws SMGInconsistentException {
    SMG smg1 = new SMG(MachineModel.LINUX64);
    SMG smg2 = new SMG(MachineModel.LINUX64);
    SMG destSmg = new SMG(MachineModel.LINUX64);

    SMGObject obj1 = new SMGRegion(8, "Test object 1");
    SMGObject obj2 = new SMGRegion(8, "Test object 2");

    smg1.addObject(obj1);
    smg2.addObject(obj2);

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    jssDefined =
        new SMGJoinSubSMGs(SMGJoinStatus.EQUAL, smg1, smg2, destSmg, mapping1, mapping2, obj1, obj2,
            null, 0, false, false);
  }

  @Test
  public void testIsDefined() {
    Assert.assertTrue(jssDefined.isDefined());
  }

  @Test
  public void testGetStatusOnDefined() {
    Assert.assertNotNull(jssDefined.getStatus());
  }

  @Test
  public void testGetSMG1() {
    Assert.assertNotNull(jssDefined.getSMG1());
  }

  @Test
  public void testGetSMG2() {
    Assert.assertNotNull(jssDefined.getSMG2());
  }

  @Test
  public void testGetDestSMG() {
    Assert.assertNotNull(jssDefined.getDestSMG());
  }

  @Test
  public void testGetMapping1() {
    Assert.assertNotNull(jssDefined.getMapping1());
  }

  @Test
  public void testGetMapping2() {
    Assert.assertNotNull(jssDefined.getMapping2());
  }
}
