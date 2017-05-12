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
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Helper class can build a <code>BlockPartitioning</code> from a partition of a program's CFA into
 * blocks.
 */
public class BlockPartitioningBuilder {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION =
      CFATraversal.dfs().ignoreFunctionCalls();

  protected final Map<CFANode, Set<ReferencedVariable>> referencedVariablesMap = new HashMap<>();
  protected final Map<CFANode, Set<CFANode>> callNodesMap = new HashMap<>();
  protected final Map<CFANode, Set<CFANode>> returnNodesMap = new HashMap<>();
  protected final Map<CFANode, Set<FunctionEntryNode>> innerFunctionCallsMap = new HashMap<>();
  protected final Map<CFANode, Set<CFANode>> blockNodesMap = new HashMap<>();

  public BlockPartitioningBuilder() {
  }

  public BlockPartitioning build(CFANode mainFunction) {
    //fixpoint iteration to take inner function calls into account for referencedVariables and callNodesMap
    boolean changed = true;
    outer:
    while (changed) {
      changed = false;
      for (Entry<CFANode, Set<ReferencedVariable>> entry : referencedVariablesMap.entrySet()) {
        CFANode node = entry.getKey();
        for (CFANode calledFun : innerFunctionCallsMap.get(node)) {
          Set<ReferencedVariable> functionVars = referencedVariablesMap.get(calledFun);
          Set<CFANode> functionBody = blockNodesMap.get(calledFun);
          if (functionVars == null || functionBody == null) {
            assert functionVars == null && functionBody == null;
            //compute it only the fly
            functionBody = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(calledFun);
            functionVars = collectReferencedVariables(functionBody);
            //and save it
            blockNodesMap.put(calledFun, functionBody);
            referencedVariablesMap.put(calledFun, functionVars);
            innerFunctionCallsMap.put(calledFun, collectInnerFunctionCalls(functionBody));
            changed = true;
            continue outer;
          }

          if (entry.getValue().addAll(functionVars)) {
            changed = true;
          }
          if (blockNodesMap.get(node).addAll(functionBody)) {
            changed = true;
          }
        }
      }
    }

    //now we can create the Blocks   for the BlockPartitioning
    Collection<Block> blocks = new ArrayList<>(returnNodesMap.keySet().size());
    for (Entry<CFANode, Set<CFANode>> entry : returnNodesMap.entrySet()) {
      CFANode key = entry.getKey();
      blocks.add(new Block(referencedVariablesMap.get(key), callNodesMap.get(key), entry.getValue(),
          blockNodesMap.get(key)));
    }
    return new BlockPartitioning(blocks, mainFunction);
  }

  /**
   * @param nodes Nodes from which Block should be created; if the set of nodes contains inner
   *              function calls, the called function body should NOT be included
   */

  public void addBlock(Set<CFANode> nodes, CFANode mainFunction) {
    Set<ReferencedVariable> referencedVariables = collectReferencedVariables(nodes);
    Set<CFANode> callNodes = collectCallNodes(nodes, mainFunction);
    Set<CFANode> returnNodes = collectReturnNodes(nodes, mainFunction);
    Set<FunctionEntryNode> innerFunctionCalls = collectInnerFunctionCalls(nodes);

    if (callNodes.isEmpty()) {
     /* What shall we do with function, which is not called from anywhere?
      * There are problems with them at partitioning building stage
      */
      return;
    }

    CFANode registerNode = null;
    for (CFANode node : callNodes) {
      registerNode = node;
      if (node instanceof FunctionEntryNode) {
        break;
      }
    }

    referencedVariablesMap.put(registerNode, referencedVariables);
    callNodesMap.put(registerNode, callNodes);
    returnNodesMap.put(registerNode, returnNodes);
    innerFunctionCallsMap.put(registerNode, innerFunctionCalls);
    blockNodesMap.put(registerNode, nodes);
  }

  private Set<FunctionEntryNode> collectInnerFunctionCalls(Set<CFANode> pNodes) {
    Set<FunctionEntryNode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      for (CFAEdge e : CFAUtils.leavingEdges(node).filter(CFunctionCallEdge.class)) {
        result.add(((CFunctionCallEdge) e).getSuccessor());
      }
    }
    return result;
  }

  private Set<CFANode> collectCallNodes(Set<CFANode> pNodes, CFANode mainFunction) {
    Set<CFANode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      if (node instanceof FunctionEntryNode &&
          node.getFunctionName().equalsIgnoreCase(mainFunction.getFunctionName())) {
        //main definition is always a call edge
        result.add(node);
        continue;
      }
      if (node.getEnteringSummaryEdge() != null) {
        CFANode pred = node.getEnteringSummaryEdge().getPredecessor();
        if (!pNodes.contains(pred)) {
          result.add(node);
        }
        //ignore inner function calls
        continue;
      }
      for (CFAEdge edge : CFAUtils.enteringEdges(node)) {
        CFANode pred = edge.getPredecessor();
        if (!pNodes.contains(pred)) {
          //entering edge from "outside" of the given set of nodes
          //-> this is a call-node
          result.add(node);
        }
      }
    }
    return result;
  }

  private Set<CFANode> collectReturnNodes(Set<CFANode> pNodes, CFANode mainFunction) {
    Set<CFANode> result = new HashSet<>();
    for (CFANode node : pNodes) {
      if (node instanceof FunctionExitNode &&
          node.getFunctionName().equalsIgnoreCase(mainFunction.getFunctionName())) {
        //main exit nodes are always return nodes
        result.add(node);
        continue;
      }

      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
        CFANode succ = leavingEdge.getSuccessor();
        if (!pNodes.contains(succ)) {
          //leaving edge from inside of the given set of nodes to outside
          //-> this is a either return-node or a function call
          if (!(leavingEdge instanceof CFunctionCallEdge)) {
            //-> only add if its not a function call
            result.add(node);
          } else {
            //otherwise check if the summary edge is inside of the block
            CFANode sumSucc = ((CFunctionCallEdge) leavingEdge).getSummaryEdge().getSuccessor();
            if (!pNodes.contains(sumSucc)) {
              //summary edge successor not in nodes set; this is a leaving edge
              //add entering nodes
              Iterables.addAll(result, CFAUtils.predecessorsOf(sumSucc));
            }
          }
        }
      }
    }
    return result;
  }

  private Set<ReferencedVariable> collectReferencedVariables(Set<CFANode> nodes) {
    return (new ReferencedVariablesCollector(nodes)).getVars();
  }
}
