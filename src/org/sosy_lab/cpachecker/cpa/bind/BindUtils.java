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
package org.sosy_lab.cpachecker.cpa.bind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class BindUtils {

  private static List<CFANode> cfaNodes;

  public static List<CFANode> getAllNodesFromCFA() {
    return cfaNodes;
  }

  public static Pair<Set<AccessPath>, Map<FunctionEntryNode, Set<AccessPath>>> getAllVariables(
      CFANode pMainNode) {
    List<AccessPath> globalVariables = new ArrayList<>();
    List<CFANode> nodes = new ArrayList<>();

    assert (pMainNode instanceof FunctionEntryNode);
    Map<FunctionEntryNode, Set<AccessPath>> result = new HashMap<>();

    HashSet<FunctionEntryNode> reachedFunctions = new HashSet<>();
    Stack<FunctionEntryNode> functionsToProcess = new Stack<>();

    Stack<CFANode> currentWaitlist = new Stack<>();
    HashSet<CFANode> seen = new HashSet<>();
    List<AccessPath> localVariables = new ArrayList<>();
    CFANode currentElement;
    FunctionEntryNode currentFunction;

    reachedFunctions.add((FunctionEntryNode) pMainNode);
    functionsToProcess.add((FunctionEntryNode) pMainNode);

    while (!functionsToProcess.isEmpty()) {
      currentFunction = functionsToProcess.pop();
      currentWaitlist.clear();
      currentWaitlist.add(currentFunction);
      seen.clear();
      seen.add(currentFunction);
      localVariables.clear();

      while (!currentWaitlist.isEmpty()) {
        currentElement = currentWaitlist.pop();
        nodes.add(currentElement);

        for (CFAEdge out : CFAUtils.leavingEdges(currentElement)) {
          if (out instanceof FunctionReturnEdge) {
            continue;
          }

          if (out instanceof FunctionCallEdge) {
            if (!reachedFunctions.contains(out.getSuccessor())) {
              functionsToProcess.add((FunctionEntryNode) out.getSuccessor());
              reachedFunctions.add((FunctionEntryNode) out.getSuccessor());
            }
            out = currentElement.getLeavingSummaryEdge();
          }

          if (out instanceof CDeclarationEdge) {
            handleDeclaration((CDeclarationEdge) out, globalVariables, localVariables);
          }

          if (out instanceof MultiEdge) {
            for (CFAEdge inner : ((MultiEdge) out).getEdges()) {
              assert (!(inner instanceof FunctionCallEdge || inner instanceof FunctionReturnEdge));
              if (inner instanceof CDeclarationEdge) {
                handleDeclaration((CDeclarationEdge) inner, globalVariables, localVariables);
              }
            }
          }

          if (!seen.contains(out.getSuccessor())) {
            currentWaitlist.add(out.getSuccessor());
            seen.add(out.getSuccessor());
          }
        }
      }

      result.put(currentFunction, ImmutableSet.copyOf(localVariables));
    }
    cfaNodes = ImmutableList.copyOf(nodes);
    return Pair.of((Set<AccessPath>) ImmutableSet.copyOf(globalVariables), result);
  }

  /**
   * Use qualified names
   */
  private static void handleDeclaration(
      final CDeclarationEdge declEdge,
      final List<AccessPath> globalVariables,
      final List<AccessPath> localVariables) {
    if (declEdge.getDeclaration() instanceof CVariableDeclaration) {
      if (declEdge.getDeclaration().isGlobal()) {
        globalVariables.add(new AccessPath(declEdge.getDeclaration()));
      } else {
        localVariables.add(new AccessPath(declEdge.getDeclaration()));
      }
    }
  }

  public static PathCopyingPersistentTree<String, Integer> getWritesFromCFAEdge(
      CFAEdge edge, List<AbstractState> otherStates)
      throws UnrecognizedCCodeException {
    switch (edge.getEdgeType()) {
      case StatementEdge: {
        CStatementEdge statementEdge = (CStatementEdge) edge;
        CStatement statement = statementEdge.getStatement();
        if (statement instanceof CAssignment) {
          CRightHandSide right = ((CAssignment) statement).getRightHandSide();
          return right.accept(new AccessPathCollector(otherStates));
        }
        return PathCopyingPersistentTree.of();
      }
      case DeclarationEdge: {
        CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        CDeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration varDeclaration = (CVariableDeclaration) declaration;
          CInitializer initializer = varDeclaration.getInitializer();
          if (initializer != null) {
            return handleInitializer(initializer, otherStates);
          }
        }
        return PathCopyingPersistentTree.of();
      }
      default:
        return PathCopyingPersistentTree.of();
    }
  }

  private static PathCopyingPersistentTree<String, Integer> handleInitializer(
      CInitializer initializer, List<AbstractState> otherStates)
      throws UnrecognizedCCodeException {
    if (initializer instanceof CInitializerExpression) {
      CExpression exp = ((CInitializerExpression) initializer).getExpression();
      return exp.accept(new AccessPathCollector(otherStates));
    } else if (initializer instanceof CDesignatedInitializer) {
      // we only care about the right-hand-side
      CInitializer rightHandSide = ((CDesignatedInitializer) initializer).getRightHandSide();
      return handleInitializer(rightHandSide, otherStates);
    } else {
      PathCopyingPersistentTree<String, Integer> writes = new PathCopyingPersistentTree<>();
      CInitializerList initializerList = (CInitializerList) initializer;
      List<CInitializer> initializers = initializerList.getInitializers();
      for (CInitializer singleInitializer : initializers) {
        writes = PathCopyingPersistentTree.merge(writes, handleInitializer(singleInitializer,
            otherStates),
            AccessPathCollector.defaultMerger);
      }
      return writes;
    }
  }


  public static CRightHandSide extractRightHandSide(CFAEdge edge, AccessPath path) {
    switch (edge.getEdgeType()) {
      case StatementEdge: {
        CStatement statement = ((CStatementEdge) edge).getStatement();
        if (statement instanceof CAssignment) {
          return ((CAssignment) statement).getRightHandSide();
        }
        break;
      }
      case DeclarationEdge: {
        CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration varDeclaration = (CVariableDeclaration) declaration;
          CInitializer initializer = varDeclaration.getInitializer();
          if (initializer != null && initializer instanceof CInitializerExpression) {
            return ((CInitializerExpression) initializer).getExpression();
          }
        }
        break;
      }
      case FunctionCallEdge: {
        final CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
        final CFunctionEntryNode entry = callEdge.getSuccessor();
        List<CExpression> args = callEdge.getArguments();
        List<CParameterDeclaration> params = entry.getFunctionParameters();
        for (int i = 0; i < params.size(); i++) {
          CParameterDeclaration param = params.get(i);
          if (path.getQualifiedName().equals(param.getQualifiedName())) {
            return args.get(i);
          }
        }
        break;
      }
      case FunctionReturnEdge: {
        CFunctionSummaryEdge summaryEdge = ((CFunctionReturnEdge) edge).getSummaryEdge();
        CFunctionCall call = summaryEdge.getExpression();
        if (call instanceof CAssignment) {
          return ((CAssignment) call).getRightHandSide();
        }
        break;
      }
      default:
        return null;
    }
    return null;
  }

}