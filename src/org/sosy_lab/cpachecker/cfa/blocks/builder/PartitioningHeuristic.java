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
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;


/**
 * Defines an interface for heuristics for the partition of a program's CFA into blocks.
 */
public abstract class PartitioningHeuristic {

  protected final CFA cfa;
  protected final LogManager logger;

  /**
   * Do not change signature! Constructor will be created with Reflections.
   * Subclasses should also implement the same signature.
   */
  public PartitioningHeuristic(LogManager pLogger, CFA pCfa) {
    cfa = pCfa;
    logger = pLogger;
  }

  /**
   * Creates a <code>BlockPartitioning</code> using the represented heuristic.
   *
   * @param mainFunction CFANode at which the main-function is defined
   * @return BlockPartitioning
   * @see org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning
   */
  public final BlockPartitioning buildPartitioning(
      CFANode mainFunction,
      BlockPartitioningBuilder builder) {

    //traverse CFG
    Set<CFANode> seen = new HashSet<>();
    Deque<CFANode> stack = new ArrayDeque<>();

    seen.add(mainFunction);
    stack.push(mainFunction);

    while (!stack.isEmpty()) {
      CFANode node = stack.pop();

      if (shouldBeCached(node)) {
        Set<CFANode> subtree = getBlockForNode(node);
        if (subtree != null) {
          builder.addBlock(subtree, mainFunction);
        }
      }

      for (CFANode nextNode : CFAUtils.successorsOf(node)) {
        if (!seen.contains(nextNode)) {
          stack.push(nextNode);
          seen.add(nextNode);
        }
      }
    }

    return builder.build(mainFunction);
  }

  /**
   * @param pNode the node to be checked
   * @return <code>true</code>, if for the given node a new <code>Block</code> should be created;
   * <code>false</code> otherwise
   */
  protected abstract boolean shouldBeCached(CFANode pNode);

  /**
   * @param pNode CFANode that should be cached. We assume {@link #shouldBeCached(CFANode)} for the
   *              node.
   * @return set of nodes that represent a <code>Block</code>.
   */
  protected abstract Set<CFANode> getBlockForNode(CFANode pNode);
}
