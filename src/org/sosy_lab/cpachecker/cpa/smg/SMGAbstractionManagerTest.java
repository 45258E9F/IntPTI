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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.io.File;
import java.io.IOException;
import java.util.Set;


public class SMGAbstractionManagerTest {
  private CLangSMG smg;

  @Before
  public void setUp() {
    smg = new CLangSMG(MachineModel.LINUX64);

    SMGRegion globalVar = new SMGRegion(8, "pointer");

    SMGRegion next = null;
    for (int i = 0; i < 20; i++) {
      SMGRegion node = new SMGRegion(16, "node " + i);
      SMGEdgeHasValue hv;
      smg.addHeapObject(node);
      if (next != null) {
        int address = SMGValueFactory.getNewValue();
        SMGEdgePointsTo pt = new SMGEdgePointsTo(address, next, 0);
        hv = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 8, node, address);
        smg.addValue(address);
        smg.addPointsToEdge(pt);
      } else {
        hv = new SMGEdgeHasValue(16, 0, node, 0);
      }
      smg.addHasValueEdge(hv);
      next = node;
    }

    int address = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 8, globalVar, address);
    SMGEdgePointsTo pt = new SMGEdgePointsTo(address, next, 0);
    smg.addGlobalObject(globalVar);
    smg.addValue(address);
    smg.addPointsToEdge(pt);
    smg.addHasValueEdge(hv);
  }

  @Test
  public void testExecute() throws IOException {
    Path outputFile = Paths.get(new File(System.getProperty("user.dir") + "/output/source.dot"));
    HashBiMap<SMGKnownSymValue, SMGKnownExpValue> empty = HashBiMap.create();
    Files.writeFile(outputFile, new SMGPlotter().smgAsDot(smg, "test", "test", empty));
    SMGAbstractionManager manager = new SMGAbstractionManager(smg);
    CLangSMG afterAbstraction = manager.execute();

    SMGRegion globalVar = afterAbstraction.getObjectForVisibleVariable("pointer");
    Set<SMGEdgeHasValue> hvs =
        afterAbstraction.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalVar));
    Assert.assertEquals(1, hvs.size());
    SMGEdgeHasValue hv = Iterables.getOnlyElement(hvs);
    SMGEdgePointsTo pt = afterAbstraction.getPointer(hv.getValue());
    SMGObject segment = pt.getObject();
    Assert.assertTrue(segment.isAbstract());
    Set<SMGObject> heap = afterAbstraction.getHeapObjects();
    Assert.assertEquals(2, heap.size());
    outputFile = Paths.get(new File(System.getProperty("user.dir") + "/output/result.dot"));
    Files.writeFile(outputFile, new SMGPlotter().smgAsDot(afterAbstraction, "test", "test", empty));
  }
}
