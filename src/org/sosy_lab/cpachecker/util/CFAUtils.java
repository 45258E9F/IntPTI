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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import com.google.common.collect.UnmodifiableIterator;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

public class CFAUtils {

  /**
   * Return an {@link Iterable} that contains all entering edges of a given CFANode,
   * including the summary edge if the node has one.
   */
  public static FluentIterable<CFAEdge> allEnteringEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge (-1 means the summary edge)
          private int i = (node.getEnteringSummaryEdge() != null) ? -1 : 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumEnteringEdges();
          }

          @Override
          public CFAEdge next() {
            if (i == -1) {
              i = 0;
              return node.getEnteringSummaryEdge();
            }
            return node.getEnteringEdge(i++);
          }
        };
      }
    };
  }

  /**
   * Return an {@link Iterable} that contains the entering edges of a given CFANode,
   * excluding the summary edge.
   */
  public static FluentIterable<CFAEdge> enteringEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge
          private int i = 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumEnteringEdges();
          }

          @Override
          public CFAEdge next() {
            return node.getEnteringEdge(i++);
          }
        };
      }
    };
  }

  /**
   * Return an {@link Iterable} that contains all leaving edges of a given CFANode,
   * including the summary edge if the node as one.
   */
  public static FluentIterable<CFAEdge> allLeavingEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge (-1 means the summary edge)
          private int i = (node.getLeavingSummaryEdge() != null) ? -1 : 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumLeavingEdges();
          }

          @Override
          public CFAEdge next() {
            if (i == -1) {
              i = 0;
              return node.getLeavingSummaryEdge();
            }
            return node.getLeavingEdge(i++);
          }
        };
      }
    };
  }

  /**
   * Return an {@link Iterable} that contains the leaving edges of a given CFANode,
   * excluding the summary edge.
   */
  public static FluentIterable<CFAEdge> leavingEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge
          private int i = 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumLeavingEdges();
          }

          @Override
          public CFAEdge next() {
            return node.getLeavingEdge(i++);
          }
        };
      }
    };
  }

  public static final Function<CFAEdge, CFANode> TO_PREDECESSOR = new Function<CFAEdge, CFANode>() {
    @Override
    public CFANode apply(CFAEdge pInput) {
      return pInput.getPredecessor();
    }
  };


  public static final Function<CFAEdge, CFANode> TO_SUCCESSOR = new Function<CFAEdge, CFANode>() {
    @Override
    public CFANode apply(CFAEdge pInput) {
      return pInput.getSuccessor();
    }
  };

  /**
   * Return an {@link Iterable} that contains the predecessor nodes of a given CFANode,
   * excluding the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> predecessorsOf(final CFANode node) {
    return enteringEdges(node).transform(TO_PREDECESSOR);
  }

  /**
   * Return an {@link Iterable} that contains all the predecessor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allPredecessorsOf(final CFANode node) {
    return allEnteringEdges(node).transform(TO_PREDECESSOR);
  }

  /**
   * Return an {@link Iterable} that contains the successor nodes of a given CFANode,
   * excluding the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> successorsOf(final CFANode node) {
    return leavingEdges(node).transform(TO_SUCCESSOR);
  }

  /**
   * Return an {@link Iterable} that contains all the successor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allSuccessorsOf(final CFANode node) {
    return allLeavingEdges(node).transform(TO_SUCCESSOR);
  }

  public static final Function<CFANode, String> GET_FUNCTION = new Function<CFANode, String>() {
    @Override
    public String apply(CFANode pInput) {
      return pInput.getFunctionName();
    }
  };

  /**
   * A comparator for comparing {@link CFANode}s by their node numbers.
   */
  public static final Comparator<CFANode> NODE_NUMBER_COMPARATOR = new Comparator<CFANode>() {
    @Override
    public int compare(CFANode pO1, CFANode pO2) {
      return Integer.compare(pO1.getNodeNumber(), pO2.getNodeNumber());
    }
  };

  /**
   * Returns a predicate for CFA edges with the given edge type.
   * The predicate is not null safe.
   *
   * @param pType the edge type matched on.
   */
  public static Predicate<CFAEdge> edgeHasType(final CFAEdgeType pType) {
    checkNotNull(pType);
    return new Predicate<CFAEdge>() {

      @Override
      public boolean apply(CFAEdge pInput) {
        return pInput.getEdgeType() == pType;
      }

    };
  }

  /**
   * Returns the other AssumeEdge (with the negated condition)
   * of a given AssumeEdge.
   */
  public static AssumeEdge getComplimentaryAssumeEdge(AssumeEdge edge) {
    checkArgument(edge.getPredecessor().getNumLeavingEdges() == 2);
    return (AssumeEdge) Iterables.getOnlyElement(
        CFAUtils.leavingEdges(edge.getPredecessor())
            .filter(not(Predicates.<CFAEdge>equalTo(edge))));
  }

  /**
   * Checks if a path from the source to the target exists, using the given
   * function to obtain the edges leaving a node.
   *
   * @param pSource           the search start node.
   * @param pTarget           the target.
   * @param pGetLeavingEdges  the function used to obtain leaving edges and thus the successors of a
   *                          node.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   * @return {@code true} if a path from the source to the target exists, {@code false} otherwise.
   * @throws InterruptedException if a shutdown has been requested by the given shutdown notifier.
   */
  public static boolean existsPath(
      CFANode pSource,
      CFANode pTarget, Function<CFANode, Iterable<CFAEdge>> pGetLeavingEdges,
      ShutdownNotifier pShutdownNotifier) throws InterruptedException {
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(pSource);
    while (!waitlist.isEmpty()) {
      pShutdownNotifier.shutdownIfNecessary();
      CFANode current = waitlist.poll();
      if (current.equals(pTarget)) {
        return true;
      }
      if (visited.add(current)) {
        for (CFAEdge leavingEdge : pGetLeavingEdges.apply(current)) {
          CFANode succ = leavingEdge.getSuccessor();
          waitlist.offer(succ);
        }
      }
    }
    return false;
  }


  /**
   * This Visitor searches for backwards edges in the CFA, if some backwards edges
   * were found can be obtained by calling the method hasBackwardsEdges()
   */
  private static class FindBackwardsEdgesVisitor extends DefaultCFAVisitor {

    private boolean hasBackwardsEdges = false;

    @Override
    public TraversalProcess visitNode(CFANode pNode) {

      if (pNode.getNumLeavingEdges() == 0) {
        return TraversalProcess.CONTINUE;
      } else if (pNode.getNumLeavingEdges() == 1
          && pNode.getLeavingEdge(0).getSuccessor().getReversePostorderId() >= pNode
          .getReversePostorderId()) {

        hasBackwardsEdges = true;
        return TraversalProcess.ABORT;
      } else if (pNode.getNumLeavingEdges() == 2
          && (pNode.getLeavingEdge(0).getSuccessor().getReversePostorderId() >= pNode
          .getReversePostorderId() ||
          pNode.getLeavingEdge(1).getSuccessor().getReversePostorderId() >= pNode
              .getReversePostorderId())) {
        hasBackwardsEdges = true;
        return TraversalProcess.ABORT;
      } else if (pNode.getNumLeavingEdges() > 2) {
        throw new AssertionError("forgotten case in traversing cfa with more than 2 leaving edges");
      } else {
        return TraversalProcess.CONTINUE;
      }
    }

    public boolean hasBackwardsEdges() {
      return hasBackwardsEdges;
    }
  }

  /**
   * Searches for backwards edges from a given starting node
   *
   * @param rootNode The node where the search is started
   * @return indicates if a backwards edge was found
   */
  static boolean hasBackWardsEdges(CFANode rootNode) {
    FindBackwardsEdgesVisitor visitor = new FindBackwardsEdgesVisitor();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(rootNode, visitor);

    return visitor.hasBackwardsEdges();
  }

  /**
   * This method allows to select from a set of variables
   * all local variables from a given function.
   * This requires that the given set contains the qualified names of each variable
   * as returned by {@link AbstractSimpleDeclaration#getQualifiedName()}.
   *
   * @param variables Set of qualified names of variables.
   * @param function  A function name.
   * @return A subset of "variables".
   */
  public static SortedSet<String> filterVariablesOfFunction(
      SortedSet<String> variables,
      String function) {
    // TODO: Currently the format of the qualified name is not defined.
    // In theory, frontends could use different formats.
    // The best would be to eliminate all uses of this method
    // (code should not use Strings, but for example AIdExpressions).
    // For now, we just assume all variables are named as
    // {@link org.sosy_lab.cpachecker.cfa.parser.eclipse.c.FunctionScope#createQualifiedName(String, String)}
    // produces them.
    String prefix = checkNotNull(function) + "::";
    return Collections3.subSetWithPrefix(variables, prefix);
  }

  /**
   * Get all (sub)-paths through the given nodes connected only via blank edges.
   *
   * @param pNode the node to get the blank paths for.
   * @return all (sub)-paths through the given nodes connected only via blank edges.
   */
  public static Iterable<List<CFANode>> getBlankPaths(CFANode pNode) {
    List<List<CFANode>> blankPaths = new ArrayList<>();
    Queue<List<CFANode>> waitlist = Queues.newArrayDeque();
    waitlist.offer(ImmutableList.of(pNode));
    while (!waitlist.isEmpty()) {
      List<CFANode> currentPath = waitlist.poll();
      CFANode pathSucc = currentPath.get(currentPath.size() - 1);
      List<BlankEdge> leavingBlankEdges =
          CFAUtils.leavingEdges(pathSucc).filter(BlankEdge.class).toList();
      if (pathSucc.getNumLeavingEdges() <= 0
          || leavingBlankEdges.size() < pathSucc.getNumLeavingEdges()) {
        blankPaths.add(currentPath);
      } else {
        for (CFAEdge leavingEdge : leavingBlankEdges) {
          CFANode successor = leavingEdge.getSuccessor();
          if (!currentPath.contains(successor)) {
            List<CFANode> newPath =
                ImmutableList.<CFANode>builder().addAll(currentPath).add(successor).build();
            waitlist.offer(newPath);
          }
        }
      }
    }
    waitlist.addAll(blankPaths);
    blankPaths.clear();
    while (!waitlist.isEmpty()) {
      List<CFANode> currentPath = waitlist.poll();
      CFANode pathPred = currentPath.get(0);
      List<BlankEdge> enteringBlankEdges =
          CFAUtils.enteringEdges(pathPred).filter(BlankEdge.class).toList();
      if (pathPred.getNumEnteringEdges() <= 0
          || enteringBlankEdges.size() < pathPred.getNumEnteringEdges()) {
        blankPaths.add(currentPath);
      } else {
        for (CFAEdge enteringEdge : enteringBlankEdges) {
          CFANode predecessor = enteringEdge.getPredecessor();
          if (!currentPath.contains(predecessor)) {
            List<CFANode> newPath =
                ImmutableList.<CFANode>builder().add(predecessor).addAll(currentPath).build();
            waitlist.offer(newPath);
          }
        }
      }
    }
    return blankPaths;
  }

  @Nullable
  public static CFAEdge getConnectingEdge(
      CFANode predecessor, CFANode successor, Class<? extends
      CFAEdge> edgeClass) {
    int numOfLeaving = predecessor.getNumLeavingEdges();
    for (int i = 0; i < numOfLeaving; i++) {
      CFAEdge edge = predecessor.getLeavingEdge(i);
      if (Predicates.instanceOf(edgeClass).apply(edge) && edge.getSuccessor() == successor) {
        return edge;
      }
    }
    return null;
  }

}
