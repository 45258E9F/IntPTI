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
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public final class TestHelpers {
  static public Integer createList(
      CLangSMG pSmg,
      int pLength,
      int pSize,
      int pOffset,
      String pPrefix) {
    Integer value = null;
    for (int i = 0; i < pLength; i++) {
      SMGObject node = new SMGRegion(pSize, pPrefix + "list_node" + i);
      SMGEdgeHasValue hv;
      if (value == null) {
        hv = new SMGEdgeHasValue(pSize, 0, node, 0);
      } else {
        hv = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, pOffset, node, value);
      }
      value = SMGValueFactory.getNewValue();
      SMGEdgePointsTo pt = new SMGEdgePointsTo(value, node, 0);
      pSmg.addHeapObject(node);
      pSmg.addValue(value);
      pSmg.addHasValueEdge(hv);
      pSmg.addPointsToEdge(pt);
    }
    return value;
  }

  static public SMGEdgeHasValue createGlobalList(
      CLangSMG pSmg,
      int pLength,
      int pSize,
      int pOffset,
      String pVariable) {
    Integer value = TestHelpers.createList(pSmg, pLength, pSize, pOffset, pVariable);
    SMGRegion globalVar = new SMGRegion(8, pVariable);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 0, globalVar, value);
    pSmg.addGlobalObject(globalVar);
    pSmg.addHasValueEdge(hv);

    return hv;
  }

  private TestHelpers() {
  }
}
