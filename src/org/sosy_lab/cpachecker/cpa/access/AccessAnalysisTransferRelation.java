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
package org.sosy_lab.cpachecker.cpa.access;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AccessSummaryUtil;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This transfer relation should be always used in backward analysis
 */
class AccessAnalysisTransferRelation
    extends ForwardingTransferRelation<AccessAnalysisState, AccessAnalysisState, Precision> {

  private final Map<String, CFunctionDeclaration> funDecls;
  private final AccessSummaryProvider provider;

  public AccessAnalysisTransferRelation(SummaryManager<AccessAnalysisState> pSummary) {
    super();
    Preconditions.checkArgument(GlobalInfo.getInstance().getCFAInfo().isPresent());
    funDecls = GlobalInfo.getInstance().getCFAInfo().get().getFunctionDecls();
    provider = AccessSummaryProvider.fromValue(pSummary);
  }

  /**
   * expression is read
   */
  private AccessAnalysisState handleRightHandSide(CRightHandSide rhs, AccessAnalysisState state)
      throws CPATransferException {
    AccessPathVisitor visitor = new AccessPathVisitor(state, provider);
    AccessPath ap = rhs.accept(visitor);
    return AccessAnalysisState.markRead(ap, visitor.getState());
  }

  /**
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
    // apply summary
    return AccessSummaryUtil.apply(calledFunctionName, provider, arguments, state);
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
    // tomgu FIXME declaration with initializer
    if (qn == null) {
      return AccessAnalysisState.copyOf(state);
    }
    // Example: int *p = pN; *p = 1;where pN is a pointer;
    // that is -> if is simpleType ok, else we should keep r/w trees
    // tomgu FIXME build a hanleAssignment
    AccessAnalysisState resultState = state;
    // CVariableDeclaration we only consider the initExpression
    if (decl instanceof CVariableDeclaration) {
      CInitializer initializer = ((CVariableDeclaration) decl).getInitializer();
      if (initializer instanceof CInitializerExpression) {
        if (decl.getType() instanceof CPointerType) {
          CLeftHandSide left = new CIdExpression(decl.getFileLocation(), decl);
          AccessPathVisitor visitor = new AccessPathVisitor(resultState, provider);
          AccessPath apLhs = left.accept(visitor);
          List<String> lpath = apLhs == null ? null : AccessPath.toStrList(apLhs);
          if (apLhs != null) {
            resultState = resultState.eraseRead(lpath);
            resultState =
                handleAssignment(left, ((CInitializerExpression) initializer).getExpression(),
                    resultState);
          }
        }
      }

    }
    // at last we remove all the r/w trees
    resultState = resultState.erase(qn);
    return resultState;
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
   * expression is written assume: lhs = rhs 1. clear READ  lhs: reading of lhs should be erased 2.
   * mark  READ  rhs: the entire rhs is read 3. mark  WRITE rhs: transfer writing of lhs to rhs (on
   * paths with *), note that reading of lhs does not need to be transfered becauseof 1. 4. mark
   * WRITE lhs: lhs should be marked write
   */
  private AccessAnalysisState handleAssignment(
      CLeftHandSide lhs,
      CRightHandSide rhs, AccessAnalysisState postState) throws UnrecognizedCCodeException {
    AccessPathVisitor visitor = new AccessPathVisitor(postState, provider);
    AccessPath apLhs = lhs.accept(visitor);
    AccessPath apRhs = rhs.accept(visitor);
    List<String> lpath = apLhs == null ? null : AccessPath.toStrList(apLhs);
    List<String> rpath = apRhs == null ? null : AccessPath.toStrList(apRhs);
    AccessAnalysisState start = visitor.getState();
    AccessAnalysisState s = start;
    // 1
    // example:
    // -- x is read here? NO!
    // x = y.a;
    // output(x);
    if (apLhs != null) {
      s = s.eraseRead(lpath);
    }
    // 2
    if (apRhs != null) {
      s = AccessAnalysisState.markRead(apRhs, s);
    }
    // 3
    if (apLhs != null && apRhs != null) {
      // use start instead of s, because some of the read path are already erased from s
      s = AccessSummaryUtil.attach(s, rpath, start, lpath);
    }
    // 4
    if (apLhs != null) {
      s = AccessAnalysisState.markWrite(apLhs, s);
    }
    return s;
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
    if (expr instanceof CIdExpression) {
      // function name explicitly given
      return AccessSummaryUtil.apply(
          functionName,
          provider,
          cfaEdge.getExpression().getFunctionCallExpression().getParameterExpressions(),
          state
      );
    } else {
      // TODO: we could use approximation for function pointers
      // try to use AccessSummaryUtil.mostGeneralSummary
      throw new RuntimeException("function pointers not supported!");
    }
  }

  @Override
  protected AccessAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getDescription() != null &&
        cfaEdge.getDescription().equals("Function start dummy edge")) {
      // Entry --BlacnkEdge--> Node
      // update summary
      CFunctionDeclaration funDecl = funDecls.get(functionName);
      AccessAnalysisState newSummary = renameParameters(funDecl, state);
      AccessAnalysisState oldSummary = provider.provide().getSummary(functionName);
      AccessAnalysisState joinedSummary = oldSummary.join(newSummary);
      try {
        if (joinedSummary.isLessOrEqual(oldSummary)) {
          // already covered, no successor
          return null;
        } else {
          provider.provide().setSummary(functionName, joinedSummary);
          // the summary can be used as AbstractState
          // for debug
          System.out.println(functionName + " -> " + joinedSummary);
          return joinedSummary;
        }
      } catch (CPAException e) {
        // do not produce successors on failure.
        e.printStackTrace();
        return null;
      } catch (InterruptedException e) {
        // do not produce successors on failure.
        e.printStackTrace();
        return null;
      }
    } else {
      return AccessAnalysisState.copyOf(state);
    }
  }

  private AccessAnalysisState renameParameters(
      CFunctionDeclaration funDecl,
      AccessAnalysisState summary) {
    int i = 0;
    for (CParameterDeclaration pd : funDecl.getParameters()) {
      List<String> oldPath = Collections.singletonList(pd.getName());
      List<String> newPath = Collections.singletonList("$" + i);
      summary = summary.move(oldPath, newPath);
      i++;
    }
    return summary;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates, CFAEdge pCfaEdge, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    return null;
  }

}
