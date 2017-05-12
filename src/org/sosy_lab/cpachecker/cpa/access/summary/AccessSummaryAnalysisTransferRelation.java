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
package org.sosy_lab.cpachecker.cpa.access.summary;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.access.AccessAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AccessSummaryUtil;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by tomgu on 2/7/17.
 * This transfer relation should be always used in backward analysis
 * tomgu for summary - loop and function
 * 1. for each edge we compute function level
 * 2. if it is in any loop, we compute loop
 * 3. Notice: we always think trans in function level, the loopMap is used for recording
 *
 * Note: for access, function call and assignment may change the state
 * 1. function call: [treated in Util]
 * if it has summary -> Util.apply [For, we ensure the summary is right in assignment]
 * if it not have -> all is read, pointer or array is written
 * 2. assignment:
 * 2.1 if it is a pointer p = q;
 * 2.1.1 q is read
 * 2.1.2 apply p to q [For, p and q point to the same address]
 * 2.1.3 clear p [For, after the assign statement, state of program do not depend on old_p]
 * 2.2 if it is a value x = y; [C is call by value, copy the memory of y to x]
 * 2.2.1 y is read
 * 2.2.2 x is clear [For, before the assign statement, state of program do not depend on old_p]
 * 2.2.3 x is written [For, after the assign statement, state of program depend on new_p]
 */
public class AccessSummaryAnalysisTransferRelation
    extends ForwardingTransferRelation<AccessAnalysisState, AccessAnalysisState, Precision> {

  /*
   * tomgu
   * for summary phase, record loop state
   * for each edge, we compute the function level
   * then if edge in loop, we compute again using loopState
   */
  private Map<Loop, AccessAnalysisState> loopStateMap;
  // to record the variable declaration in loop, finally, we need to manually remove these variables
  private Map<Loop, Set<CVariableDeclaration>> loopVarDeclarationMap;
  // save callee's summary
  protected Map<String, AccessAnalysisState> summary;

  public AccessSummaryAnalysisTransferRelation() {
    super();

    loopStateMap = Maps.newHashMap();
    loopVarDeclarationMap = Maps.newHashMap();
    summary = Maps.newHashMap();
  }

  /**
   * for computation
   * *
   * * /
   *
   * /**
   * Handles the {@link CAssumeEdge}
   *
   * @param cfaEdge         the edge to handle
   * @param expression      the condition of the edge
   * @param truthAssumption indicates if this is the then or the else branch
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleAssumption(
      CAssumeEdge cfaEdge,
      CExpression expression,
      boolean truthAssumption)
      throws CPATransferException {
    // for loop
    List<Loop> loops = getLoopContainEdge(cfaEdge);
    for (Loop loop : loops) {
      AccessAnalysisState loopState = loopStateMap.get(loop);
      loopState = handleRightHandSide(expression, loopState);
      loopStateMap.put(loop, loopState);
    }
    return handleRightHandSide(expression, state);
  }

  /**
   * Handles the {@link CFunctionCallEdge}.
   *
   * @param cfaEdge            the edge to be handled
   * @param arguments          the arguments given to the function
   * @param parameters         the parameters of the function
   * @param calledFunctionName the name of the function
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    // for loop
    List<Loop> loops = getLoopContainEdge(cfaEdge);
    for (Loop loop : loops) {
      AccessAnalysisState loopState = loopStateMap.get(loop);
      loopState = AccessSummaryUtil.apply(calledFunctionName, summary, arguments, loopState);
      loopStateMap.put(loop, loopState);
    }
    // apply summary
    return AccessSummaryUtil.apply(calledFunctionName, summary, arguments, state);
  }

  /**
   * Handles the {@link CFunctionReturnEdge}
   *
   * @param cfaEdge            the edge to handle
   * @param fnkCall            the summary edge of the formerly called function
   * @param summaryExpr        the function call
   * @param callerFunctionName the name of the called function
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    // do not perform interprocedural analysis now, handle it in the algorithm level
    return null;
  }

  /**
   * Handles the {@link CDeclarationEdge}
   *
   * @param cfaEdge the edge to handle
   * @param decl    the declaration at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    String qn = decl.getQualifiedName();
    // tomgu declaration with initializer
    if (qn == null) {
      return AccessAnalysisState.copyOf(state);
    }

    // for loop
    List<Loop> loops = getLoopContainEdge(cfaEdge);
    for (Loop loop : loops) {
      AccessAnalysisState loopState = loopStateMap.get(loop);
      if (decl instanceof CVariableDeclaration) {
        // add into map, we have to remove these as external loop summary
        // TODO we need refactor loopSummary -> external and internal
        loopVarDeclarationMap.get(loop).add((CVariableDeclaration) decl);
        loopState = handleVariableDeclarationEdge(decl, loopState);
        loopStateMap.put(loop, loopState);
      }
    }

    // Example: int *p = pN; *p = 1;where pN is a pointer;
    // that is -> if is simpleType ok, else we should keep r/w trees
    // tomgu build a handleAssignment
    return handleVariableDeclarationEdge(decl, state);
  }


  /**
   * Handles the {@link CStatementEdge}
   *
   * @param cfaEdge   the edge to handle
   * @param statement the statement at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    // for loop
    List<Loop> loops = getLoopContainEdge(cfaEdge);
    for (Loop loop : loops) {
      AccessAnalysisState loopState = loopStateMap.get(loop);
      loopState = handleStatementEdgeForLoop(cfaEdge, statement, loopState);
      loopStateMap.put(loop, loopState);
    }


    if (statement instanceof CFunctionCallStatement) {
      CFunctionCallStatement fcs = (CFunctionCallStatement) statement;
      return handleRightHandSide(fcs.getFunctionCallExpression(), state);
    } else if (statement instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement fcas = (CFunctionCallAssignmentStatement) statement;
      return handleAssignment(fcas.getLeftHandSide(), fcas.getRightHandSide(), state);
    } else if (statement instanceof CExpressionStatement) {
      CExpressionStatement es = (CExpressionStatement) statement;
      return handleRightHandSide(es.getExpression(), state);
    } else if (statement instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement fes = (CExpressionAssignmentStatement) statement;
      return handleAssignment(fes.getLeftHandSide(), fes.getRightHandSide(), state);
    } else {
      throw new UnrecognizedCCodeException(statement.toASTString(), statement);
    }
  }


  /**
   * Handles the {@link CReturnStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {
    // function return statement does not change the state
    // although the returned value may be read/write in the caller
    // it is not necessary to consider those access actions here
    return AccessAnalysisState.copyOf(state);
  }

  /**
   * Handle the {@link CFunctionSummaryEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  @Override
  protected AccessAnalysisState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge)
      throws CPATransferException {
    CExpression expr =
        cfaEdge.getExpression().getFunctionCallExpression().getFunctionNameExpression();

    // for loop
    List<Loop> loops = getLoopContainEdge(cfaEdge);
    for (Loop loop : loops) {
      AccessAnalysisState loopState = loopStateMap.get(loop);
      loopState = handleFunctionSummaryEdgeForLoop(cfaEdge, loopState);
      loopStateMap.put(loop, loopState);
    }

    if (expr instanceof CIdExpression) {
      // function name explicitly given
      return AccessSummaryUtil.apply(
          ((CIdExpression) expr).getName(),
          summary,
          cfaEdge.getExpression().getFunctionCallExpression().getParameterExpressions(),
          state
      );
    } else {
      //: we could use approximation for function pointers
      //throw new RuntimeException("function pointers not supported!");
      return state;
    }
  }

  @Override
  protected AccessAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getDescription() != null &&
        cfaEdge.getDescription().equals("Function start dummy edge")) {
      // Entry --BlankEdge--> Node
      // return null to finish this function
      // System.out.println(state);
      return null;
    } else {
      return AccessAnalysisState.copyOf(state);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates, CFAEdge pCfaEdge, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    return null;
  }

  private AccessAnalysisState handleVariableDeclarationEdge(
      CDeclaration decl,
      AccessAnalysisState pState) throws UnrecognizedCCodeException {
    AccessAnalysisState resultState = pState;
    // CVariableDeclaration we only consider the initExpression
    if (decl instanceof CVariableDeclaration) {
      CInitializer initializer = ((CVariableDeclaration) decl).getInitializer();
      if (initializer instanceof CInitializerExpression) {
        CLeftHandSide left = new CIdExpression(decl.getFileLocation(), decl);
        resultState = handleAssignment(left, ((CInitializerExpression) initializer).getExpression(),
            resultState);
      }

    }
    // at last we remove all the r/w trees
    resultState = resultState.erase(decl.getQualifiedName());
    return resultState;
  }

  private AccessAnalysisState handleStatementEdgeForLoop(
      CStatementEdge cfaEdge,
      CStatement statement, AccessAnalysisState pLoopState) throws CPATransferException {
    if (statement instanceof CFunctionCallStatement) {
      CFunctionCallStatement fcs = (CFunctionCallStatement) statement;
      return handleRightHandSide(fcs.getFunctionCallExpression(), pLoopState);
    } else if (statement instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement fcas = (CFunctionCallAssignmentStatement) statement;
      return handleAssignment(fcas.getLeftHandSide(), fcas.getRightHandSide(), pLoopState);
    } else if (statement instanceof CExpressionStatement) {
      CExpressionStatement es = (CExpressionStatement) statement;
      return handleRightHandSide(es.getExpression(), pLoopState);
    } else if (statement instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement fes = (CExpressionAssignmentStatement) statement;
      return handleAssignment(fes.getLeftHandSide(), fes.getRightHandSide(), pLoopState);
    } else {
      throw new UnrecognizedCCodeException(statement.toASTString(), statement);
    }
  }

  private AccessAnalysisState handleFunctionSummaryEdgeForLoop(
      CFunctionSummaryEdge pCfaEdge,
      AccessAnalysisState pLoopState) throws UnrecognizedCCodeException {
    CExpression expr =
        pCfaEdge.getExpression().getFunctionCallExpression().getFunctionNameExpression();
    if (expr instanceof CIdExpression) {
      // function name explicitly given
      return AccessSummaryUtil.apply(
          ((CIdExpression) expr).getName(),
          summary,
          pCfaEdge.getExpression().getFunctionCallExpression().getParameterExpressions(),
          pLoopState
      );
    } else {
      // : we could use approximation for function pointers
      // throw new RuntimeException("function pointers not supported!");
      return state;
    }
  }

  /**
   * expression is written handle assignment assume: lhs = rhs case one, lhs is pointer -> that is
   * lhs and rhs point to the same address. if rhs is functionCall, we ignore rhs TODO how to
   * handle? 1. make rhs read 2. transfer lhs to rhs 3. clear lhs
   *
   * case two, lhs is value -> that is lhs is not pointer[array can not be lvalue], in C, call by
   * value so, it performs the memory copy operation 1. rhs is read 2. lhs is clear 3. lhs is
   * written
   */
  private AccessAnalysisState handleAssignment(
      CLeftHandSide lhs,
      CRightHandSide rhs, AccessAnalysisState postState) throws UnrecognizedCCodeException {
    AccessSummaryPathVisitor visitor = new AccessSummaryPathVisitor(postState, summary);
    AccessPath apLhs = lhs.accept(visitor);
    AccessPath apRhs = rhs.accept(visitor);
    List<String> lpath = apLhs == null ? null : AccessPath.toStrList(apLhs);
    List<String> rpath = apRhs == null ? null : AccessPath.toStrList(apRhs);
    AccessAnalysisState start = visitor.getState();
    AccessAnalysisState s = start;

    // Case one:
    if (lhs.getExpressionType() instanceof CPointerType) {
      // 1. make rhs read
      if (apRhs != null) {
        s = AccessAnalysisState.markRead(apRhs, s);
      }

      // 2. transfer lhs to rhs
      if (apLhs != null && apRhs != null) {
        // use start instead of s, because some of the read path are already erased from s
        // for pointer = &obj; we should add * into lpath -> search pointer|* paths and remove amper of rpath
        if (lhs.getExpressionType() instanceof CPointerType &&
            rpath.get(rpath.size() - 1).equals(AddressingSegment.INSTANCE.getName())) {
          PathSegment seg = new PointerDereferenceSegment();
          List<String> lList = new ArrayList<>(lpath);
          lList.add(seg.getName());
          List<String> rList = new ArrayList<>(rpath);
          rList = rList.subList(0, rpath.size() - 1);
          s = AccessSummaryUtil.attach(s, rList, start, lList, true, true);
        } else {
          s = AccessSummaryUtil.attach(s, rpath, start, lpath, true, true);
        }
      }

      // 3. clear lhs
      if (lpath != null) {
        s = s.eraseRead(lpath).eraseWrite(lpath);
      }

    } else { // Case two:
      // 1. make rhs read
      if (apRhs != null) {
        s = AccessAnalysisState.markRead(apRhs, s);
      }

      // 2. clear lhs
      if (lpath != null) {
        // in tree: markWrite will remove the sub tree -> that is the write will be rewritten in step3
        s = s.eraseRead(lpath).eraseWrite(lpath);
      }

      // 3. lhs is written
      if (apLhs != null) {
        s = AccessAnalysisState.markWrite(apLhs, s);
      }
    }

    return s;
  }


  /**
   * expression is read
   */
  private AccessAnalysisState handleRightHandSide(CRightHandSide rhs, AccessAnalysisState state)
      throws CPATransferException {
    AccessSummaryPathVisitor visitor = new AccessSummaryPathVisitor(state, summary);
    AccessPath ap = rhs.accept(visitor);
    return AccessAnalysisState.markRead(ap, visitor.getState());
  }

  /************************
   for inner data structure
   *************************/

  // save loop
  void initLoopState(String pFName) {
    loopStateMap = Maps.newHashMap();
    loopVarDeclarationMap = Maps.newHashMap();
    Preconditions.checkArgument(GlobalInfo.getInstance().getCFAInfo().isPresent());
    if (GlobalInfo.getInstance().getCFAInfo().get().getCFA().getLoopStructure().isPresent()) {
      LoopStructure
          structure = GlobalInfo.getInstance().getCFAInfo().get().getCFA().getLoopStructure().get();
      ImmutableCollection<Loop> pImmutableCollection = structure.getLoopsForFunction(pFName);
      for (Loop l : pImmutableCollection) {
        loopStateMap.put(l, new AccessAnalysisState());
        loopVarDeclarationMap.put(l, new HashSet<CVariableDeclaration>());
      }
    }

  }

  private List<Loop> getLoopContainEdge(final CFAEdge edge) {
    List<Loop> loops = Lists.newArrayList();
    for (Loop loop : loopStateMap.keySet()) {
      // check whether this edge is in loop, for outcoming edges there have the same read/write state as incoming
      if (loop.getInnerLoopEdges().contains(edge) || loop.getIncomingEdges().contains(edge)) {
        loops.add(loop);
      }
    }
    return loops;
  }

  void eraseLoopVariable() {
    for (Loop loop : loopStateMap.keySet()) {
      for (CVariableDeclaration var : loopVarDeclarationMap.get(loop)) {
        AccessAnalysisState state = loopStateMap.get(loop);
        try {
          state = handleVariableDeclarationEdge(var, state);
        } catch (UnrecognizedCCodeException e) {
          e.printStackTrace();
        }
        loopStateMap.put(loop, state);
      }
    }
  }

  void getLoopStateMapCopy(Map<Loop, AccessAnalysisState> map) {
    for (Loop loop : loopStateMap.keySet()) {
      map.put(loop, new AccessAnalysisState(
          loopStateMap.get(loop).readTree, loopStateMap.get(loop).writeTree));
    }
  }
  // end loop

}
