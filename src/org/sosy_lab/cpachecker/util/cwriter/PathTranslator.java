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
package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public abstract class PathTranslator {

  private static Function<CAstNode, String> RAW_SIGNATURE_FUNCTION =
      new Function<CAstNode, String>() {

        @Override
        public String apply(CAstNode pArg0) {
          return pArg0.toASTString();
        }
      };

  protected final static CFunctionEntryNode extractFunctionCallLocation(ARGState state) {
    // We assume, that each node has one location.
    // TODO: the location is invalid for all concurrent programs,
    //       because interleaving threads are not handled.
    return FluentIterable.from(extractLocations(state))
        .filter(CFunctionEntryNode.class)
        .first().orNull();
  }

  protected final List<String> mGlobalDefinitionsList = new ArrayList<>();
  protected final List<String> mFunctionDecls = new ArrayList<>();
  private int mFunctionIndex = 0;

  // list of functions
  protected final List<FunctionBody> mFunctionBodies = new ArrayList<>();

  protected PathTranslator() {
  }

  /**
   * Gets the piece of code that should appear at the target state; e.g. <code>assert(0)</code> or
   * <code>exit(-1)</code>
   *
   * @return Line of code for target state
   */
  protected abstract String getTargetState();

  protected Appender generateCCode() {
    return Appenders.forIterable(Joiner.on('\n'),
        concat(mGlobalDefinitionsList,
            mFunctionDecls,
            mFunctionBodies));
  }

  /**
   * Translate a single linear path to code.
   *
   * @param pPath    the path to translate
   * @param callback A callback that receives each <code>ARGState</code> along with their edges and
   *                 can then determine what code to generate from it. The default behavior of a
   *                 <code>ProcessEdgeFunction</code> is to call {@link #processEdge(ARGState,
   *                 CFAEdge, Stack)}
   */
  protected void translateSinglePath0(ARGPath pPath, EdgeVisitor callback) {
    assert pPath.size() >= 1;

    PathIterator pathIt = pPath.pathIterator();
    ARGState firstElement = pathIt.getAbstractState();

    Stack<FunctionBody> functionStack = new Stack<>();

    // create the first function and put in into the stack
    startFunction(firstElement, functionStack, extractFunctionCallLocation(firstElement));

    while (pathIt.hasNext()) {
      pathIt.advance();

      CFAEdge currentCFAEdge = pathIt.getIncomingEdge();
      ARGState childElement = pathIt.getAbstractState();

      callback.visit(childElement, currentCFAEdge, functionStack);
    }
  }

  protected final void translatePaths0(
      final ARGState firstElement,
      Set<ARGState> elementsOnPath,
      EdgeVisitor callback) {
    // waitlist for the edges to be processed
    List<Edge> waitlist = new ArrayList<>();

    // map of nodes to check end of a condition
    Map<Integer, MergeNode> mergeNodes = new HashMap<>();

    // create initial element
    {
      Stack<FunctionBody> newStack = new Stack<>();

      // create the first function and put in into newStack
      startFunction(firstElement, newStack, extractFunctionCallLocation(firstElement));

      waitlist.addAll(getRelevantChildrenOfState(firstElement, newStack, elementsOnPath));
    }

    while (!waitlist.isEmpty()) {
      // we need to sort the list based on arg element id because we have to process
      // the edges in topological sort
      Collections.sort(waitlist);

      // get the first element in the list (this is the smallest element when topologically sorted)
      Edge nextEdge = waitlist.remove(0);

      waitlist.addAll(handleEdge(nextEdge, mergeNodes, elementsOnPath, callback));
    }
  }

  /**
   * Start the function, puts another body on the function stack.
   *
   * @param firstFunctionElement the first state inside the function
   * @param functionStack        the current callstack
   * @param predecessor          the previous node
   */
  protected String startFunction(
      ARGState firstFunctionElement,
      Stack<FunctionBody> functionStack,
      CFANode predecessor) {
    // create the first stack element using the first element of the function
    CFunctionEntryNode functionStartNode = extractFunctionCallLocation(firstFunctionElement);
    String freshFunctionName = getFreshFunctionName(functionStartNode);

    String lFunctionHeader =
        functionStartNode.getFunctionDefinition().getType().toASTString(freshFunctionName);
    // lFunctionHeader is for example "void foo_99(int a)"

    // create a new function
    FunctionBody newFunction = new FunctionBody(firstFunctionElement.getStateId(),
        lFunctionHeader);

    // register function
    mFunctionDecls.add(lFunctionHeader + ";");
    mFunctionBodies.add(newFunction);
    functionStack.push(newFunction); // add function to current stack
    return freshFunctionName;
  }

  /**
   * Processes an edge of the CFA and will write code to the output function body.
   *
   * @param childElement  the state after the given edge
   * @param edge          the edge to process
   * @param functionStack the current callstack
   */
  void processEdge(ARGState childElement, CFAEdge edge, Stack<FunctionBody> functionStack) {
    FunctionBody currentFunction = functionStack.peek();

    if (childElement.isTarget()) {
      currentFunction.write(getTargetState());
    }

    // handle the edge

    if (edge instanceof CFunctionCallEdge) {
      // if this is a function call edge we need to create a new state and push
      // it to the topmost stack to represent the function

      // create function and put in onto stack
      String freshFunctionName = startFunction(childElement, functionStack, edge.getPredecessor());

      // write summary edge to the caller site (with the new unique function name)
      currentFunction.write(processFunctionCall(edge, freshFunctionName));

    } else if (edge instanceof CFunctionReturnEdge) {
      functionStack.pop();

    } else {
      currentFunction.write(processSimpleEdge(edge, currentFunction.getCurrentBlock()));
    }
  }

  private Collection<Edge> handleEdge(
      Edge nextEdge, Map<Integer, MergeNode> mergeNodes,
      Set<ARGState> elementsOnPath,
      EdgeVisitor callback) {
    ARGState childElement = nextEdge.getChildState();
    CFAEdge edge = nextEdge.getEdge();
    Stack<FunctionBody> functionStack = nextEdge.getStack();

    // clone stack to have a different representation of the function calls and conditions
    // for every element
    functionStack = cloneStack(functionStack);

    callback.visit(childElement, edge, functionStack);

    // how many parents does the child have?
    // ignore parents not on the error path
    int noOfParents = from(childElement.getParents()).filter(in(elementsOnPath)).size();
    assert noOfParents >= 1;

    // handle merging if necessary
    if (noOfParents > 1) {
      assert !((edge instanceof CFunctionCallEdge)
          || (childElement.isTarget()));

      // this is the end of a condition, determine whether we should continue or backtrack

      int elemId = childElement.getStateId();
      FunctionBody currentFunction = functionStack.peek();
      currentFunction.write("goto label_" + elemId + ";");

      // get the merge node for that node
      MergeNode mergeNode = mergeNodes.get(elemId);
      // if null create new and put in the map
      if (mergeNode == null) {
        mergeNode = new MergeNode(elemId);
        mergeNodes.put(elemId, mergeNode);
      }

      // this tells us the number of edges (entering that node) processed so far
      int noOfProcessedBranches = mergeNode.addBranch(currentFunction);

      // if all edges are processed
      if (noOfParents == noOfProcessedBranches) {
        // all branches are processed, now decide which nodes to remove from the stack
        List<FunctionBody> incomingStacks = mergeNode.getIncomingStates();

        FunctionBody newFunction = processIncomingStacks(incomingStacks);

        // replace the current function body with the right one
        functionStack.pop();
        functionStack.push(newFunction);

        newFunction.write("label_" + elemId + ": ;");

      } else {
        return Collections.emptySet();
      }
    }

    return getRelevantChildrenOfState(childElement, functionStack, elementsOnPath);
  }

  private Collection<Edge> getRelevantChildrenOfState(
      ARGState currentElement, Stack<FunctionBody> functionStack,
      Set<ARGState> elementsOnPath) {
    // find the next elements to add to the waitlist

    List<ARGState> relevantChildrenOfElement =
        from(currentElement.getChildren()).filter(in(elementsOnPath)).toList();

    // if there is only one child on the path
    if (relevantChildrenOfElement.size() == 1) {
      // get the next ARG state, create a new edge using the same stack and add it to the waitlist
      ARGState elem = Iterables.getOnlyElement(relevantChildrenOfElement);
      CFAEdge e = currentElement.getEdgeToChild(elem);
      Edge newEdge = new Edge(elem, e, functionStack);
      return Collections.singleton(newEdge);

    } else if (relevantChildrenOfElement.size() > 1) {
      // if there are more than one relevant child, then this is a condition
      // we need to update the stack
      assert relevantChildrenOfElement.size() == 2;
      Collection<Edge> result = new ArrayList<>(2);
      int ind = 0;
      for (ARGState elem : relevantChildrenOfElement) {
        Stack<FunctionBody> newStack = cloneStack(functionStack);
        CFAEdge e = currentElement.getEdgeToChild(elem);
        FunctionBody currentFunction = newStack.peek();
        assert e instanceof CAssumeEdge;
        CAssumeEdge assumeEdge = (CAssumeEdge) e;
        boolean truthAssumption = assumeEdge.getTruthAssumption();

        String cond = "";

        if (ind == 0) {
          cond = "if ";
        } else if (ind == 1) {
          cond = "else if ";
        } else {
          assert false;
        }
        ind++;

        if (truthAssumption) {
          cond += "(" + assumeEdge.getExpression().toASTString() + ")";
        } else {
          cond += "(!(" + assumeEdge.getExpression().toASTString() + "))";
        }

        // create a new block starting with this condition
        currentFunction.enterBlock(currentElement.getStateId(), assumeEdge, cond);

        Edge newEdge = new Edge(elem, e, newStack);
        result.add(newEdge);
      }
      return result;
    }
    return Collections.emptyList();
  }

  private static FunctionBody processIncomingStacks(
      List<FunctionBody> pIncomingStacks) {

    FunctionBody maxStack = null;
    int maxSizeOfStack = 0;

    for (FunctionBody stack : pIncomingStacks) {
      while (true) {
        if (stack.getCurrentBlock().isClosedBefore()) {
          stack.leaveBlock();
        } else {
          break;
        }
      }
      if (stack.size() > maxSizeOfStack) {
        maxStack = stack;
        maxSizeOfStack = maxStack.size();
      }
    }

    return maxStack;

  }

  protected String processSimpleEdge(CFAEdge pCFAEdge, BasicBlock currentBlock) {

    switch (pCFAEdge.getEdgeType()) {

      case BlankEdge:
      case StatementEdge:
      case ReturnStatementEdge:
      case CallToReturnEdge:
        return pCFAEdge.getCode();

      case AssumeEdge: {
        CAssumeEdge lAssumeEdge = (CAssumeEdge) pCFAEdge;
        return ("__CPROVER_assume(" + lAssumeEdge.getCode() + ");");
        //    return ("if(! (" + lAssumptionString + ")) { return (0); }");
      }

      case DeclarationEdge: {
        CDeclarationEdge lDeclarationEdge = (CDeclarationEdge) pCFAEdge;

        if (lDeclarationEdge.getDeclaration().isGlobal()) {
          mGlobalDefinitionsList.add(lDeclarationEdge.getCode());
          return "";
        }

        // avoid having the same declaration edge twice in one basic block
        if (currentBlock.hasDeclaration(lDeclarationEdge)) {
          return "";
        } else {
          currentBlock.addDeclaration(lDeclarationEdge);
          return lDeclarationEdge.getCode();
        }
      }

      case MultiEdge: {
        StringBuilder sb = new StringBuilder();

        for (CFAEdge edge : (MultiEdge) pCFAEdge) {
          sb.append(processSimpleEdge(edge, currentBlock));
        }
        return sb.toString();
      }

      default:
        throw new AssertionError(
            "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType());
    }
  }

  protected final String processFunctionCall(CFAEdge pCFAEdge, String functionName) {

    CFunctionCallEdge lFunctionCallEdge = (CFunctionCallEdge) pCFAEdge;

    List<String> lArguments =
        Lists.transform(lFunctionCallEdge.getArguments(), RAW_SIGNATURE_FUNCTION);
    String lArgumentString = "(" + Joiner.on(", ").join(lArguments) + ")";

    CFunctionSummaryEdge summaryEdge = lFunctionCallEdge.getSummaryEdge();
    if (summaryEdge == null) {
      // no summary edge, i.e., no return to this function (CFA was pruned)
      // we don't need to care whether this was an assignment or just a function call
      return functionName + lArgumentString + ";";
    }

    CFunctionCall expressionOnSummaryEdge = summaryEdge.getExpression();
    if (expressionOnSummaryEdge instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp =
          (CFunctionCallAssignmentStatement) expressionOnSummaryEdge;
      String assignedVarName = assignExp.getLeftHandSide().toASTString();
      return assignedVarName + " = " + functionName + lArgumentString + ";";

    } else {
      assert expressionOnSummaryEdge instanceof CFunctionCallStatement;
      return functionName + lArgumentString + ";";
    }
  }

  protected final String getFreshFunctionName(FunctionEntryNode functionStartNode) {
    return functionStartNode.getFunctionName() + "_" + mFunctionIndex++;
  }

  private Stack<FunctionBody> cloneStack(Stack<FunctionBody> pStack) {

    Stack<FunctionBody> ret = new Stack<>();
    for (FunctionBody functionBody : pStack) {
      ret.push(new FunctionBody(functionBody));
    }
    return ret;
  }
}
