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

import com.google.common.base.Preconditions;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;

import java.util.Set;


/**
 * <code>PartitioningHeuristic</code> that creates a block for each function-body.
 */
public class FunctionPartitioning extends PartitioningHeuristic {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION =
      CFATraversal.dfs().ignoreFunctionCalls();

  /**
   * Do not change signature! Constructor will be created with Reflections.
   */
  public FunctionPartitioning(LogManager pLogger, CFA pCfa) {
    super(pLogger, pCfa);
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    if (pNode.getFunctionName().startsWith("__VERIFIER_")) {
      // exception for __VERIFIER helper functions
      // TODO do we need this? why?
      return false;
    }
    return pNode instanceof FunctionEntryNode;
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pNode) {
    Preconditions.checkArgument(shouldBeCached(pNode));
    Set<CFANode> blockNodes = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(pNode);
    return blockNodes;
  }
}
