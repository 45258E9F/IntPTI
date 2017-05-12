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
package org.sosy_lab.cpachecker.cpa.shape.merge.joiner;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGNodeMapping;

public class SGAddressValueJoin {

  private CShapeGraph destGraph;
  private SGNodeMapping mapping1;
  private SGNodeMapping mapping2;
  private GuardedAbstraction absInfo;

  private Long merged;

  /**
   * Join two address (pointer) values.
   * MODIFIED: pDG (since we are to add a new point-to edge), pMap1, pMap2
   * Precondition:
   * (1) pAddress1 and pAddress2 are not both null values
   * (2) the target objects of address1/address2 have already been merged (mapping entry exists
   * in the shape graph node mapping)
   */
  public SGAddressValueJoin(
      CShapeGraph pG1, CShapeGraph pG2, CShapeGraph pDG,
      SGNodeMapping pMap1, SGNodeMapping pMap2,
      Long pAddress1, Long pAddress2,
      GuardedAbstraction pAbsInfo,
      MergeTable pTable) {
    destGraph = pDG;
    mapping1 = pMap1;
    mapping2 = pMap2;
    absInfo = pAbsInfo;

    SGPointToEdge pointer1 = pG1.getPointer(pAddress1);
    SGPointToEdge pointer2 = pG2.getPointer(pAddress2);
    assert (pointer1 != null && pointer2 != null);
    SGObject target1 = pointer1.getObject();
    SGObject target2 = pointer2.getObject();
    int offset;
    SGObject target;
    if (target1.notNull()) {
      offset = pointer1.getOffset();
      target = mapping1.get(target1);
    } else {
      offset = pointer2.getOffset();
      target = mapping2.get(target2);
    }
    for (SGPointToEdge edge : destGraph.getPTEdges().values()) {
      if (edge.getObject() == target && edge.getOffset() == offset) {
        merged = edge.getValue();
        return;
      }
    }
    // Precondition: two address values are consistent
    Long newValue = Preconditions.checkNotNull(pTable.merge(pAddress1, pAddress2));
    destGraph.addValue(newValue);
    merged = newValue;
    SGPointToEdge newEdge = new SGPointToEdge(merged, target, offset);
    destGraph.addPointToEdge(newEdge);
  }

  /* *********** */
  /* write backs */
  /* *********** */

  public CShapeGraph getDestGraph() {
    return destGraph;
  }

  public SGNodeMapping getMapping1() {
    return mapping1;
  }

  public SGNodeMapping getMapping2() {
    return mapping2;
  }

  public GuardedAbstraction getAbstraction() {
    return absInfo;
  }

  public Long getValue() {
    return merged;
  }

}
