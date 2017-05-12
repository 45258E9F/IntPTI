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
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

import java.util.Set;


/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body.
 */
public class FunctionAndLoopPartitioning extends PartitioningHeuristic {

  private FunctionPartitioning functionPartitioning;
  private LoopPartitioning loopPartitioning;

  public FunctionAndLoopPartitioning(LogManager pLogger, CFA pCfa) {
    super(pLogger, pCfa);
    functionPartitioning = new FunctionPartitioning(pLogger, pCfa);
    loopPartitioning = new LoopPartitioning(pLogger, pCfa);
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    return functionPartitioning.shouldBeCached(pNode) || loopPartitioning.shouldBeCached(pNode);
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pNode) {
    // TODO what to do if both want to cache it?
    if (functionPartitioning.shouldBeCached(pNode)) {
      return functionPartitioning.getBlockForNode(pNode);
    } else if (loopPartitioning.shouldBeCached(pNode)) {
      return loopPartitioning.getBlockForNode(pNode);
    } else {
      throw new AssertionError("node should not be cached: " + pNode);
    }
  }
}
