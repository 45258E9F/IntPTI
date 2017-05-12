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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * This Writer can dump a cfa with blocks into a file.
 */
public class BlockToDotWriter {

  private final BlockPartitioning blockPartitioning;
  int blockIndex = 0;

  public BlockToDotWriter(BlockPartitioning blockPartitioning) {
    this.blockPartitioning = blockPartitioning;
  }

  /**
   * dump the cfa with blocks and colourful nodes.
   */
  public void dump(final Path filename, final LogManager logger) {
    try (Writer w = Files.openOutputFile(filename)) {
      dump(w);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write blocks to dot file");
      // ignore exception and continue analysis
    }
  }

  /**
   * dump the cfa with blocks and colourful nodes.
   */
  private void dump(final Appendable app) throws IOException {

    // get hierarchy, Multimap of <outer block, inner blocks>
    final Multimap<Block, Block> hierarchy = getHierarchy();

    app.append("digraph " + "blocked_CFA" + " {\n");
    final List<CFAEdge> edges = new ArrayList<>();

    // dump nodes of all blocks
    dumpBlock(app, new HashSet<CFANode>(), blockPartitioning.getMainBlock(), hierarchy, edges, 0);

    // we have to dump edges after the nodes and sub-graphs,
    // because Dot generates wrong graphs for edges from an inner block to an outer block.
    for (CFAEdge edge : edges) {
      app.append(formatEdge(edge));
    }

    app.append("}");
  }

  /**
   * This function returns a structure which contains hierarchical dependencies between blocks.
   * The returned Multimap contains the outer block (father) as key
   * and the inner blocks (children) as values for the key.
   * We assume, that for each pair of blocks the following conditions hold:
   * a block is either completely part of the other block or there is nothing common in both blocks.
   */
  private Multimap<Block, Block> getHierarchy() {

    // sort blocks, largest blocks first
    List<Block> sortedBlocks = Lists.newArrayList(blockPartitioning.getBlocks());
    Collections.sort(sortedBlocks, new Comparator<Block>() {
      @Override
      public int compare(Block b1, Block b2) {
        return b2.getNodes().size() - b1.getNodes().size();
      }
    });

    // build hierarchy, worst case runtime O(n^2), iff mainBlock contains all other blocks 'directly'.
    final Multimap<Block, Block> hierarchy = HashMultimap.create();
    while (!sortedBlocks.isEmpty()) {
      // get smallest block and then the smallest outer block, that contains it
      Block currentBlock = sortedBlocks.remove(sortedBlocks.size() - 1); // get smallest block,
      for (Block possibleOuterBlock : Lists
          .reverse(sortedBlocks)) { // order is important, smallest first
        // trick: we know, iff one node is contained in outer block, all nodes must be contained. So we check only one.
        if (possibleOuterBlock.getNodes().contains(currentBlock.getNodes().iterator().next())) {
          hierarchy.put(possibleOuterBlock, currentBlock);
          break;
        }
      }
    }

    assert hierarchy.values().size() <= blockPartitioning.getBlocks().size() - 1 :
        "all blocks except mainBlock might appear at most once as child.";
    // there might also be blocks, that are not part of the hierarchy, for example unused functions.

    return hierarchy;
  }

  /**
   * Dump the current block and all innerblocks of it.
   */
  private void dumpBlock(
      final Appendable app, final Set<CFANode> finished,
      final Block block, final Multimap<Block, Block> hierarchy,
      final List<CFAEdge> edges, final int depth) throws IOException {
    // todo use some block-identifier instead of index as blockname?
    final String blockname =
        (block == blockPartitioning.getMainBlock()) ? "main_block" : "block_" + (blockIndex++);
    app.append("subgraph cluster_" + blockname + " {\n");
    app.append("style=filled\n");
    app.append("fillcolor=" + (depth % 2 == 0 ? "white" : "lightgrey") + "\n");
    app.append("label=\"" + blockname + "\"\n");

    // dump inner blocks
    for (Block innerBlock : hierarchy.get(block)) {
      dumpBlock(app, finished, innerBlock, hierarchy, edges, depth + 1);
    }

    // dump nodes,that are in current block and not in inner blocks (nodes of inner blocks are 'finished')
    for (CFANode node : block.getNodes()) {
      if (finished.add(node)) {
        app.append(formatNode(node));
        Iterables.addAll(edges, CFAUtils.leavingEdges(node));
        FunctionSummaryEdge func = node.getEnteringSummaryEdge();
        if (func != null) {
          edges.add(func);
        }
      }
    }

    app.append("}\n");
  }

  private String formatNode(CFANode node) {
    String color = "";
    if (blockPartitioning.isCallNode(node) && blockPartitioning.isReturnNode(node)) {
      // when does this happen? block.size == 1?
      color = "style=filled penwidth=6 fillcolor=green color=red ";
    } else if (blockPartitioning.isCallNode(node)) {
      color = "style=filled fillcolor=green ";
    } else if (blockPartitioning.isReturnNode(node)) {
      color = "style=filled fillcolor=red ";
    }

    String shape = "";
    if (node.isLoopStart()) {
      shape = "shape=doubleoctagon ";
    } else if (node.getNumLeavingEdges() > 0
        && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge) {
      shape = "shape=diamond ";
    }

    String label = "label=\"" + node.getNodeNumber() + "\\n" + node.getReversePostorderId() + "\" ";
    return node.getNodeNumber() + " [" + shape + label + color + "]\n";
  }

  /**
   * method copied from <ode>org.sosy_lab.cpachecker.cfa.export.DOTBuilder.DotGenerator#formatEdge</code>
   */
  private static String formatEdge(CFAEdge edge) {
    StringBuilder sb = new StringBuilder();
    sb.append(edge.getPredecessor().getNodeNumber());
    sb.append(" -> ");
    sb.append(edge.getSuccessor().getNodeNumber());
    sb.append(" [label=\"");

    //the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
    //future updates of dotty may make this obsolete.
    sb.append(edge.getDescription()
        .replaceAll("\\Q\\\"\\E", "\\ \"")
        .replaceAll("\\\"", "\\\\\\\"")
        .replaceAll("\n", " "));

    sb.append("\"");
    if (edge instanceof FunctionSummaryEdge) {
      sb.append(" style=\"dotted\" arrowhead=\"empty\"");
    } else if (edge instanceof FunctionCallEdge) {
      sb.append(" style=\"dashed\" arrowhead=\"empty\"");
    } else if (edge instanceof FunctionReturnEdge) {
      sb.append(" style=\"dashed\" arrowhead=\"empty\"");
    }
    sb.append("]\n");
    return sb.toString();
  }
}
