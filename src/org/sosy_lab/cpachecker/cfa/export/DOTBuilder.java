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
package org.sosy_lab.cpachecker.cfa.export;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class for generating a DOT file from a CFA.
 */
public final class DOTBuilder {

  private DOTBuilder() { /* utility class */ }

  private static final String MAIN_GRAPH = "____Main____Diagram__";
  private static final Joiner JOINER_ON_NEWLINE = Joiner.on('\n');

  // After this many characters the node shape changes to box.
  private static final int NODE_SHAPE_CHANGE_CHAR_LIMIT = 10;

  private static final Function<CFANode, String> DEFAULT_NODE_FORMATTER =
      new Function<CFANode, String>() {
        @Override
        public String apply(CFANode node) {
          return "N" + node.getNodeNumber() + "\\n" + node.getReversePostorderId() + " (" + node
              .getPostDominatorId() + ")";
        }
      };


  public static void generateDOT(Appendable sb, CFA cfa) throws IOException {
    generateDOT(sb, cfa, DEFAULT_NODE_FORMATTER);
  }

  public static void generateDOT(
      Appendable sb, CFA cfa,
      Function<CFANode, String> formatNodeLabel) throws IOException {
    DotGenerator dotGenerator = new DotGenerator(cfa, formatNodeLabel);
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), dotGenerator);
    // notice that not every function is used, visit all function entries
    for (FunctionEntryNode fnode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().traverseOnce(fnode, dotGenerator);
    }

    sb.append("digraph " + "CFA" + " {\n");

    JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.nodes);
    sb.append('\n');

    // define the graphic representation for all subsequent nodes
    sb.append("node [shape=\"circle\"]\n");

    for (FunctionEntryNode fnode : cfa.getAllFunctionHeads()) {
      // If Array belongs to functionCall in Parameter, replace [].
      // If Name Contains '.' replace with '_'
      sb.append("subgraph cluster_" + fnode.getFunctionName()
          .replace("[", "").replace("]", "_array")
          .replace(".", "_") + " {\n");
      sb.append("label=\"" + fnode.getFunctionName() + "()\"\n");
      JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.edges.get(fnode.getFunctionName()));
      sb.append("}\n");
    }

    JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.edges.get(MAIN_GRAPH));
    sb.append("}");
  }

  /**
   * Visit a CFA and collect node & edges
   * Nodes and edges can be visited multiple times but will be collected only once.
   */
  private static class DotGenerator implements CFATraversal.CFAVisitor {
    private final Set<CFANode> collectedNodes;
    private final Set<CFAEdge> collectedEdges;

    // nodes
    private final List<String> nodes = new ArrayList<>();
    // edges for each function
    private final ListMultimap<String, String> edges = ArrayListMultimap.create();

    private final Optional<ImmutableSet<CFANode>> loopHeads;
    private final Function<CFANode, String> formatNodeLabel;

    public DotGenerator(CFA cfa, Function<CFANode, String> pFormatNodeLabel) {
      collectedNodes = Sets.newHashSet();
      collectedEdges = Sets.newHashSet();
      loopHeads = cfa.getAllLoopHeads();
      formatNodeLabel = pFormatNodeLabel;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      if (collectedEdges.contains(edge)) {
        return CFATraversal.TraversalProcess.SKIP;
      } else {
        collectedEdges.add(edge);
        CFANode predecessor = edge.getPredecessor();
        List<String> graph;
        if ((edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
            || edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          graph = edges.get(MAIN_GRAPH);
        } else {
          graph = edges.get(predecessor.getFunctionName());
        }
        graph.add(formatEdge(edge));
        return CFATraversal.TraversalProcess.CONTINUE;
      }
    }

    @Override
    public TraversalProcess visitNode(CFANode node) {
      if (collectedNodes.contains(node)) {
        return CFATraversal.TraversalProcess.SKIP;
      } else {
        collectedNodes.add(node);
        nodes.add(formatNode(node, loopHeads, formatNodeLabel));
        return CFATraversal.TraversalProcess.CONTINUE;
      }
    }

  }

  static String formatNode(
      CFANode node, Optional<ImmutableSet<CFANode>> loopHeads) {
    return formatNode(node, loopHeads, DEFAULT_NODE_FORMATTER);
  }

  static String formatNode(
      CFANode node, Optional<ImmutableSet<CFANode>> loopHeads,
      Function<CFANode, String> formatNodeLabel) {
    final String shape;

    String nodeAnnotation = formatNodeLabel.apply(node);
    nodeAnnotation = nodeAnnotation != null ? nodeAnnotation : "";

    if (nodeAnnotation.length() > NODE_SHAPE_CHANGE_CHAR_LIMIT) {
      shape = "box";
    } else {
      if (loopHeads.isPresent() && loopHeads.get().contains(node)) {
        shape = "doublecircle";
      } else if (node.isLoopStart()) {
        shape = "doubleoctagon";

      } else if (node.getNumLeavingEdges() > 0
          && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge) {
        shape = "diamond";
      } else {
        shape = "circle";
      }
    }

    return node.getNodeNumber()
        + " [shape=\""
        + shape
        + "\" label=\""
        + escapeGraphvizLabel(nodeAnnotation, "\\\\n") + "\"]";
  }

  static String formatEdge(CFAEdge edge) {
    StringBuilder sb = new StringBuilder();
    sb.append(edge.getPredecessor().getNodeNumber());
    sb.append(" -> ");
    sb.append(edge.getSuccessor().getNodeNumber());
    sb.append(" [label=\"");

    //the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
    //future updates of dotty may make this obsolete.
    sb.append(escapeGraphvizLabel(edge.getDescription(), " "));

    sb.append("\"");
    if (edge instanceof FunctionSummaryEdge) {
      sb.append(" style=\"dotted\" arrowhead=\"empty\"");
    }
    sb.append("]");
    return sb.toString();
  }

  public static String escapeGraphvizLabel(String input, String newlineReplacement) {
    return input.replaceAll("\\Q\\\"\\E", "\\ \"")
        .replaceAll("\\\"", "\\\\\\\"")
        .replaceAll("\n", newlineReplacement);
  }
}
