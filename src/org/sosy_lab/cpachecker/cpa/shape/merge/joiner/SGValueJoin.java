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
package org.sosy_lab.cpachecker.cpa.shape.merge.joiner;

import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGNodeMapping;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;

public class SGValueJoin {

  private CShapeGraph inputGraph1;
  private CShapeGraph inputGraph2;
  private CShapeGraph destGraph;

  private SGNodeMapping mapping1;
  private SGNodeMapping mapping2;

  private GuardedAbstraction absInfo;

  private boolean isDefined = false;
  private Long merged;

  /**
   * Merge two has-value edges.
   * MODIFIED: pG1, pG2 (due to recursive call to SGObjectJoin), pDG, pMap1, pMap2
   */
  public SGValueJoin(
      CShapeGraph pG1, CShapeGraph pG2, CShapeGraph pDG,
      SGNodeMapping pMap1, SGNodeMapping pMap2,
      SGHasValueEdge pEdge1, SGHasValueEdge pEdge2,
      GuardedAbstraction pAbsInfo,
      MergeTable pTable) {
    inputGraph1 = pG1;
    inputGraph2 = pG2;
    destGraph = pDG;
    mapping1 = pMap1;
    mapping2 = pMap2;
    absInfo = pAbsInfo;

    Long v1 = pEdge1.getValue();
    Long v2 = pEdge2.getValue();

    // STEP 1: several sanity checks
    // if two values are not pointers, we can directly merge them
    if (joinNonPointerValues(pEdge1, pEdge2, pTable)) {
      return;
    }
    // if one value is pointer and the other is not, join operation is undefined
    if (joinMixedPointerValues(v1, v2)) {
      return;
    }
    // STEP 2: if we reach here, two values are pointers
    SGTargetObjectJoin targetJoin = new SGTargetObjectJoin(inputGraph1, inputGraph2, destGraph,
        mapping1, mapping2, v1, v2, absInfo, pTable);
    if (targetJoin.isDefined()) {
      merged = targetJoin.getValue();
      isDefined = true;
      targetJoinUpdater(targetJoin);
    }
  }

  /* ************* */
  /* sanity checks */
  /* ************* */

  private boolean joinNonPointerValues(
      SGHasValueEdge pHV1, SGHasValueEdge pHV2, MergeTable
      pTable) {
    Long v1 = pHV1.getValue();
    Long v2 = pHV2.getValue();
    if (inputGraph1.isPointer(v1) || inputGraph2.isPointer(v2)) {
      // we do not handle this case here
      return false;
    }
    // Precondition: v1 and v2 have not been merged yet
    merged = mergeSymbolicValues(v1, v2, inputGraph1, inputGraph2, destGraph, absInfo,
        pTable);
    isDefined = true;
    return true;
  }

  private boolean joinMixedPointerValues(Long pV1, Long pV2) {
    return (inputGraph1.isPointer(pV1) != inputGraph2.isPointer(pV2));
  }

  /* ************** */
  /* symbolic merge */
  /* ************** */

  /**
   * Merge two symbolic values.
   * Precondition:
   * (1) v1 and v2 have not been merged yet;
   * (2) v1 and v2 are feasible to be merged.
   * Lemma: Let R1 and R2 be two equivalence relations of two shape states to be merged, then R1
   * \cap R2 = R where each representative of R1 is also a representative of R.
   * Proof:
   * For a representative x of R1, which is not a representative of R. Thus, we have x \in [y]
   * where y < x. [y] = [x] \cap [s] where x \in [s] in R2. Since y \in [x] and y < x, then [x]
   * should be [y], which was a contradiction.
   */
  public static Long mergeSymbolicValues(
      Long v1, Long v2, CShapeGraph pG1, CShapeGraph pG2,
      CShapeGraph pGd, GuardedAbstraction pAbsInfo,
      MergeTable pTable) {
    Long newValue = pTable.merge(v1, v2);
    if (newValue == null) {
      KnownExplicitValue expValue = pTable.getExplicitEquality(v1, v2);
      if (expValue != null) {
        // check if there has already been an auxiliary value for the certain explicit value
        Long symValue = pTable.getAssociatedSymbolic(expValue);
        if (symValue != null) {
          return symValue;
        } else {
          // associate a fresh value with the certain explicit value for the first time
          newValue = SymbolicValueFactory.getNewValue();
          pGd.addValue(newValue);
          pGd.putExplicitValue(KnownSymbolicValue.valueOf(newValue), expValue);
          // we add the entry for explicit mapping later
          pTable.addExplicitRelation(newValue, expValue);
          return newValue;
        }
      }
      newValue = SymbolicValueFactory.getNewValue();
      pAbsInfo.addAbstraction(newValue, pG1, v1, pG2, v2);
    }
    pGd.addValue(newValue);
    return newValue;
  }

  /* *********** */
  /* write backs */
  /* *********** */

  public CShapeGraph getShapeGraph1() {
    return inputGraph1;
  }

  public CShapeGraph getShapeGraph2() {
    return inputGraph2;
  }

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

  public boolean isDefined() {
    return isDefined;
  }

  public Long getValue() {
    return merged;
  }

  /* ******** */
  /* updaters */
  /* ******** */

  private void targetJoinUpdater(SGTargetObjectJoin pJoin) {
    inputGraph1 = pJoin.getShapeGraph1();
    inputGraph2 = pJoin.getShapeGraph2();
    destGraph = pJoin.getDestGraph();
    mapping1 = pJoin.getMapping1();
    mapping2 = pJoin.getMapping2();
    absInfo = pJoin.getAbstraction();
  }

}
