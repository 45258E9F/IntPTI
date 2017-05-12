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
package org.sosy_lab.cpachecker.core.phase;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseEmptyResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.callgraph.CallGraph;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Options(prefix = "phase.critical")
public class CriticalCountPhase extends CPAPhase {

  private CFA cfa;
  private MachineModel machineModel;
  private CallGraph callGraph;

  private Map<FunctionEntryNode, Integer> indegreeMap = Maps.newHashMap();
  private Map<FunctionEntryNode, FunctionEntryWrapper> wrappers = Maps.newHashMap();
  private PriorityQueue<FunctionEntryWrapper> waitList = new PriorityQueue<>();

  private Set<FileLocation> criticalSites = new HashSet<>();

  @Option(secure = true, description = "whether all critical sites are counted")
  private boolean countAllSites = true;

  public CriticalCountPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStatistics)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStatistics);
    config.inject(this);
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new InvalidConfigurationException("Invalid CFA set-up");
    }
    cfa = cfaInfo.getCFA();
    callGraph = cfaInfo.getCallGraph();
    machineModel = cfa.getMachineModel();
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    currResult = CPAPhaseEmptyResult.createInstance();
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    // traverse the CFA to find critical sites
    // Note: since the CFA could be non-connected, we should traverse each function head to fully
    // cover all nodes in the CFA

    // sorting function heads by their in-degree
    for (FunctionEntryNode entry : cfa.getAllFunctionHeads()) {
      indegreeMap.put(entry, callGraph.getNumCaller(entry));
      wrappers.put(entry, new FunctionEntryWrapper(entry));
    }
    waitList.addAll(wrappers.values());
    while (!waitList.isEmpty()) {
      FunctionEntryNode nextEntry = waitList.poll().entry;
      CFATraversal.dfs().traverseOnce(nextEntry, new CriticalSiteVisitor());
    }

    System.out.println("CRITICAL: " + criticalSites.size());

    return CPAPhaseStatus.SUCCESS;
  }

  private class CriticalSiteVisitor implements CFAVisitor {

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      if (edge instanceof MultiEdge) {
        for (CFAEdge singleEdge : (MultiEdge) edge) {
          checkEdge(singleEdge);
          checkArraySubscript(singleEdge);
        }
      } else {
        checkEdge(edge);
        checkArraySubscript(edge);
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode node) {
      return TraversalProcess.CONTINUE;
    }

    private void checkEdge(CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {
        case AssumeEdge: {
          CExpression condition = ((CAssumeEdge) pEdge).getExpression();
          if (condition instanceof CBinaryExpression) {
            CExpression op1 = ((CBinaryExpression) condition).getOperand1();
            CExpression op2 = ((CBinaryExpression) condition).getOperand2();
            CType type1 = op1.getExpressionType();
            CType type2 = op2.getExpressionType();
            if (countAllSites || !Types.isEquivalent(type1, type2)) {
              addCriticalSite(condition.getFileLocation());
            } else {
              checkExpression(op1, type1);
              checkExpression(op2, type2);
            }
          }
          break;
        }
        case StatementEdge: {
          CStatement statement = ((CStatementEdge) pEdge).getStatement();
          if (statement instanceof CFunctionCall) {
            CFunctionCallExpression callExp = ((CFunctionCall) statement)
                .getFunctionCallExpression();
            List<CExpression> args = callExp.getParameterExpressions();
            CExpression functionName = callExp.getFunctionNameExpression();
            CType functionType = functionName.getExpressionType();
            if (functionType instanceof CFunctionType) {
              List<CType> parameters = ((CFunctionType) functionType).getParameters();
              if (parameters.size() <= args.size()) {
                for (int i = 0; i < parameters.size(); i++) {
                  checkExpression(args.get(i), parameters.get(i));
                }
              }
            }
          }
          break;
        }
        case ReturnStatementEdge: {
          Optional<CAssignment> orAssign = ((CReturnStatementEdge) pEdge).asAssignment();
          if (orAssign.isPresent()) {
            CAssignment assign = orAssign.get();
            CLeftHandSide lhs = assign.getLeftHandSide();
            CRightHandSide rhs = assign.getRightHandSide();
            if (rhs instanceof CExpression) {
              checkExpression((CExpression) rhs, lhs.getExpressionType());
            }
          }
          break;
        }
        case FunctionCallEdge: {
          CFunctionCallEdge callEdge = (CFunctionCallEdge) pEdge;
          List<CExpression> args = callEdge.getArguments();

          CFunctionEntryNode entryNode = callEdge.getSuccessor();
          // remove visited entry from work list
          FunctionEntryWrapper wrapper = wrappers.get(entryNode);
          if (wrapper != null) {
            waitList.remove(wrapper);
          }

          List<CParameterDeclaration> params = entryNode.getFunctionParameters();
          assert (params.size() <= args.size());
          for (int i = 0; i < params.size(); i++) {
            checkExpression(args.get(i), params.get(i).getType());
          }
          break;
        }
        case FunctionReturnEdge:
        case DeclarationEdge:
        case BlankEdge:
        default:
          // nothing to be checked
      }
    }

    private void checkArraySubscript(CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {
        case AssumeEdge: {
          CExpression condition = ((CAssumeEdge) pEdge).getExpression();
          checkArraySubscriptRecursively(condition);
          break;
        }
        case StatementEdge: {
          CStatement statement = ((CStatementEdge) pEdge).getStatement();
          if (statement instanceof CAssignment) {
            CLeftHandSide lhs = ((CAssignment) statement).getLeftHandSide();
            CRightHandSide rhs = ((CAssignment) statement).getRightHandSide();
            checkArraySubscriptRecursively(lhs);
            checkArraySubscriptRecursively(rhs);
          } else if (statement instanceof CFunctionCall) {
            CFunctionCallExpression call = ((CFunctionCall) statement).getFunctionCallExpression();
            checkArraySubscriptRecursively(call);
          } else {
            assert (statement instanceof CExpressionStatement);
            CExpression exp = ((CExpressionStatement) statement).getExpression();
            checkArraySubscriptRecursively(exp);
          }
          break;
        }
        case DeclarationEdge: {
          CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
          if (declaration instanceof CVariableDeclaration) {
            CVariableDeclaration varDecl = (CVariableDeclaration) declaration;
            CInitializer initializer = varDecl.getInitializer();
            if (initializer != null) {
              checkArraySubscriptRecursively(initializer);
            }
          }
          break;
        }
        case FunctionCallEdge: {
          List<CExpression> args = ((CFunctionCallEdge) pEdge).getArguments();
          for (CExpression arg : args) {
            checkArraySubscriptRecursively(arg);
          }
          break;
        }
        case ReturnStatementEdge: {
          Optional<CExpression> retExp = ((CReturnStatementEdge) pEdge).getExpression();
          if (retExp.isPresent()) {
            checkArraySubscriptRecursively(retExp.get());
          }
          break;
        }
        case FunctionReturnEdge: {
          CFunctionReturnEdge retEdge = (CFunctionReturnEdge) pEdge;
          CFunctionCall call = retEdge.getSummaryEdge().getExpression();
          if (call instanceof CAssignment) {
            CLeftHandSide lhs = ((CAssignment) call).getLeftHandSide();
            checkArraySubscriptRecursively(lhs);
          }
          break;
        }
        default:
      }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkArraySubscriptRecursively(CRightHandSide exp) {
      try {
        exp.accept(new ArraySubscriptVisitor());
      } catch (UnrecognizedCCodeException ignored) {}
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkArraySubscriptRecursively(CInitializer initializer) {
      try {
        initializer.accept(new ArraySubscriptInitializerChecker());
      } catch (UnrecognizedCCodeException ignored) {}
    }

    private void checkExpression(CExpression exp, CType type) {
      // precondition: the specified type is arithmetic
      CSimpleType expType = Types.toIntegerType(exp.getExpressionType());
      CSimpleType targetType = Types.toIntegerType(type);
      if (expType != null && targetType != null) {
        if (Types.canHoldAllValues(targetType, expType, machineModel)) {
          if (countAllSites || isArithmeticOperation(exp)) {
            addCriticalSite(exp.getFileLocation());
          }
        } else {
          addCriticalSite(exp.getFileLocation());
        }
      }
    }

    private boolean isArithmeticOperation(CExpression exp) {
      if (exp instanceof CBinaryExpression) {
        BinaryOperator optr = ((CBinaryExpression) exp).getOperator();
        return (optr == BinaryOperator.PLUS || optr == BinaryOperator.MINUS || optr ==
            BinaryOperator.MULTIPLY || optr == BinaryOperator.DIVIDE || optr == BinaryOperator
            .SHIFT_LEFT);
      } else if (exp instanceof CUnaryExpression) {
        return (((CUnaryExpression) exp).getOperator() == UnaryOperator.MINUS);
      } else if (exp instanceof CIdExpression) {
        String name = ((CIdExpression) exp).getName();
        return name.startsWith("__CPAchecker_TMP_");
      }
      return false;
    }

    private void addCriticalSite(FileLocation pLocation) {
      if (pLocation != null && !pLocation.equals(FileLocation.DUMMY)) {
        criticalSites.add(pLocation);
      }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private class ArraySubscriptVisitor
        extends DefaultCExpressionVisitor<Void, UnrecognizedCCodeException>
        implements CRightHandSideVisitor<Void, UnrecognizedCCodeException> {

      @Override
      public Void visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws UnrecognizedCCodeException {
        List<CExpression> args = pIastFunctionCallExpression.getParameterExpressions();
        for (CExpression arg : args) {
          arg.accept(this);
        }
        return pIastFunctionCallExpression.getFunctionNameExpression().accept(this);
      }

      @Override
      protected Void visitDefault(CExpression exp) throws UnrecognizedCCodeException {
        return null;
      }

      @Override
      public Void visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
        CExpression arrayExp = e.getArrayExpression();
        CExpression indexExp = e.getSubscriptExpression();
        arrayExp.accept(this);
        indexExp.accept(this);
        if (countAllSites || isArithmeticOperation(indexExp)) {
          addCriticalSite(indexExp.getFileLocation());
        }
        return null;
      }

      @Override
      public Void visit(CBinaryExpression e) throws UnrecognizedCCodeException {
        CExpression op1 = e.getOperand1();
        CExpression op2 = e.getOperand2();
        op1.accept(this);
        op2.accept(this);
        return null;
      }

      @Override
      public Void visit(CCastExpression e) throws UnrecognizedCCodeException {
        return e.getOperand().accept(this);
      }

      @Override
      public Void visit(CFieldReference e) throws UnrecognizedCCodeException {
        return e.getFieldOwner().accept(this);
      }

      @Override
      public Void visit(CUnaryExpression e) throws UnrecognizedCCodeException {
        return e.getOperand().accept(this);
      }

      @Override
      public Void visit(CPointerExpression e) throws UnrecognizedCCodeException {
        return e.getOperand().accept(this);
      }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private class ArraySubscriptInitializerChecker implements CInitializerVisitor<Void,
        UnrecognizedCCodeException> {

      @Override
      public Void visit(CInitializerExpression pInitializerExpression)
          throws UnrecognizedCCodeException {
        CExpression exp = pInitializerExpression.getExpression();
        checkArraySubscriptRecursively(exp);
        return null;
      }

      @Override
      public Void visit(CInitializerList pInitializerList) throws UnrecognizedCCodeException {
        for(CInitializer member : pInitializerList.getInitializers()) {
          member.accept(this);
        }
        return null;
      }

      @Override
      public Void visit(CDesignatedInitializer pCStructInitializerPart)
          throws UnrecognizedCCodeException {
        CInitializer rhs = pCStructInitializerPart.getRightHandSide();
        return rhs.accept(this);
      }
    }

  }

  private class FunctionEntryWrapper implements Comparable<FunctionEntryWrapper> {

    private final FunctionEntryNode entry;

    private FunctionEntryWrapper(FunctionEntryNode pEntry) {
      entry = pEntry;
    }

    @Override
    public int compareTo(FunctionEntryWrapper pOther) {
      return indegreeMap.get(entry) - indegreeMap.get(pOther.entry);
    }

    @Override
    public int hashCode() {
      return entry.hashCode();
    }

    @Override
    public boolean equals(Object pO) {
      if (pO == this) {
        return true;
      }
      if (pO == null || !(pO instanceof FunctionEntryWrapper)) {
        return false;
      }
      FunctionEntryWrapper other = (FunctionEntryWrapper) pO;
      return entry.equals(other.entry);
    }
  }

}
