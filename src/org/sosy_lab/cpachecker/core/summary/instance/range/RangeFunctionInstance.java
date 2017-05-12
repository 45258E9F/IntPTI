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
package org.sosy_lab.cpachecker.core.summary.instance.range;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.core.summary.apply.AbstractFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.PersistentTreeNode;

public class RangeFunctionInstance extends AbstractFunctionSummaryInstance<RangeState> {

  private PathCopyingPersistentTree<String, Range> globalSummary;
  private PathCopyingPersistentTree<String, Range> returnSummary;

  public RangeFunctionInstance(String pFunctionName) {
    super(pFunctionName);
    globalSummary = PathCopyingPersistentTree.of();
    returnSummary = PathCopyingPersistentTree.of();
  }

  private RangeFunctionInstance(
      String pFunctionName, PathCopyingPersistentTree<String, Range>
      pGlobalSummary, PathCopyingPersistentTree<String, Range> pReturnSummary) {
    super(pFunctionName);
    globalSummary = pGlobalSummary;
    returnSummary = pReturnSummary;
  }

  @Override
  public RangeState apply() {
    RangeState result = new RangeState();
    result.addAllRanges(globalSummary);
    return result;
  }

  @Override
  public boolean isEqualTo(SummaryInstance that) {
    if (that == this) {
      return true;
    }
    if (!(that instanceof RangeFunctionInstance)) {
      return false;
    }
    RangeFunctionInstance other = (RangeFunctionInstance) that;
    return other.function.equals(function) &&
        Objects.equal(globalSummary, other.globalSummary) &&
        Objects.equal(returnSummary, other.returnSummary);
  }

  public PathCopyingPersistentTree<String, Range> getGlobalSummary() {
    return globalSummary;
  }

  public PathCopyingPersistentTree<String, Range> getReturnSummary() {
    return returnSummary;
  }

  public RangeFunctionInstance merge(RangeFunctionInstance other) {
    if (function.equals(other.function)) {
      PathCopyingPersistentTree<String, Range> newGlobalSummary = RangeState.mergeTree
          (globalSummary, other.globalSummary);
      PathCopyingPersistentTree<String, Range> newReturnSummary = RangeState.mergeTree
          (returnSummary, other.returnSummary);
      return new RangeFunctionInstance(function, newGlobalSummary, newReturnSummary);
    } else {
      return this;
    }
  }

  public void addGlobalSummary(String name, PersistentTreeNode<String, Range> node) {
    if (node != null) {
      globalSummary = globalSummary.setSubtreeAndCopy(Lists.newArrayList(name), node);
    }
  }

  public void addReturnSummary(String name, PersistentTreeNode<String, Range> node) {
    if (node != null) {
      returnSummary = returnSummary.setSubtreeAndCopy(Lists.newArrayList(name), node);
    }
  }

  @Override
  public String toString() {
    return "global: [" + globalSummary.toString() + "]\nreturn: [" + returnSummary.toString() +
        "]\n";
  }
}
