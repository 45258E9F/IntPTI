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
package org.sosy_lab.cpachecker.cfa.blocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages a given partition of a program's CFA into a set of blocks.
 */
public class BlockPartitioning {
  private final Block mainBlock;
  private final Map<CFANode, Block> callNodeToBlock;
  private final Map<CFANode, Block> returnNodeToBlock;
  private final Set<Block> blocks;

  public BlockPartitioning(Collection<Block> subtrees, CFANode mainFunction) {
    Block mainBlock = null;
    final ImmutableMap.Builder<CFANode, Block> callNodeToSubtree = new ImmutableMap.Builder<>();
    final ImmutableMap.Builder<CFANode, Block> returnNodeToBlock = new ImmutableMap.Builder<>();
    final ImmutableSet.Builder<Block> blocks = new ImmutableSet.Builder<>();

    // this set is needed for special cases, when different blocks have the same returnNode.
    // TODO can we avoid this?
    final Set<CFANode> returnNodes = new HashSet<>();

    for (Block subtree : subtrees) {
      blocks.add(subtree);
      for (CFANode callNode : subtree.getCallNodes()) {
        if (callNode instanceof FunctionEntryNode &&
            callNode.getFunctionName().equalsIgnoreCase(mainFunction.getFunctionName())) {
          assert mainBlock == null;
          mainBlock = subtree;
        }
        callNodeToSubtree.put(callNode, subtree);
      }

      for (CFANode returnNode : subtree.getReturnNodes()) {
        if (returnNodes.add(returnNode)) {
          returnNodeToBlock.put(returnNode, subtree);
        }
      }
    }

    assert mainBlock != null;
    this.mainBlock = mainBlock;

    this.callNodeToBlock = callNodeToSubtree.build();
    this.returnNodeToBlock = returnNodeToBlock.build();
    this.blocks = blocks.build();
  }

  /**
   * @param node the node to be checked
   * @return true, if there is a <code>Block</code> such that <code>node</node> is a callnode of the
   * subtree.
   */
  public boolean isCallNode(CFANode node) {
    return callNodeToBlock.containsKey(node);
  }

  /**
   * Requires <code>isCallNode(node)</code> to be <code>true</code>.
   *
   * @param node call node of some cached subtree
   * @return Block for given call node
   */
  public Block getBlockForCallNode(CFANode node) {
    return callNodeToBlock.get(node);
  }

  public Block getMainBlock() {
    return mainBlock;
  }

  public boolean isReturnNode(CFANode node) {
    return returnNodeToBlock.containsKey(node);
  }

  public Block getBlockForReturnNode(CFANode pCurrentNode) {
    return returnNodeToBlock.get(pCurrentNode);
  }

  public Set<Block> getBlocks() {
    return blocks;
  }

}
