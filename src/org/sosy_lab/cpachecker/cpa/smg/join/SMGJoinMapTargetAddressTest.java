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
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGJoinMapTargetAddressTest {

  private SMG smg1;
  private SMG destSMG;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  final SMGRegion obj1 = new SMGRegion(8, "ze label");
  final Integer value1 = SMGValueFactory.getNewValue();
  final SMGEdgePointsTo edge1 = new SMGEdgePointsTo(value1, obj1, 0);

  final Integer value2 = SMGValueFactory.getNewValue();

  final SMGObject destObj = new SMGRegion(8, "destination");
  final Integer destValue = SMGValueFactory.getNewValue();

  @Before
  public void setUp() {
    smg1 = new SMG(MachineModel.LINUX64);
    destSMG = new SMG(MachineModel.LINUX64);
    mapping1 = new SMGNodeMapping();
    mapping2 = new SMGNodeMapping();
  }

  @Test
  public void mapTargetAddressExistingNull() {
    SMG origDestSMG = new SMG(destSMG);
    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);

    SMGJoinMapTargetAddress mta =
        new SMGJoinMapTargetAddress(smg1, destSMG, mapping1, null, smg1.getNullValue(), null,
            false);
    Assert.assertEquals(origDestSMG, mta.getSMG());
    Assert.assertEquals(origMapping1, mta.getMapping1());
    Assert.assertNull(mta.getMapping2());
    Assert.assertSame(destSMG.getNullValue(), mta.getValue());
  }

  @Test
  public void mapTargetAddressExisting() {
    SMGEdgePointsTo destEdge = new SMGEdgePointsTo(destValue, destObj, 0);

    smg1.addValue(value1);
    smg1.addObject(obj1);
    smg1.addPointsToEdge(edge1);

    destSMG.addValue(destValue);
    destSMG.addObject(destObj);
    destSMG.addPointsToEdge(destEdge);

    mapping1.map(obj1, destObj);

    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);
    SMG origDestSMG = new SMG(destSMG);

    SMGJoinMapTargetAddress mta =
        new SMGJoinMapTargetAddress(smg1, destSMG, mapping1, null, value1, null, false);
    Assert.assertEquals(origDestSMG, mta.getSMG());
    Assert.assertEquals(origMapping1, mta.getMapping1());
    Assert.assertNull(mta.getMapping2());
    Assert.assertSame(destValue, mta.getValue());
  }

  @Test
  public void mapTargetAddressNew() {
    smg1.addValue(value1);
    smg1.addObject(obj1);
    smg1.addPointsToEdge(edge1);

    destSMG.addObject(destObj);

    mapping1.map(obj1, destObj);

    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);
    SMGNodeMapping origMapping2 = new SMGNodeMapping(mapping2);
    SMG origDestSMG = new SMG(destSMG);

    SMGJoinMapTargetAddress mta =
        new SMGJoinMapTargetAddress(smg1, destSMG, mapping1, mapping2, value1, value2, false);
    Assert.assertNotEquals(origDestSMG, mta.getSMG());
    Assert.assertNotEquals(origMapping1, mta.getMapping1());
    Assert.assertNotEquals(origMapping2, mta.getMapping2());

    Assert.assertFalse(origDestSMG.getValues().contains(mta.getValue()));

    SMGEdgePointsTo newEdge = destSMG.getPointer(mta.getValue());
    Assert.assertSame(destObj, newEdge.getObject());
    Assert.assertEquals(0, newEdge.getOffset());

    Assert.assertSame(mta.getValue(), mta.getMapping1().get(value1));
    Assert.assertSame(mta.getValue(), mta.getMapping2().get(value2));
  }
}
