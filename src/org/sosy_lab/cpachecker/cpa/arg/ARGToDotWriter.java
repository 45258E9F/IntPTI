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
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ARGToDotWriter {

  private final Appendable sb;

  ARGToDotWriter(Appendable pSb) throws IOException {
    sb = pSb;

    sb.append("digraph ARG {\n");
    // default style for nodes
    sb.append("node [style=\"filled\" shape=\"box\" color=\"white\"]\n");
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   *
   * @param sb                Where to write the ARG into.
   * @param rootState         the root element of the ARG
   * @param successorFunction A function giving all successors of an ARGState. Only states reachable
   *                          from root by iteratively applying this function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed. States
   *                          which are only reachable via non-displayed states are ignored, too.
   * @param highlightEdge     Which edges to highlight in the graph?
   */
  public static void write(
      Appendable sb,
      final ARGState rootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> highlightEdge)
      throws IOException {

    ARGToDotWriter toDotWriter = new ARGToDotWriter(sb);
    toDotWriter.writeSubgraph(rootState,
        successorFunction,
        displayedElements,
        highlightEdge);
    toDotWriter.finish();
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   *
   * @param sb                Where to write the ARG into.
   * @param rootStates        the root elements of the ARGs
   * @param connections       start- and end-points of edges between separate graphs
   * @param successorFunction A function giving all successors of an ARGState. Only states reachable
   *                          from root by iteratively applying this function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed. States
   *                          which are only reachable via non-displayed states are ignored, too.
   * @param highlightEdge     Which edges to highlight in the graph?
   */
  public static void write(
      final Appendable sb,
      final Set<ARGState> rootStates,
      final Multimap<ARGState, ARGState> connections,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> highlightEdge)
      throws IOException {

    ARGToDotWriter toDotWriter = new ARGToDotWriter(sb);
    for (ARGState rootState : rootStates) {
      toDotWriter.enterSubgraph("cluster_" + rootState.getStateId(),
          "reachedset_" + rootState.getStateId());
      toDotWriter.writeSubgraph(rootState,
          successorFunction,
          displayedElements,
          highlightEdge);
      toDotWriter.leaveSubgraph();
    }

    for (Map.Entry<ARGState, ARGState> connection : connections.entries()) {
      sb.append(connection.getKey().getStateId() + " -> " + connection.getValue().getStateId());
      sb.append(" [color=green style=bold]\n");
    }

    toDotWriter.finish();
  }

  /**
   * Create String with ARG in the DOT format of Graphviz.
   * Only the states and edges are written, no surrounding graph definition.
   *
   * @param rootState         the root element of the ARG
   * @param successorFunction A function giving all successors of an ARGState. Only states reachable
   *                          from root by iteratively applying this function will be dumped.
   * @param displayedElements A predicate for selecting states that should be displayed. States
   *                          which are only reachable via non-displayed states are ignored, too.
   * @param highlightEdge     Which edges to highlight in the graph?
   */
  void writeSubgraph(
      final ARGState rootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> highlightEdge) throws IOException {

    Deque<ARGState> worklist = new ArrayDeque<>();
    Set<ARGState> processed = new HashSet<>();
    StringBuilder edges = new StringBuilder();

    worklist.add(rootState);

    while (!worklist.isEmpty()) {
      ARGState currentElement = worklist.removeLast();
      if (!displayedElements.apply(currentElement)) {
        continue;
      }
      if (!processed.add(currentElement)) {
        continue;
      }

      sb.append(determineNode(currentElement));
      sb.append(determineStateHint(currentElement));

      for (ARGState covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getStateId());
        edges.append(" -> ");
        edges.append(currentElement.getStateId());
        edges.append(" [style=\"dashed\" weight=\"0\" label=\"covered by\"]\n");
      }

      for (ARGState child : successorFunction.apply(currentElement)) {
        edges.append(determineEdge(highlightEdge, currentElement, child));
        worklist.add(child);
      }
    }
    sb.append(edges);
  }

  private static String determineEdge(
      final Predicate<? super Pair<ARGState, ARGState>> highlightEdge,
      final ARGState state, final ARGState succesorState) {
    final StringBuilder builder = new StringBuilder();
    builder.append(state.getStateId()).append(" -> ").append(succesorState.getStateId());
    builder.append(" [");

    if (state.getChildren().contains(succesorState)) {
      final CFAEdge edge = state.getEdgeToChild(succesorState);

      if (edge == null) {
        // there is no direct edge between the nodes, use a dummy-edge
        builder.append("style=\"bold\" color=\"blue\" label=\"dummy edge\"");
      } else {
        // edge exists, use info from edge
        boolean colored = highlightEdge.apply(Pair.of(state, succesorState));
        if (colored) {
          builder.append("color=\"red\" ");
        }
        builder.append("label=\"");
        builder.append("Line ");
        builder.append(edge.getLineNumber());
        builder.append(": ");
        builder.append(edge.getDescription().replaceAll("\n", " ").replace('"', '\''));
        builder.append("\"");
      }
      builder.append(" id=\"");
      builder.append(state.getStateId());
      builder.append(" -> ");
      builder.append(succesorState.getStateId());
      builder.append("\"");
    }

    builder.append("]\n");
    return builder.toString();
  }

  void writeEdge(ARGState start, ARGState end) throws IOException {
    sb.append("" + start.getStateId());
    sb.append(" -> ");
    sb.append("" + end.getStateId());
    sb.append("\n");
  }

  void enterSubgraph(String name, String label) throws IOException {
    sb.append("subgraph ");
    sb.append(name);
    sb.append(" {\n");

    sb.append("label=\"");
    sb.append(label);
    sb.append("\"\n");
  }

  void leaveSubgraph() throws IOException {
    sb.append("}\n");
  }

  void finish() throws IOException {
    sb.append("}\n");
  }

  private static String escapeLabelString(final String rawString) {
    return rawString;
  }

  private static String determineStateHint(final ARGState currentElement) {

    final String stateNodeId = Integer.toString(currentElement.getStateId());
    final String hintNodeId = stateNodeId + "hint";

    String hintLabel = "";
//    PredicateAbstractState abstraction = AbstractStates.extractStateByType(currentElement, PredicateAbstractState.class);
//    if (abstraction != null && abstraction.isAbstractionState()) {
//      final StringBuilder labelBuilder = new StringBuilder();
//      labelBuilder.append(abstraction.getAbstractionFormula().asFormula().toString());
//      hintLabel = labelBuilder.toString();
//    }

    final StringBuilder builder = new StringBuilder();

    if (!hintLabel.isEmpty()) {
      builder.append(" {");
      builder.append(" rank=same;\n");

      builder.append(" ");
      builder.append(stateNodeId);
      builder.append(";\n");

      builder.append(" \"");
      builder.append(hintNodeId);
      builder.append("\" [label=\"");
      builder.append(escapeLabelString(hintLabel));
      builder.append("\", shape=box, style=filled, fillcolor=gray];\n");

      builder.append(" ");
      builder.append(stateNodeId);
      builder.append(" -> ");
      builder.append("\"");
      builder.append(hintNodeId);
      builder.append("\"");
      builder.append(" [arrowhead=none, color=gray, style=solid]");
      builder.append(";\n");

      builder.append(" }\n");
    }

    return builder.toString();
  }

  private static String determineNode(final ARGState currentElement) {
    final StringBuilder builder = new StringBuilder();
    builder.append(currentElement.getStateId());
    builder.append(" [");
    final String color = determineColor(currentElement);
    if (color != null) {
      builder.append("fillcolor=\"").append(color).append("\" ");
    }
    builder.append("label=\"").append(determineLabel(currentElement)).append("\" ");
    builder.append("id=\"").append(currentElement.getStateId()).append("\"]\n");
    return builder.toString();
  }

  private static String determineLabel(ARGState currentElement) {
    StringBuilder builder = new StringBuilder();

    builder.append(currentElement.getStateId());

    Iterable<CFANode> locs = AbstractStates.extractLocations(currentElement);
    if (locs != null) {
      for (CFANode loc : AbstractStates.extractLocations(currentElement)) {
        builder.append(" @ ");
        builder.append(loc.toString());
        builder.append("\\n");
        builder.append(loc.getFunctionName());
        if (loc instanceof FunctionEntryNode) {
          builder.append(" entry");
        } else if (loc instanceof FunctionExitNode) {
          builder.append(" exit");
        }
        builder.append("\\n");
      }
    } else {
      builder.append("\\n");
    }

    builder.append(
        DOTBuilder.escapeGraphvizLabel(currentElement.toDOTLabel(), "\\\\n"));

    return builder.toString().trim();
  }

  private static String determineColor(ARGState currentElement) {
    if (currentElement.isCovered()) {
      return "green";
    }
    if (currentElement.isTarget()) {
      return "red";
    }

    if (!currentElement.wasExpanded()) {
      return "orange";
    }

    if (currentElement.shouldBeHighlighted()) {
      return "cornflowerblue";
    }

    return null;
  }
}