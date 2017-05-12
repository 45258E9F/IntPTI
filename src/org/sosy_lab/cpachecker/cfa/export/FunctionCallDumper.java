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
package org.sosy_lab.cpachecker.cfa.export;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.io.IOException;
import java.util.Set;


/**
 * This class allows to dump functioncalls in a tree-like structure.
 * For most cases the structure is a tree, but for special cases the graph contains
 * loops (-> recursion) or several root-nodes (-> one for each unused function).
 */
public class FunctionCallDumper {

  /**
   * This method iterates over the CFA, searches for functioncalls
   * and after that, dumps them in a dot-format.
   */
  public static void dump(final Appendable pAppender, final CFA pCfa) throws IOException {

    // get all function calls
    final CFAFunctionCallFinder finder = new CFAFunctionCallFinder();
    for (final FunctionEntryNode entryNode : pCfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(entryNode, finder);
    }

    // build dot-file
    pAppender.append("digraph functioncalls {\n");
    pAppender.append("rankdir=LR;\n\n"); // node-order from Left to Right is nicer

    // external functions are not part of functionNames
    final Set<String> functionNames = pCfa.getAllFunctionNames();

    final String mainFunction = pCfa.getMainFunction().getFunctionName();
    pAppender.append(mainFunction + " [shape=\"box\", color=blue];\n");

    for (final String callerFunctionName : finder.functionCalls.keySet()) {
      for (final String calleeFunctionName : finder.functionCalls.get(callerFunctionName)) {
        // call to external function
        if (!functionNames.contains(calleeFunctionName)) {
          pAppender.append(calleeFunctionName + " [shape=\"box\", color=grey];\n");
        }

        pAppender.append(callerFunctionName + " -> " + calleeFunctionName + ";\n");
      }
    }

    pAppender.append("}\n");
  }

  private static class CFAFunctionCallFinder extends DefaultCFAVisitor {

    /**
     * contains pairs of (functionname, calledFunction)
     */
    final Multimap<String, String> functionCalls = LinkedHashMultimap.create();

    @Override
    public TraversalProcess visitEdge(final CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {

        case CallToReturnEdge: {
          // the normal case of functioncall, both functions have their complete CFA
          final FunctionSummaryEdge function = (FunctionSummaryEdge) pEdge;
          final String functionName = function.getPredecessor().getFunctionName();
          final String calledFunction =
              function.getPredecessor().getLeavingEdge(0).getSuccessor().getFunctionName();
          functionCalls.put(functionName, calledFunction);
          break;
        }

        case StatementEdge: {
          final AStatementEdge edge = (AStatementEdge) pEdge;
          if (edge.getStatement() instanceof AFunctionCall) {
            // called function has no body, only declaration available, external function
            final AFunctionCall functionCall = (AFunctionCall) edge.getStatement();
            final AFunctionCallExpression functionCallExpression =
                functionCall.getFunctionCallExpression();
            final AFunctionDeclaration declaration = functionCallExpression.getDeclaration();
            if (declaration != null) {
              final String functionName = pEdge.getPredecessor().getFunctionName();
              final String calledFunction = declaration.getName();
              functionCalls.put(functionName, calledFunction);
            }
          }
          break;
        }

        case FunctionCallEdge: {
          throw new AssertionError("traversal-strategy should ignore functioncalls");
        }

        default:
          // nothing to do

      }
      return TraversalProcess.CONTINUE;
    }
  }
}
