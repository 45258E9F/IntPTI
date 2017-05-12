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
package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class which tries to find all sequences of simple edges in the CFA and
 * replaces them by {@link MultiEdge}s.
 */
public class MultiEdgeCreator extends DefaultCFAVisitor {

  public static void createMultiEdges(MutableCFA cfa) {
    final MultiEdgeCreator visitor = new MultiEdgeCreator(cfa);
    for (final CFANode functionStart : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(functionStart, visitor);
    }
  }

  private final MutableCFA cfa;

  private MultiEdgeCreator(MutableCFA pCfa) {
    cfa = pCfa;
  }

  @Override
  public TraversalProcess visitNode(final CFANode pNode) {

    if (nodeQualifiesAsStartNode(pNode)) {
      List<CFAEdge> edges = new ArrayList<>();
      Set<CFANode> nodes = new HashSet<>();

      CFANode node = pNode;
      do {
        CFAEdge edge = node.getLeavingEdge(0);

        if (!edgeQualifies(edge)) {
          break;
        }

        edges.add(edge);

        nodes.add(edge.getPredecessor());
        nodes.add(edge.getSuccessor());

        node = edge.getSuccessor();
      } while (nodeQualifies(node));

      if (edges.size() > 1) {
        CFAEdge firstEdge = edges.get(0);
        CFANode firstNode = firstEdge.getPredecessor();
        assert firstNode == pNode;
        CFAEdge lastEdge = edges.get(edges.size() - 1);
        CFANode lastNode = lastEdge.getSuccessor();

        // remove old edges
        firstNode.removeLeavingEdge(firstEdge);
        lastNode.removeEnteringEdge(lastEdge);

        // add new edges
        MultiEdge newEdge = new MultiEdge(firstNode, lastNode, edges);
        firstNode.addLeavingEdge(newEdge);
        lastNode.addEnteringEdge(newEdge);

        // remove now unreachable nodes
        nodes.remove(firstNode);
        nodes.remove(lastNode);
        assert !nodes.isEmpty();
        for (CFANode middleNode : nodes) {
          cfa.removeNode(middleNode);
        }
      }
    }

    return TraversalProcess.CONTINUE;
  }

  private boolean nodeQualifiesAsStartNode(CFANode node) {
    return node.getNumLeavingEdges() == 1         // linear chain of edges
        && node.getLeavingSummaryEdge() == null   // without a functioncall
        && node.getNumEnteringEdges() > 0;        // without a functionstart
  }

  private boolean nodeQualifies(CFANode node) {
    return node.getNumLeavingEdges() == 1
        && node.getNumEnteringEdges() == 1
        && node.getLeavingSummaryEdge() == null
        && !node.isLoopStart()
        && node.getClass() == CFANode.class;
  }

  private boolean edgeQualifies(CFAEdge edge) {
    boolean result = edge.getEdgeType() == CFAEdgeType.BlankEdge
        || edge.getEdgeType() == CFAEdgeType.DeclarationEdge
        || edge.getEdgeType() == CFAEdgeType.StatementEdge
        || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge;

    return result && !containsFunctionCall(edge);
  }

  /**
   * This method checks, if the given (statement) edge contains a function call
   * directly or via a function pointer.
   *
   * @param edge the edge to inspect
   * @return whether or not this edge contains a function call or not.
   */
  private boolean containsFunctionCall(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) edge;

/* Temporarily disabled because SV-COMP specification relies on matching
 * extern function calls, and target states cannot appear within a multi edge.
      if ((statementEdge.getStatement() instanceof CFunctionCall)) {
        CFunctionCall call = ((CFunctionCall)statementEdge.getStatement());
        CSimpleDeclaration declaration = call.getFunctionCallExpression().getDeclaration();

        // declaration == null -> functionPointer
        // functionName exists in CFA -> functioncall with CFA for called function
        // otherwise: call of non-existent function, example: nondet_int() -> ignore this case
        return declaration == null || cfa.getAllFunctionNames().contains(declaration.getQualifiedName());
      }
*/
      return (statementEdge.getStatement() instanceof CFunctionCall);
    }
    return false;
  }
}
