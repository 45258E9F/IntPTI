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
package org.sosy_lab.cpachecker.cpa.range.util;

import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.TypeRangeVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

import java.util.List;

public final class AccessSummaryApplicator {

  private final int maxTrackedArrayElements;
  private final MachineModel machineModel;

  private static AccessSummaryApplicator instance;

  private AccessSummaryApplicator(int pMaxTrackedArrayElements, MachineModel pMachineModel) {
    maxTrackedArrayElements = pMaxTrackedArrayElements;
    machineModel = pMachineModel;
  }

  public static void initialize(int pMaxTrackedArrayElements, MachineModel pMachineModel) {
    instance = new AccessSummaryApplicator(pMaxTrackedArrayElements, pMachineModel);
  }

  public static AccessSummaryApplicator getInstance() {
    if(instance == null) {
      throw new IllegalArgumentException("Access summary applicator should be initialized first");
    }
    return instance;
  }

  public RangeState applySummary(RangeState pState, AccessPath pWritePath)
      throws UnrecognizedCCodeException {
    RangeState newState = RangeState.copyOf(pState);
    List<CType> typeList = pWritePath.parseTypeList();
    assert (!typeList.isEmpty());
    CType lastType = typeList.get(typeList.size() - 1);
    // even if the declaration is global, we still set the zero-initialize flag as FALSE
    TypeRangeVisitor typeRangeVisitor = new TypeRangeVisitor(pWritePath, maxTrackedArrayElements,
        machineModel, false);
    PathCopyingPersistentTree<String, Range> typeRangeTree = lastType.accept(typeRangeVisitor);
    if (typeRangeTree.isEmpty()) {
      newState.addRange(Lists.newArrayList(pWritePath.getQualifiedName()), Range.UNBOUND, false);
    } else {
      newState.addAllRanges(typeRangeTree);
    }
    return newState;
  }

}
