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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.ForwardingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Essentially perform the following:
 * 1. compute the post dominator relation of a function
 * 2. build the post dominator tree
 * 3. topological sort tree nodes
 *
 * Precondition: reverse post order is set
 *
 * For functions without exit node, use the reverse post order id.
 */
public class CFAPostDominatorTree {
  private static class StoppingNodeCollector extends ForwardingCFAVisitor {
    private final Set<CFANode> stopNodes;

    public StoppingNodeCollector(CFANode... stop) {
      super(new DefaultCFAVisitor());
      stopNodes = Sets.newHashSet(stop);
    }

    private final Set<CFANode> visitedNodes = new HashSet<>();

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      if (visitedNodes.add(pNode) && !stopNodes.contains(pNode)) {
        return super.visitNode(pNode);
      }

      return TraversalProcess.SKIP;
    }

    public Set<CFANode> getVisitedNodes() {
      return visitedNodes;
    }
  }

  public void assignSorting(CFANode start) {
    if (start instanceof FunctionEntryNode) {
      CFANode exit = ((FunctionEntryNode) start).getExitNode();
      // if the exit node is reachable from start
      if (CFATraversal.dfs().collectNodesReachableFrom(start).contains(exit)) {
        Pair<CFANode, List<CFANode>> pair = ignoreHeadingLinear((FunctionEntryNode) start);
        sortAndAssign(((FunctionEntryNode) start).getExitNode(), pair.getFirst(), pair.getSecond());
      } else {
        // use the reverse post order id
        CFATraversal.dfs().traverseOnce(start, new CFAVisitor() {
          @Override
          public TraversalProcess visitEdge(CFAEdge pEdge) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitNode(CFANode pNode) {
            pNode.setPostDominatorId(pNode.getReversePostorderId());
            return TraversalProcess.CONTINUE;
          }
        });
      }
    } else {
      throw new RuntimeException(
          "invalid invocation to CFAPostDominatorTree.assignSorting: always give the function entry node");
    }
  }

  private void sortAndAssign(FunctionExitNode exit, CFANode start, List<CFANode> ignored) {
    // collect all nodes
    Set<CFANode> allNodes = CFATraversal.dfs().collectNodesReachableFrom(start);
    // maps node to the nodes post dominate it, dominate(x) = {y | y post-dominates x}
    Multimap<CFANode, CFANode> dominator = HashMultimap.create();
    // compute
    for (CFANode w : allNodes) {
      // try to remove w from the graph, see what nodes are still backward reachable?
      // note that w is always in the returned set
      StoppingNodeCollector visitor =
          new StoppingNodeCollector(start, w);  // stop at 'start' and 'w'
      CFATraversal.dfs().backwards().traverseOnce(exit, visitor);
      Set<CFANode> reachable = visitor.getVisitedNodes();
      Set<CFANode> dominatee = Sets.difference(allNodes, reachable);
      for (CFANode e : dominatee) {
        dominator.put(e, w);    // w post dominates e
      }
    }
    // final order
    List<CFANode> ordered = Lists.newArrayList();
    // decide the full order
    while (!allNodes.isEmpty()) {
      // pick any node that no other node dominates it
      // TODO: due to unknown reasons it happens that nodes cyclically dominates each other
      // in this case, for termination of the algorithm, we select nodes with smallest number of dominators
      int minSize = Integer.MAX_VALUE;
      Set<CFANode> leastDominated = Sets.newHashSet();
      for (CFANode n : allNodes) {
        int size = dominator.get(n).size();
        if (size < minSize) {
          minSize = size;
          leastDominated = Sets.newHashSet(n);
        } else if (size == minSize) {
          leastDominated.add(n);
        }
      }
      // remove n from other nodes' dominator
      for (CFANode n : leastDominated) {
        for (CFANode v : allNodes) {
          dominator.remove(v, n);
        }
      }
      ordered.addAll(leastDominated);
      allNodes.removeAll(leastDominated);
    }
    // don't forget the ignored heading linear line of nodes
    ordered.addAll(Lists.reverse(ignored));
    // labeling
    int id = 0;
    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).setPostDominatorId(id++);
    }
  }

  /**
   * ignore the heading linear line of nodes
   *
   * @param entry entry node of a function
   * @return pair of (s, list) where: (1) the first node of list is 'entry' (2) the last node of
   * list is the only predecessor 's' (3) 's' is the new start
   */
  private Pair<CFANode, List<CFANode>> ignoreHeadingLinear(FunctionEntryNode entry) {
    // Optimization:
    // There may be a very large linear line of nodes at the very beginning of 'main' function
    // which results in out of memory when allocating matrix of size x size in 'identifyLoop'
    // Technically, we solve this problem by ignoring the linear line of nodes at the very
    // beginning that must not be any part of loop.
    // search from the function entry node
    CFANode head = entry;
    List<CFANode> skipped = Lists.newArrayList();
    do {
      // multiple successors, stop
      if (head.getNumLeavingEdges() != 1) {
        break;
      }
      CFAEdge edge = head.getLeavingEdge(0);
      // reaching the real start, stop
      if (edge.getEdgeType() == CFAEdgeType.BlankEdge && edge.getDescription()
          .equals("Function start dummy edge")) {
        break;
      }
      // not blank edge, declaration edge, stop
      if (!(edge.getEdgeType() == CFAEdgeType.BlankEdge ||
          edge.getEdgeType() == CFAEdgeType.DeclarationEdge)) {
        break;
      }
      CFANode next = edge.getSuccessor();
      // the only successor has multiple entering edge or leaving edge, stop
      // actually, we don't have to restrict that 'next' has unique leaving edge
      // however, we put the condition here in order to avoid that the loop identifying algorithm
      // start from a decision node, which we are not 100 percent sure OK.
      if (next.getNumEnteringEdges() != 1 || next.getNumLeavingEdges() != 1) {
        break;
      }
      skipped.add(head);
      head = next;
    } while (true);
    return Pair.of(head, skipped);
  }
}
