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
package org.sosy_lab.cpachecker.cpa.shape;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithCheck;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerManager;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.cpa.shape.checker.InvalidFreeChecker;
import org.sosy_lab.cpachecker.cpa.shape.checker.InvalidReadChecker;
import org.sosy_lab.cpachecker.cpa.shape.checker.InvalidWriteChecker;
import org.sosy_lab.cpachecker.cpa.shape.checker.MemoryLeakChecker;
import org.sosy_lab.cpachecker.cpa.shape.checker.StackAddressReturnChecker;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicKind;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapePointerAdapter;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapeValueAdapter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.ShapeGeneralLessOrEqual;
import org.sosy_lab.cpachecker.cpa.shape.util.AssumeEvaluator;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SymbolicAssumeInfo;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.DeclaredTypeData;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicExpressionAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ValueAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import javax.annotation.Nullable;

@Options(prefix = "cpa.shape")
public class ShapeTransferRelation extends SingleEdgeTransferRelation
    implements TransferRelationWithCheck {

  @Option(secure = true, description = "check unreachable memory in every function return")
  private boolean checkMemoryLeakAtFrameDrop = true;

  @Option(secure = true, description = "memory that is not freed before the end of a non-return "
      + "function is reported as memory leak defect")
  private boolean checkMemoryLeakForNonReturnFunction = false;

  @Option(secure = true, description = "memory that is not freed before the end of main function."
      + " In general this flag should not be set, because OS is responsible to clean memory for "
      + "terminated program")
  private boolean checkMemoryLeakForMainFunction = false;

  @Option(secure = true, name = "allowAllocFail", description = "if this option is activated, "
      + "failure of memory allocation is simulated")
  private boolean allowAllocationFail = true;

  private final MachineModel machineModel;

  /* ********************* */
  /* constraint processing */
  /* ********************* */

  private Solver solver;
  private FormulaManagerView formulaManager;
  private CtoFormulaConverter converter;

  private CheckerManager<ShapeState> checkerManager;

  public ShapeTransferRelation(
      Configuration pConfig,
      LogManager pLogger,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier,
      Solver pSolver,
      String pMergeType) throws InvalidConfigurationException {
    pConfig.inject(this);
    LogManagerWithoutDuplicates logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    // initialize core shape adapter
    CoreShapeAdapter.initialize(pConfig, machineModel, logger);
    // set the flag that control the allocation failure analysis
    ShapePointerAdapter.instance().setAllocationFail(allowAllocationFail);
    // initialize solver infrastructures
    solver = pSolver;
    formulaManager = solver.getFormulaManager();
    initializeCToFormulaConverter(pLogger, pConfig, pShutdownNotifier);
    // initialize less-or-equal operator
    ShapeGeneralLessOrEqual.initialize(pMergeType, pConfig, solver);
    // initialize checker manager
    checkerManager = new CheckerManager<>(pConfig, ShapeState.class);
    checkerManager.register(InvalidFreeChecker.class);
    checkerManager.register(InvalidReadChecker.class);
    checkerManager.register(InvalidWriteChecker.class);
    checkerManager.register(MemoryLeakChecker.class);
    checkerManager.register(StackAddressReturnChecker.class);
  }

  /**
   * Initialize C-to-formula converter. For inner use only.
   */
  private void initializeCToFormulaConverter(
      LogManager pLogger, Configuration pConfig,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    FormulaEncodingOptions options = new FormulaEncodingOptions(pConfig);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(pLogger, machineModel);
    converter = new CtoFormulaConverter(options, formulaManager, machineModel, Optional
        .<VariableClassification>absent(), pLogger, pShutdownNotifier, typeHandler,
        AnalysisDirection.FORWARD);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    Collection<ShapeState> results;
    if (cfaEdge instanceof MultiEdge) {
      MultiEdge multiEdge = (MultiEdge) cfaEdge;
      Queue<ShapeState> processQueue = new ArrayDeque<>();
      Queue<ShapeState> resultQueue = new ArrayDeque<>();
      processQueue.add((ShapeState) state);
      for (CFAEdge edge : multiEdge) {
        while (!processQueue.isEmpty()) {
          ShapeState nextState = processQueue.poll();
          Collection<ShapeState> singleResults = getAbstractSuccessorsForEdge0(nextState,
              otherStates, edge);
          for (ShapeState singleResult : singleResults) {
            resultQueue.add(singleResult);
          }
        }
        while (!resultQueue.isEmpty()) {
          processQueue.add(resultQueue.poll());
        }
      }
      results = ImmutableSet.copyOf(processQueue);
    } else {
      results = getAbstractSuccessorsForEdge0((ShapeState) state, otherStates, cfaEdge);
    }
    return results;
  }

  private Collection<ShapeState> getAbstractSuccessorsForEdge0(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pCFAEdge)
      throws CPATransferException {
    List<ShapeState> successors;
    switch (pCFAEdge.getEdgeType()) {
      case DeclarationEdge:
        successors = handleDeclaration(pState, pOtherStates, (CDeclarationEdge) pCFAEdge);
        break;
      case StatementEdge:
        successors = handleStatement(pState, pOtherStates, (CStatementEdge) pCFAEdge);
        break;
      case AssumeEdge:
        successors = handleAssumption(pState, pOtherStates, (CAssumeEdge) pCFAEdge);
        break;
      case FunctionCallEdge:
        successors = handleFunctionCall(pState, pOtherStates, (CFunctionCallEdge) pCFAEdge);
        break;
      case FunctionReturnEdge:
        CFunctionReturnEdge returnEdge = (CFunctionReturnEdge) pCFAEdge;
        successors = handleFunctionReturn(pState, pOtherStates, returnEdge);
        if (checkMemoryLeakAtFrameDrop) {
          for (ShapeState successor : successors) {
            successor.pruneUnreachable();
          }
        }
        break;
      case ReturnStatementEdge:
        CReturnStatementEdge retStmtEdge = (CReturnStatementEdge) pCFAEdge;
        successors = handleReturnStatement(pState, pOtherStates, retStmtEdge);
        if (retStmtEdge.getSuccessor().getNumLeavingEdges() == 0) {
          boolean isMain = retStmtEdge.getSuccessor().getEntryNode().getFunctionDefinition().
              getName().equals("main");
          boolean dropFrame = checkMemoryLeakForNonReturnFunction ||
              (isMain && checkMemoryLeakForMainFunction);
          // we only handle non-return function excluding main()
          for (ShapeState successor : successors) {
            if (dropFrame) {
              successor.dropStackFrame();
              if (isMain) {
                // drop global variables
                successor.dropGlobals();
              }
            }
            successor.pruneUnreachable();
          }
        }
        break;
      case BlankEdge: {
        if (!pState.hasInitialized()) {
          initializeSolverEnvironment(pState, pCFAEdge.getPredecessor().getFunctionName());
        }
        successors = ImmutableList.of(pState);
        break;
      }
      default:
        successors = ImmutableList.of(pState);
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }

  /* ******************** */
  /* simple edge handlers */
  /* ******************** */

  private List<ShapeState> handleReturnStatement(
      ShapeState state, List<AbstractState> otherStates,
      CReturnStatementEdge returnEdge) throws CPATransferException {
    CExpression returnExp = returnEdge.getExpression().or(CIntegerLiteralExpression.ZERO);
    CType returnType = CoreShapeAdapter.getType(returnExp);
    SGObject returnField = state.getFunctionReturnObject();
    Optional<CAssignment> returnAssign = returnEdge.asAssignment();
    if (returnAssign.isPresent()) {
      returnType = CoreShapeAdapter.getType(returnAssign.get().getLeftHandSide());
    }
    if (returnField != null) {
      // assign the right-hand-side value to the left-hand-side
      return handleAssignment(state, otherStates, returnEdge, returnField, 0, returnExp,
          returnType);
    }
    // otherwise, the return type is possibly void
    return ImmutableList.of(state);
  }

  private List<ShapeState> handleFunctionReturn(
      ShapeState state, List<AbstractState>
      otherStates, CFunctionReturnEdge returnEdge) throws CPATransferException {
    CFunctionSummaryEdge summaryEdge = returnEdge.getSummaryEdge();
    CFunctionCall summaryExp = summaryEdge.getExpression();
    String callerName = returnEdge.getSuccessor().getFunctionName();
    ShapeState newState = new ShapeState(state);
    if (summaryExp instanceof CFunctionCallAssignmentStatement) {
      CLeftHandSide lE = ((CFunctionCallAssignmentStatement) summaryExp).getLeftHandSide();
      CFunctionCallExpression rE = ((CFunctionCallAssignmentStatement) summaryExp)
          .getRightHandSide();
      CType rType = CoreShapeAdapter.getType(rE);
      // we extract the name expression for faulty expression display
      SymbolicValueAndStateList returnValueAndStates = getFunctionReturnValue(newState,
          otherStates, returnEdge, rType, rE.getFunctionNameExpression());
      List<ShapeState> totalResults = new ArrayList<>();
      for (SymbolicValueAndState returnValueAndState : returnValueAndStates
          .asSymbolicValueAndStateList()) {
        ShapeSymbolicValue returnValue = returnValueAndState.getObject();
        ShapeState nState = returnValueAndState.getShapeState();
        // check if the return value is the address of a stack object
        ShapeAddressValue addressValue = getAddressFromSymbolicValue(nState, returnValue);
        if (!addressValue.isUnknown()) {
          SGObject object = addressValue.getObject();
          if (nState.isStackObject(object)) {
            nState.setStackAddressReturn();
          }
        }
        // alright, now we begin to drop the stack frame
        nState.dropStackFrame();
        List<AddressAndState> leftAddresses = CoreShapeAdapter.getInstance().evaluateAddress
            (nState, otherStates, returnEdge, lE);
        List<ShapeState> results = new ArrayList<>(leftAddresses.size());
        for (AddressAndState addressAndState : leftAddresses) {
          Address address = addressAndState.getObject();
          nState = addressAndState.getShapeState();
          if (!address.isUnknown()) {
            if (returnValue.isUnknown()) {
              // this case should not occur?
              returnValue = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
            }
            SGObject object = address.getObject();
            int offset = address.getOffset().getAsInt();
            ShapeState resultState = assign(nState, otherStates, returnEdge, object, offset,
                returnValue, rType);
            initializeSolverEnvironment(resultState, callerName);
            results.add(resultState);
          } else {
            // ROB-2: a possible buffer overwrite issue
            initializeSolverEnvironment(nState, callerName);
            results.add(nState);
          }
        }
        totalResults.addAll(results);
      }
      return ImmutableList.copyOf(totalResults);
    } else {
      newState.dropStackFrame();
      initializeSolverEnvironment(newState, callerName);
      return ImmutableList.of(newState);
    }
  }

  private List<ShapeState> handleFunctionCall(
      ShapeState state, List<AbstractState> otherStates, CFunctionCallEdge callEdge)
      throws CPATransferException {
    CFunctionEntryNode functionEntry = callEdge.getSuccessor();
    CFunctionDeclaration functionDeclaration = functionEntry.getFunctionDefinition();
    List<CParameterDeclaration> parameters = functionEntry.getFunctionParameters();
    List<CExpression> arguments = callEdge.getArguments();
    if (!functionDeclaration.getType().takesVarArgs()) {
      assert (parameters.size() == arguments.size());
    }
    Map<ShapeState, RegionAndValueList> parameterMap = new HashMap<>();
    ShapeState initialState = new ShapeState(state);
    parameterMap.put(initialState, RegionAndValueList.of());
    for (int i = 0; i < parameters.size(); i++) {
      CExpression arg = arguments.get(i);
      CParameterDeclaration parameter = parameters.get(i);
      CType pType = CoreShapeAdapter.getType(parameter.getType());
      String paramName = parameter.getName();
      SGRegion argObject;
      if (pType instanceof CArrayType) {
        // if the argument has the type T[], then we convert its type to T*
        CPointerType newType = new CPointerType(pType.isConst(), pType.isVolatile(), (
            (CArrayType) pType).getType());
        argObject = new SGRegion(paramName, newType, machineModel.getSizeofPtr(), SGRegion.STATIC);
      } else {
        argObject = new SGRegion(paramName, pType, machineModel.getSizeof(pType), SGRegion.STATIC);
      }
      // traverse parameter mapping to handle each shape state
      Map<ShapeState, RegionAndValueList> newParamMap = new HashMap<>();
      for (Entry<ShapeState, RegionAndValueList> entry : parameterMap.entrySet()) {
        ShapeState newState = entry.getKey();
        RegionAndValueList regionAndValues = entry.getValue();
        // first read the right-hand-side
        SymbolicValueAndStateList valueAndStates = readValueToBeAssignedWithConstraint(newState,
            otherStates, callEdge, arg);
        for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
          ShapeSymbolicValue value = valueAndState.getObject();
          newState = valueAndState.getShapeState();
          // UPDATE: we have assigned explicit value to symbolic value if possible, in
          // `readValueToBeAssignedWithConstraint()` method
          RegionAndValueList newRegionAndValues = RegionAndValueList.copyOf(regionAndValues);
          newRegionAndValues.add(RegionAndValue.valueOf(argObject, (KnownSymbolicValue) value));
          newParamMap.put(newState, newRegionAndValues);
        }
      }
      // update the original parameter map
      parameterMap.clear();
      parameterMap.putAll(newParamMap);
    }
    // construct the new stack frame
    List<ShapeState> results = new ArrayList<>(parameterMap.size());
    for (Entry<ShapeState, RegionAndValueList> entry : parameterMap.entrySet()) {
      ShapeState newState = entry.getKey();
      RegionAndValueList valueList = entry.getValue();
      assert (valueList.size() == parameters.size());
      newState.addStackFrame(functionDeclaration);
      for (int i = 0; i < parameters.size(); i++) {
        CType pType = CoreShapeAdapter.getType(parameters.get(i).getType());
        if (pType instanceof CArrayType) {
          pType = new CPointerType(pType.isConst(), pType.isVolatile(), ((CArrayType) pType)
              .getType());
        }
        RegionAndValue regionAndValue = valueList.get(i);
        SGRegion region = regionAndValue.getRegion();
        KnownSymbolicValue value = regionAndValue.getValue();
        newState.addLocalVariable(region);
        newState = assign(newState, otherStates, callEdge, region, 0, value, pType);
      }
      // initialize solver environment
      initializeSolverEnvironment(newState, functionEntry.getFunctionName());
      results.add(newState);
    }
    return results;
  }

  private List<ShapeState> handleAssumption(
      ShapeState pState, List<AbstractState> pOtherStates,
      CAssumeEdge assumeEdge) throws CPATransferException {
    CExpression assumeExp = assumeEdge.getExpression();
    boolean truthValue = assumeEdge.getTruthAssumption();
    ShapeState newState = new ShapeState(pState);
    SymbolicAssumeInfo result = CoreShapeAdapter.getInstance().symbolizeAssumption(newState,
        pOtherStates, assumeEdge, assumeExp, true, true);
    List<ShapeState> totalResult = new ArrayList<>();
    for (int i = 0; i < result.size(); i++) {
      SymbolicExpression se = result.getSymbolicExpression(i);
      AssumeEvaluator evaluator = result.getAssumeEvaluator(i);
      newState = result.getState(i);
      // derive value of condition expression
      if (se instanceof ConstantSE) {
        ConstantSE constSE = (ConstantSE) se;
        switch (constSE.getValueKind()) {
          case SYMBOLIC: {
            KnownSymbolicValue symValue = (KnownSymbolicValue) constSE.getValue();
            if ((truthValue && symValue.equals(KnownSymbolicValue.TRUE)) ||
                (!truthValue && symValue.equals(KnownSymbolicValue.FALSE))) {
              totalResult.add(newState);
              continue;
            }
            if ((truthValue && symValue.equals(KnownSymbolicValue.FALSE)) ||
                (!truthValue && symValue.equals(KnownSymbolicValue.TRUE))) {
              continue;
            }
            break;
          }
          case EXPLICIT: {
            KnownExplicitValue expValue = (KnownExplicitValue) constSE.getValue();
            if (truthValue) {
              if (expValue.equals(KnownExplicitValue.ZERO)) {
                continue;
              } else {
                totalResult.add(newState);
                continue;
              }
            } else {
              if (expValue.equals(KnownExplicitValue.ZERO)) {
                totalResult.add(newState);
                continue;
              } else {
                continue;
              }
            }
          }
          default:
            break;
        }
      }
      // otherwise, we cannot determine the condition value
      totalResult.addAll(deriveFurtherInformationFromAssumption(newState, pOtherStates, assumeEdge,
          truthValue, se, evaluator));
    }
    return totalResult;
  }

  private List<ShapeState> handleDeclaration(
      ShapeState pState, List<AbstractState> pOtherStates, CDeclarationEdge declarationEdge)
      throws CPATransferException {
    CDeclaration declaration = declarationEdge.getDeclaration();
    if (!(declaration instanceof CVariableDeclaration)) {
      return ImmutableList.of(pState);
    }
    ShapeState newState = new ShapeState(pState);
    return handleVariableDeclaration(newState, pOtherStates, (CVariableDeclaration) declaration,
        declarationEdge);
  }

  private List<ShapeState> handleStatement(
      ShapeState pState, List<AbstractState> pOtherStates,
      CStatementEdge pEdge) throws CPATransferException {
    CStatement statement = pEdge.getStatement();
    List<ShapeState> results;
    ShapeState newState = new ShapeState(pState);
    if (statement instanceof CAssignment) {
      CAssignment assignment = (CAssignment) statement;
      CLeftHandSide lE = assignment.getLeftHandSide();
      CRightHandSide rE = assignment.getRightHandSide();
      results = handleAssignment(newState, pOtherStates, pEdge, lE, rE);
    } else if (statement instanceof CFunctionCallStatement) {
      CFunctionCallExpression callExpression = ((CFunctionCallStatement) statement)
          .getFunctionCallExpression();
      // FIX: invoke pointer adapter or value adapter?
      // Pointer adapter mainly contains memory manipulation methods returning pointer, while
      // value adapter contains both memory operations and arithmetic operations (and they return
      // arithmetic values)
      if (ShapeValueAdapter.instance().isRegistered(callExpression)) {
        List<ValueAndState> valueAndStates = ShapeValueAdapter.instance()
            .evaluateFunctionCallExpression(callExpression, newState, pOtherStates, pEdge);
        results = FluentIterable.from(valueAndStates).transform(
            new Function<ValueAndState, ShapeState>() {
              @Override
              public ShapeState apply(ValueAndState pValueAndState) {
                return pValueAndState.getShapeState();
              }
            }).toList();
      } else {
        // other unknown functions are also handled by pointer adapter
        AddressValueAndStateList valueAndStates = ShapePointerAdapter.instance()
            .evaluateFunctionCallExpression(callExpression, newState, pOtherStates, pEdge);
        results = FluentIterable.from(valueAndStates.asAddressValueAndStateList()).transform(
            new Function<AddressValueAndState, ShapeState>() {
              @Override
              public ShapeState apply(AddressValueAndState pAddressValueAndState) {
                return pAddressValueAndState.getShapeState();
              }
            }).toList();
      }
    } else {
      results = ImmutableList.of(newState);
    }
    return results;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  /**
   * Handle the assignment given the left/right-hand-sides. Note that this method is different
   * with the below one because we have to determine the memory region to be written by
   * evaluating the left-hand-side expression.
   */
  private List<ShapeState> handleAssignment(
      ShapeState newState, List<AbstractState>
      otherStates, CFAEdge cfaEdge, CLeftHandSide
          lE, CRightHandSide rE) throws CPATransferException {
    ShapeState rState = newState;
    List<ShapeState> results = new ArrayList<>();
    List<AddressAndState> leftAddresses = CoreShapeAdapter.getInstance().evaluateAddress(rState,
        otherStates, cfaEdge, lE);
    for (AddressAndState addressAndState : leftAddresses) {
      Address address = addressAndState.getObject();
      rState = addressAndState.getShapeState();
      CType leftType = CoreShapeAdapter.getType(lE);
      if (address.isUnknown()) {
        // now we do not know where to write the new value
        // it is appropriate to simply set the INVALID_WRITE flag?
        // FIX: If we have arr[x] = y where arr[x] is not a valid read by constraint solving,
        // then we should not set the invalid write here.
        results.add(rState);
      } else {
        List<ShapeState> newStates = handleAssignment(rState, otherStates, cfaEdge, address
            .getObject(), address.getOffset().getAsInt(), rE, leftType);
        results.addAll(newStates);
      }
    }
    return results;
  }

  /**
   * Handle assignment of a right-hand-side expression to the specified memory region.
   */
  private List<ShapeState> handleAssignment(
      ShapeState newState, List<AbstractState> otherStates,
      CFAEdge cfaEdge, SGObject object, int offset,
      CRightHandSide rE, CType lType)
      throws CPATransferException {
    ShapeState rState = new ShapeState(newState);
    return assign(rState, otherStates, cfaEdge, object, offset, rE, lType);
  }

  /**
   * Perform assignment.
   *
   * @param newState    a shape state to be updated
   * @param otherStates other state components
   * @param cfaEdge     CFA edge
   * @param object      target memory object
   * @param offset      offset of target memory object
   * @param newValue    new symbolic value to be assigned
   * @param rightType   the expression type of right-hand-side
   * @return new shape state after assignment
   */
  private ShapeState assign(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge cfaEdge,
      SGObject object, int offset, ShapeSymbolicValue newValue, CType rightType)
      throws UnrecognizedCCodeException {
    if (CoreShapeAdapter.isStructOrUnion(rightType)) {
      // for structure or union, we copy data blocks to perform assignment
      return assignStructure(newState, otherStates, cfaEdge, object, offset, newValue, rightType);
    } else {
      // the data size should be determined in this case
      return CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge, object,
          KnownExplicitValue.valueOf(offset), rightType, newValue);
    }
  }

  /**
   * Perform assignment. Different from the above assign(), this method assign the value of a
   * right-hand-side expression to a specified memory region. We provide the type of
   * left-hand-side which determines the type of the whole assignment.
   *
   * @param newState    a shape state to be updated
   * @param otherStates other state components
   * @param cfaEdge     a CFA edge
   * @param object      target memory object
   * @param offset      the offset in target memory object
   * @param rE          right-hand-side expression
   * @param leftType    the type for assignment
   * @return new shape states after assignment
   */
  private List<ShapeState> assign(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge
      cfaEdge, SGObject object, int offset, CRightHandSide rE, CType leftType)
      throws CPATransferException {
    List<ShapeState> results = new ArrayList<>();
    SymbolicValueAndStateList valueAndStateList = readValueToBeAssignedWithConstraint(newState,
        otherStates, cfaEdge, rE);
    for (SymbolicValueAndState valueAndState : valueAndStateList.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue value = valueAndState.getObject();
      ShapeState rState = valueAndState.getShapeState();
      rState = assign(rState, otherStates, cfaEdge, object, offset, value, leftType);
      results.add(rState);
    }
    return results;
  }

  /**
   * Perform initializing assignment for string literal values.
   *
   * @param newState a shape state to be updated
   * @param cfaEdge  a CFA edge
   * @param object   target memory object
   * @param offset   the offset in the target memory object
   * @param string   string literal expression
   * @return new shape state after assignment
   */
  private List<ShapeState> assign(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge
      cfaEdge, SGObject object, int offset, CStringLiteralExpression string)
      throws CPATransferException {
    List<ShapeState> results = new ArrayList<>();
    int contentLength = string.getContentString().length() + 1;
    SymbolicValueAndStateList valueAndStates = readValueToBeAssigned(newState, otherStates,
        cfaEdge, string);
    for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue value = valueAndState.getObject();
      ShapeState rState = valueAndState.getShapeState();
      rState = assign(rState, otherStates, cfaEdge, object, offset, value,
          new CArrayType(false, false, CNumericTypes.CHAR,
              new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT,
                  BigInteger.valueOf(contentLength))));
      results.add(rState);
    }
    return results;
  }

  /**
   * Perform assignment for structure or union data.
   */
  private ShapeState assignStructure(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge
      cfaEdge, SGObject object, int offset, ShapeSymbolicValue newValue, CType rightType)
      throws UnrecognizedCCodeException {
    // the structure value is evaluated as address value
    if (newValue instanceof KnownAddressValue) {
      KnownAddressValue address = (KnownAddressValue) newValue;
      SGObject sourceObject = address.getObject();
      // known address value always has valid offset
      int sourceOffset = address.getOffset().getAsInt();
      int size = CoreShapeAdapter.getInstance().evaluateSizeof(newState, otherStates, cfaEdge,
          rightType);
      // check for possible overwrite
      // the size to be copied is certain
      ShapeExplicitValue targetSize = object.getSize();
      if (offset < 0 || (!object.equals(SGObject.getNullObject()) && !targetSize
          .isUnknown() && offset + size > targetSize.getAsInt())) {
        return newState.setInvalidWrite();
      }
      return newState.copy(sourceObject, object, sourceOffset, size, offset);
    }
    return newState;
  }

  /**
   * Read symbolic value from right-hand-side. If the result is unknown value (this is very
   * common if the right-hand-side is a binary arithmetic operation such as n1 + n2), we create a
   * new symbolic value for creating a new association between symbolic value and explicit value.
   */
  private SymbolicValueAndStateList readValueToBeAssigned(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge cfaEdge, CRightHandSide rE)
      throws CPATransferException {
    SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance()
        .evaluateSymbolicValue(newState, otherStates, cfaEdge, rE);
    List<SymbolicValueAndState> results = new ArrayList<>(valueAndStates.size());
    for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue value = valueAndState.getObject();
      ShapeState newerState = valueAndState.getShapeState();
      if (value.isUnknown()) {
        value = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
        if (rE instanceof CLeftHandSide) {
          // FIX: if the right-hand-side has determined address, we should insert a has-value edge
          List<AddressAndState> addresses = CoreShapeAdapter.getInstance().evaluateAddress
              (newerState, otherStates, cfaEdge, (CLeftHandSide) rE);
          boolean hasKnownAddress = false;
          for (AddressAndState address : addresses) {
            Address addr = address.getObject();
            ShapeState nState = address.getShapeState();
            if (!addr.isUnknown()) {
              hasKnownAddress = true;
              nState = CoreShapeAdapter.getInstance().writeValue(nState, otherStates, cfaEdge, addr
                  .getObject(), addr.getOffset(), CoreShapeAdapter.getType(rE), value);
              results.add(SymbolicValueAndState.of(nState, value));
            }
          }
          if (!hasKnownAddress) {
            // if the address is unknown, we should add at least one value+state as a result
            results.add(SymbolicValueAndState.of(newerState, value));
          }
        } else {
          // this case applies for non-left-hand-side expressions such as a + b
          results.add(SymbolicValueAndState.of(newerState, value));
        }
      } else {
        // if the symbolic value is known, we directly use the value as the result
        results.add(valueAndState);
      }
    }
    return SymbolicValueAndStateList.copyOfValueList(results);
  }

  /**
   * Read the symbolic value of the expression to be assigned.
   */
  private SymbolicValueAndStateList readValueToBeAssignedWithConstraint(
      ShapeState newState, List<AbstractState> otherStates, CFAEdge cfaEdge, CRightHandSide rE)
      throws CPATransferException {
    List<SymbolicExpressionAndState> seAndStates = CoreShapeAdapter.getInstance()
        .evaluateSymbolicExpression(newState, otherStates, cfaEdge, rE);
    List<SymbolicValueAndState> results = new ArrayList<>(seAndStates.size());
    for (SymbolicExpressionAndState seAndState : seAndStates) {
      SymbolicExpression se = seAndState.getObject();
      ShapeState nState = seAndState.getShapeState();
      boolean newValueCreated = false;
      KnownSymbolicValue sv;
      if (se instanceof ConstantSE) {
        switch (se.getValueKind()) {
          case SYMBOLIC: {
            sv = (KnownSymbolicValue) se.getValue();
            break;
          }
          case EXPLICIT: {
            KnownExplicitValue ev = (KnownExplicitValue) se.getValue();
            // check if this explicit value has been associated with a known symbolic value
            ShapeSymbolicValue tsv = nState.getSymbolic(ev);
            if (tsv.isUnknown()) {
              // then, we should create a fresh symbolic value
              sv = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
              newValueCreated = true;
              // this must be a fresh explicit mapping
              nState.putExplicitValue(sv, ev);
              // add a binding relation between the new symbolic value and the explicit value
              nState.addConstraint(SEs.bind(sv, se));
            } else {
              sv = (KnownSymbolicValue) tsv;
            }
            break;
          }
          default: {
            sv = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
            newValueCreated = true;
          }
        }
      } else {
        sv = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
        newValueCreated = true;
        // associate the new value with the existing symbolic expression
        SymbolicExpression binding = SEs.bind(sv, se);
        nState.addConstraint(binding);
      }
      // If we create a fresh symbolic value, it is necessary to bind the new value to certain
      // object when the right-hand-side is actually a left-hand-side expression.
      if (newValueCreated) {
        if (rE instanceof CLeftHandSide) {
          List<AddressAndState> addresses = CoreShapeAdapter.getInstance().evaluateAddress
              (nState, otherStates, cfaEdge, (CLeftHandSide) rE);
          boolean hasKnownAddress = false;
          for (AddressAndState addressState : addresses) {
            Address address = addressState.getObject();
            ShapeState aState = addressState.getShapeState();
            if (!address.isUnknown()) {
              hasKnownAddress = true;
              aState = CoreShapeAdapter.getInstance().writeValue(aState, otherStates, cfaEdge,
                  address.getObject(), address.getOffset(), CoreShapeAdapter.getType(rE), sv);
              results.add(SymbolicValueAndState.of(aState, sv));
            }
          }
          // If none of possible addresses are known, we simply return the symbolic result along
          // with the original state.
          if (!hasKnownAddress) {
            results.add(SymbolicValueAndState.of(nState, sv));
          }
          continue;
        }
      }
      // for other cases that no association creation is required
      results.add(SymbolicValueAndState.of(nState, sv));
    }
    return SymbolicValueAndStateList.copyOfValueList(results);
  }

  /**
   * Get the function return value from a specified shape state (this state contains call stack
   * information and the derived return value should correspond to the topmost function call)
   */
  private SymbolicValueAndStateList getFunctionReturnValue(
      ShapeState state, List<AbstractState>
      otherStates, CFAEdge cfaEdge, CType returnType, CExpression returnExp)
      throws UnrecognizedCCodeException {
    SGObject returnObject = state.getFunctionReturnObject();
    return CoreShapeAdapter.getInstance().readValue(state, otherStates, cfaEdge, returnObject,
        KnownExplicitValue.ZERO, returnType, returnExp);
  }

  /**
   * When the assume visitor cannot derives a determined symbolic value from an assumption
   * expression, we can: (1) use explicit visitor; (2) derive some information from comparison
   * result. For example, x > y implies that the symbolic values of x and y are not equal.
   */
  @SuppressWarnings("unused")
  private List<ShapeState> deriveFurtherInformationFromAssumption(
      ShapeState state, List<AbstractState> otherStates, CAssumeEdge pEdge, boolean truthValue,
      SymbolicExpression se, @Nullable AssumeEvaluator evaluator) throws CPATransferException {
    ShapeState newState = new ShapeState(state);
    if (evaluator != null) {
      boolean impliesEq = evaluator.impliesEqOn(truthValue, newState);
      boolean impliesNeq = evaluator.impliesNeqOn(truthValue, newState);
      ShapeSymbolicValue sV1, sV2;
      if (impliesEq || impliesNeq) {
        sV1 = evaluator.getImpliedValue1(newState);
        sV2 = evaluator.getImpliedValue2(newState);
      } else {
        sV1 = UnknownValue.getInstance();
        sV2 = UnknownValue.getInstance();
      }
      if (!sV1.isUnknown() && !sV2.isUnknown()) {
        if (impliesEq) {
          KnownSymbolicValue ksv1 = (KnownSymbolicValue) sV1;
          KnownSymbolicValue ksv2 = (KnownSymbolicValue) sV2;
          // FIX: two pointer values are merged only if they point to the same object
          // Invariant: there would not be two values v1 and v2 such that (1) v1 \= v2, (2) v1
          // and v2 point to the same object with the same offset.
          long v1 = ksv1.getAsLong();
          long v2 = ksv2.getAsLong();
          if (v1 != v2 && newState.getPointer(v1) != null && newState.getPointer(v2) != null) {
            return Collections.emptyList();
          }
          newState.identifyEqualValues(ksv1, ksv2);
          // In this case, it is unnecessary to generate a constraint because equalities are
          // captured by equivalence relation in the state.
        } else if (impliesNeq) {
          newState.identifyInequalValues((KnownSymbolicValue) sV1, (KnownSymbolicValue) sV2);
        }
      }
    }
    // furthermore, we can derive explicit value for symbolic value when the assumption
    // expression is EQ or NEQ.
    if (se instanceof BinarySE) {
      SymbolicExpression operand1 = ((BinarySE) se).getOperand1();
      SymbolicExpression operand2 = ((BinarySE) se).getOperand2();
      BinaryOperator operator = ((BinarySE) se).getOperator();
      KnownSymbolicValue symValue = null;
      KnownExplicitValue expValue = null;
      if (operand1.getValueKind() == SymbolicKind.SYMBOLIC && operand2.getValueKind() ==
          SymbolicKind.EXPLICIT) {
        symValue = (KnownSymbolicValue) operand1.getValue();
        expValue = (KnownExplicitValue) operand2.getValue();
      } else if (operand1.getValueKind() == SymbolicKind.EXPLICIT && operand2.getValueKind() ==
          SymbolicKind.SYMBOLIC) {
        symValue = (KnownSymbolicValue) operand2.getValue();
        expValue = (KnownExplicitValue) operand1.getValue();
      }
      if (symValue != null && expValue != null) {
        if (truthValue) {
          if (operator == BinaryOperator.EQUALS) {
            newState.putExplicitValue(symValue, expValue);
          }
        } else {
          if (operator == BinaryOperator.NOT_EQUALS) {
            newState.putExplicitValue(symValue, expValue);
          }
        }
      }
    } else if (se instanceof ConstantSE) {
      // Finally, we generate constraints and check whether current path is feasible.
      // an UNKNOWN constant symbolic expression could be transformed into null pointer
      // Note: if such case occurs, the program code possibly has other kinds of defects, such as
      // use of uninitialized variable.
      assert (((ConstantSE) se).isUnknown());
      return Lists.newArrayList(newState);
    }

    // generate appropriate constraint according to truth value
    if (!truthValue) {
      assert (se instanceof BinarySE) : "assumption constraint should be binary";
      BinarySE binarySE = (BinarySE) se;
      BinaryOperator operator = binarySE.getOperator();
      operator = operator.getOppositeLogicalOperator();
      se = new BinarySE(binarySE.getOperand1(), binarySE.getOperand2(), operator,
          binarySE.getType(), binarySE.getOriginalExpression());
    }
    newState.addConstraint(se);
    try {
      boolean sat = newState.checkSat();
      if (sat) {
        return Lists.newArrayList(newState);
      } else {
        return Collections.emptyList();
      }
    } catch (SolverException e) {
      throw new CPATransferException("Error in solving assumption constraints", e);
    } catch (InterruptedException e) {
      throw new CPATransferException("Solving assumption constraints interrupted", e);
    }
  }

  /**
   * Create a formula creator for constraint processing.
   * Note: we frequently create formula creator because the function name changes when the
   * analysis point reaches scopes of different functions.
   */
  private FormulaCreator getFormulaCreator(String pFunctionName) {
    return new FormulaCreator(formulaManager, converter, pFunctionName);
  }

  private void initializeSolverEnvironment(ShapeState pState, String functionName) {
    FormulaCreator formulaCreator = getFormulaCreator(functionName);
    pState.initialize(solver, formulaManager, formulaCreator);
  }

  /**
   * Analyze variable declaration with possible initializer.
   */
  private List<ShapeState> handleVariableDeclaration(
      ShapeState pState, List<AbstractState>
      otherStates, CVariableDeclaration varDecl, CDeclarationEdge pEdge)
      throws CPATransferException {
    CInitializer initializer = varDecl.getInitializer();
    String name = varDecl.getName();
    CType type = CoreShapeAdapter.getType(varDecl.getType());
    // the following method returns non-null result when we have already seen the declaration,
    // for example in loop structure
    List<ShapeState> results = new ArrayList<>();

    // calculate the size of new memory object
    List<DeclaredTypeData> sizeInfo = CoreShapeAdapter.getInstance().evaluateDeclaredSizeof
        (pState, otherStates, pEdge, type);
    for (DeclaredTypeData sizeData : sizeInfo) {
      ShapeState newState = sizeData.getState();
      SymbolicExpression sizeSe = sizeData.getSize();
      boolean isVLA = sizeData.isContainsVLA();
      CType trueType = sizeData.getTrueType();
      // this case occurs when, for example, a negative array length is encountered
      if (newState.getInvalidWriteStatus() || trueType instanceof CProblemType) {
        results.add(newState);
        continue;
      }
      ShapeExplicitValue eSize = SEs.isExplicit(sizeSe) ? (KnownExplicitValue) sizeSe.getValue()
                                                        : UnknownValue.getInstance();
      SGObject newObject;
      if (varDecl.isGlobal()) {
        // global objects are zero-initialized by default
        newObject = pState.addGlobalVariable(trueType, eSize, name);
      } else {
        // duplicated local memory object is removed automatically
        if (isVLA) {
          newObject = pState.addLocalVariableForVLA(trueType, eSize, sizeSe, name);
        } else {
          newObject = pState.addLocalVariable(trueType, eSize, name);
        }
      }
      if (initializer != null) {
        results.addAll(handleInitializer(newState, otherStates, pEdge, varDecl, newObject, 0,
            trueType,
            initializer));
      } else {
        results.add(newState);
      }
    }
    return Collections.unmodifiableList(results);
  }

  /**
   * Handle initializer. At the topmost level, no designated initializer should occur.
   */
  private List<ShapeState> handleInitializer(
      ShapeState pState, List<AbstractState> pOtherStates, CDeclarationEdge pEdge,
      CVariableDeclaration declaration, SGObject pObject,
      int pOffset, CType pType, CInitializer pInitializer) throws CPATransferException {
    if (pInitializer instanceof CInitializerExpression) {
      CExpression rightExpression = ((CInitializerExpression) pInitializer).getExpression();
      if (rightExpression instanceof CStringLiteralExpression) {
        // assigning a string literal to a variable is only possible on initialization
        return assign(pState, pOtherStates, pEdge, pObject, pOffset, (CStringLiteralExpression)
            rightExpression);
      }
      return assign(pState, pOtherStates, pEdge, pObject, pOffset, rightExpression, pType);
    } else if (pInitializer instanceof CInitializerList) {
      return handleInitializerList(pState, pOtherStates, pEdge, declaration, pObject, pOffset,
          pType, (CInitializerList) pInitializer);
    } else {
      throw new AssertionError("Designated error should only occur in initializer list");
    }
  }

  /**
   * Handle initializer list.
   */
  private List<ShapeState> handleInitializerList(
      ShapeState pState, List<AbstractState> pOtherStates, CDeclarationEdge pEdge,
      CVariableDeclaration declaration, SGObject pObject, int pOffset, CType pLeftType,
      CInitializerList pList) throws CPATransferException {
    if (pLeftType instanceof CArrayType) {
      return handleArrayInitializerList(pState, pOtherStates, pEdge, declaration, pObject, pOffset,
          (CArrayType) pLeftType, pList);
    } else if (pLeftType instanceof CCompositeType) {
      return handleStructInitializerList(pState, pOtherStates, pEdge, declaration, pObject, pOffset,
          (CCompositeType) pLeftType, pList);
    } else {
      // otherwise, the type cannot be resolved
      return ImmutableList.of(pState);
    }
  }

  /**
   * Handle initializer list for array type.
   */
  private List<ShapeState> handleArrayInitializerList(
      ShapeState pState, List<AbstractState> pOtherStates, CDeclarationEdge pEdge,
      CVariableDeclaration declaration, SGObject pObject, int pOffset,
      CArrayType arrayType, CInitializerList pList) throws CPATransferException {
    CType elementType = arrayType.getType();
    int sizeofElement = CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates, pEdge,
        elementType);
    List<StateAndCounter> results = new ArrayList<>();
    results.add(StateAndCounter.valueOf(pState, 0));
    for (CInitializer initializer : pList.getInitializers()) {
      if (initializer instanceof CDesignatedInitializer) {
        // we should individually handle designated initializer for array
        CInitializer subInitializer = ((CDesignatedInitializer) initializer).getRightHandSide();
        List<StateAndCounter> subResults = new ArrayList<>();
        for (StateAndCounter stateAndCounter : results) {
          ShapeState newState = stateAndCounter.getState();
          DesignatorPositionList positions = handleDesignatedInitializer(newState,
              pOtherStates, pEdge, arrayType, null,
              ((CDesignatedInitializer) initializer).getDesignators());
          int counter = positions.getNewCounter();
          // Derived positions from one state share the same new counter value
          for (DesignatorPosition position : positions.getPositionList()) {
            List<ShapeState> newStates = handleInitializer(newState, pOtherStates, pEdge,
                declaration, pObject, position.getOffset(), position.getType(), subInitializer);
            for (ShapeState state : newStates) {
              subResults.add(StateAndCounter.valueOf(state, counter));
            }
          }
        }
        results = subResults;
      } else {
        List<StateAndCounter> subResults = new ArrayList<>();
        for (StateAndCounter stateAndCounter : results) {
          ShapeState newState = stateAndCounter.getState();
          int counter = stateAndCounter.getCounter();
          int offset = pOffset + sizeofElement * counter;
          List<ShapeState> newStates = handleInitializer(newState, pOtherStates, pEdge, declaration,
              pObject,
              offset, elementType, initializer);
          counter++;
          for (ShapeState state : newStates) {
            subResults.add(StateAndCounter.valueOf(state, counter));
          }
        }
        results = subResults;
      }
    }
    return projectStateAndCounter(results);
  }

  private List<ShapeState> handleStructInitializerList(
      ShapeState pState, List<AbstractState> pOtherStates, CDeclarationEdge pEdge,
      CVariableDeclaration declaration, SGObject pObject, int pOffset, CCompositeType compositeType,
      CInitializerList pList) throws CPATransferException {
    List<StateAndCounter> results = new ArrayList<>();
    List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
    List<Integer> offsetList = generateOffsetList(pState, pOtherStates, pEdge, members);
    results.add(StateAndCounter.valueOf(pState, 0));
    for (CInitializer initializer : pList.getInitializers()) {
      if (initializer instanceof CDesignatedInitializer) {
        CInitializer rightHand = ((CDesignatedInitializer) initializer).getRightHandSide();
        List<StateAndCounter> subResults = new ArrayList<>();
        for (StateAndCounter stateAndCounter : results) {
          ShapeState newState = stateAndCounter.getState();
          DesignatorPositionList positions = handleDesignatedInitializer(newState,
              pOtherStates, pEdge, null, compositeType,
              ((CDesignatedInitializer) initializer).getDesignators());
          int counter = positions.getNewCounter();
          for (DesignatorPosition position : positions.getPositionList()) {
            List<ShapeState> newStates = handleInitializer(newState, pOtherStates, pEdge,
                declaration, pObject,
                position.getOffset(), position.getType(), rightHand);
            for (ShapeState state : newStates) {
              subResults.add(StateAndCounter.valueOf(state, counter));
            }
          }
        }
        results = subResults;
      } else {
        // initialization of structure also allows non-designated initializer
        List<StateAndCounter> subResults = new ArrayList<>();
        for (StateAndCounter stateAndCounter : results) {
          ShapeState newState = stateAndCounter.getState();
          int counter = stateAndCounter.getCounter();
          if (counter >= members.size()) {
            // directly return the previous result
            return projectStateAndCounter(results);
          }
          int offset = pOffset + offsetList.get(counter);
          CType type = members.get(counter).getType();
          List<ShapeState> newStates = handleInitializer(newState, pOtherStates, pEdge,
              declaration, pObject, offset, type, initializer);
          counter++;
          for (ShapeState state : newStates) {
            subResults.add(StateAndCounter.valueOf(state, counter));
          }
        }
        results = subResults;
      }
    }
    return projectStateAndCounter(results);
  }

  private List<ShapeState> projectStateAndCounter(List<StateAndCounter> pList) {
    return FluentIterable.from(pList).transform(new Function<StateAndCounter, ShapeState>() {
      @Override
      public ShapeState apply(StateAndCounter pStateAndCounter) {
        return pStateAndCounter.getState();
      }
    }).toList();
  }

  /**
   * Calculate offset for each field in struct type.
   */
  private List<Integer> generateOffsetList(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pEdge,
      List<CCompositeTypeMemberDeclaration> pMembers)
      throws CPATransferException {
    List<Integer> offsetList = new ArrayList<>(pMembers.size());
    offsetList.add(0);
    int currentOffset = 0;
    for (int i = 0; i < pMembers.size() - 1; i++) {
      CType memberType = pMembers.get(i).getType();
      currentOffset += CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates, pEdge,
          memberType);
      offsetList.add(currentOffset);
    }
    Preconditions.checkArgument(offsetList.size() == pMembers.size());
    return offsetList;
  }

  /**
   * Calculate offset and type of corresponding memory region given a designator.
   */
  private DesignatorPositionList handleDesignatedInitializer(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CDeclarationEdge pEdge,
      @Nullable CArrayType arrayType,
      @Nullable CCompositeType compositeType,
      List<CDesignator> pDesignators)
      throws CPATransferException {
    List<DesignatorPosition> positions = new ArrayList<>();
    int newCounter = -1;
    CType subType = null;
    List<CDesignator> designators = new ArrayList<>(pDesignators);
    CDesignator designator = designators.get(0);
    if (designator instanceof CArrayDesignator) {
      Preconditions.checkNotNull(arrayType);
      subType = arrayType.getType();
      CExpression indexExp = ((CArrayDesignator) designator).getSubscriptExpression();
      ShapeExplicitValue indexValue = CoreShapeAdapter.getInstance()
          .evaluateSingleExplicitValue(pState, pOtherStates, pEdge, indexExp);
      int elementSize = CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates, pEdge,
          subType);
      if (!indexValue.isUnknown()) {
        int index = indexValue.getAsInt();
        newCounter = index + 1;
        positions.add(DesignatorPosition.valueOf(index * elementSize, subType));
      }
    } else if (designator instanceof CArrayRangeDesignator) {
      Preconditions.checkNotNull(arrayType);
      subType = arrayType.getType();
      CExpression floorExp = ((CArrayRangeDesignator) designator).getFloorExpression();
      CExpression ceilExp = ((CArrayRangeDesignator) designator).getCeilExpression();
      ShapeExplicitValue floorValue = CoreShapeAdapter.getInstance().evaluateSingleExplicitValue
          (pState, pOtherStates, pEdge, floorExp);
      ShapeExplicitValue ceilValue = CoreShapeAdapter.getInstance().evaluateSingleExplicitValue
          (pState, pOtherStates, pEdge, ceilExp);
      int elementSize = CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates,
          pEdge, subType);
      if (!floorValue.isUnknown() && !ceilValue.isUnknown()) {
        int floor = floorValue.getAsInt();
        int ceil = ceilValue.getAsInt();
        newCounter = ceil + 1;
        for (int i = floor; i <= ceil; i++) {
          positions.add(DesignatorPosition.valueOf(i * elementSize, subType));
        }
      }
    } else {
      Preconditions.checkArgument(designator instanceof CFieldDesignator);
      Preconditions.checkNotNull(compositeType);
      String name = ((CFieldDesignator) designator).getFieldName();
      int offset = 0;
      newCounter = -1;
      for (CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
        CType currentType = member.getType();
        newCounter++;
        if (member.getName().equals(name)) {
          positions.add(DesignatorPosition.valueOf(offset, currentType));
          subType = currentType;
          break;
        }
        offset += CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates, pEdge,
            currentType);
      }
    }
    if (positions.isEmpty() || subType == null || newCounter < 0) {
      return DesignatorPositionList.of();
    }
    // then, we handle the remaining designators
    designators.remove(0);
    positions = handleDesignators(pState, pOtherStates, pEdge, Types.extractArrayType(subType),
        Types.extractCompositeType(subType), designators, positions);
    return DesignatorPositionList.of(positions, newCounter);
  }

  /**
   * Handle designators and extract the position information.
   */
  private List<DesignatorPosition> handleDesignators(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CDeclarationEdge pEdge,
      @Nullable CArrayType arrayType, @Nullable
          CCompositeType compositeType,
      List<CDesignator> pDesignators,
      List<DesignatorPosition> pPositions)
      throws CPATransferException {
    List<DesignatorPosition> results = pPositions;
    for (CDesignator designator : pDesignators) {
      if (designator instanceof CArrayDesignator) {
        Preconditions.checkNotNull(arrayType);
        CType elementType = arrayType.getType();
        int elementSize = CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates,
            pEdge, elementType);
        CExpression indexExp = ((CArrayDesignator) designator).getSubscriptExpression();
        ShapeExplicitValue indexValue = CoreShapeAdapter.getInstance()
            .evaluateSingleExplicitValue(pState, pOtherStates, pEdge, indexExp);
        if (indexValue.isUnknown()) {
          return Lists.newArrayList();
        }
        int index = indexValue.getAsInt();
        int delta = index * elementSize;
        List<DesignatorPosition> newResults = new ArrayList<>(results.size());
        for (DesignatorPosition result : results) {
          int oldOffset = result.getOffset();
          newResults.add(DesignatorPosition.valueOf(oldOffset + delta, elementType));
        }
        results = newResults;
      } else if (designator instanceof CArrayRangeDesignator) {
        Preconditions.checkNotNull(arrayType);
        CType elementType = arrayType.getType();
        int elementSize = CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates,
            pEdge, elementType);
        CExpression floorExp = ((CArrayRangeDesignator) designator).getFloorExpression();
        CExpression ceilExp = ((CArrayRangeDesignator) designator).getCeilExpression();
        ShapeExplicitValue floorValue = CoreShapeAdapter.getInstance()
            .evaluateSingleExplicitValue(pState, pOtherStates, pEdge, floorExp);
        ShapeExplicitValue ceilValue = CoreShapeAdapter.getInstance().evaluateSingleExplicitValue
            (pState, pOtherStates, pEdge, ceilExp);
        if (floorValue.isUnknown() || ceilValue.isUnknown()) {
          return Lists.newArrayList();
        }
        int floor = floorValue.getAsInt();
        int ceil = ceilValue.getAsInt();
        List<DesignatorPosition> newResults = new ArrayList<>(results.size() * (ceil - floor + 1));
        for (DesignatorPosition result : results) {
          int oldOffset = result.getOffset();
          for (int i = floor; i <= ceil; i++) {
            int delta = i * elementSize;
            newResults.add(DesignatorPosition.valueOf(oldOffset + delta, elementType));
          }
        }
        results = newResults;
      } else {
        Preconditions.checkNotNull(compositeType);
        List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
        String name = ((CFieldDesignator) designator).getFieldName();
        CType targetType = null;
        int delta = 0;
        for (CCompositeTypeMemberDeclaration member : members) {
          if (member.getName().equals(name)) {
            targetType = member.getType();
            break;
          }
          delta += CoreShapeAdapter.getInstance().evaluateSizeof(pState, pOtherStates, pEdge,
              member.getType());
        }
        if (targetType == null) {
          return Lists.newArrayList();
        }
        List<DesignatorPosition> newResults = new ArrayList<>(results.size());
        for (DesignatorPosition result : results) {
          int oldOffset = result.getOffset();
          newResults.add(DesignatorPosition.valueOf(oldOffset + delta, targetType));
        }
        results = newResults;
      }
    }
    return results;
  }

  private ShapeAddressValue getAddressFromSymbolicValue(
      ShapeState pState,
      ShapeSymbolicValue pValue) {
    if (pValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    if (pValue instanceof ShapeAddressValue) {
      return (ShapeAddressValue) pValue;
    }
    if (!pState.isAddress(pValue.getAsLong())) {
      return UnknownValue.getInstance();
    }
    return pState.getPointToForAddressValue(pValue.getAsLong());
  }

  /* ************************* */
  /* auxiliary data structures */
  /* ************************* */

  /**
   * A composite structure containing both memory region and symbolic value.
   */
  private static class RegionAndValue {

    private final SGRegion region;
    private final KnownSymbolicValue value;

    private RegionAndValue(SGRegion pRegion, KnownSymbolicValue pValue) {
      region = pRegion;
      value = pValue;
    }

    static RegionAndValue valueOf(SGRegion pRegion, KnownSymbolicValue pValue) {
      return new RegionAndValue(pRegion, pValue);
    }

    SGRegion getRegion() {
      return region;
    }

    KnownSymbolicValue getValue() {
      return value;
    }
  }

  /**
   * A wrapper class for {@code List<RegionAndValue>}.
   */
  private static class RegionAndValueList {

    private final List<RegionAndValue> innerList;

    private RegionAndValueList() {
      innerList = new ArrayList<>();
    }

    private RegionAndValueList(List<RegionAndValue> pList) {
      innerList = new ArrayList<>(pList);
    }

    static RegionAndValueList of() {
      return new RegionAndValueList();
    }

    static RegionAndValueList copyOf(RegionAndValueList list) {
      return new RegionAndValueList(list.innerList);
    }

    void add(RegionAndValue pElement) {
      innerList.add(pElement);
    }

    int size() {
      return innerList.size();
    }

    /**
     * Obtain the region and value with respect to the specified index. If the index is negative
     * or exceeds the upper bound, an exception is thrown.
     *
     * @param i index value
     * @return {@link RegionAndValue} instance
     */
    RegionAndValue get(int i) {
      if (i < 0 || i >= innerList.size()) {
        throw new IllegalArgumentException("illegal index for region-and-value list");
      }
      return innerList.get(i);
    }

    List<RegionAndValue> getList() {
      return ImmutableList.copyOf(innerList);
    }

  }

  /**
   * A class that records the offset and type (or length) of the specified designator
   */
  private static class DesignatorPosition {

    private final Integer offset;
    private final CType type;

    private DesignatorPosition(int pOffset, CType pType) {
      offset = pOffset;
      type = pType;
    }

    static DesignatorPosition valueOf(int pOffset, CType pType) {
      return new DesignatorPosition(pOffset, pType);
    }

    int getOffset() {
      return offset;
    }

    CType getType() {
      return type;
    }
  }

  /**
   * A designator usually corresponds to multiple memory position. We return all these positions
   * using a list. This class wraps the list structure to simplify the code.
   */
  private static class DesignatorPositionList {

    private final List<DesignatorPosition> positionList;
    /**
     * For example, consider the following initializer:
     * int arr[10] = { [4] = 4, [5..7] = 3 };
     * Then, the new counter for the initializer `[5..7] = 3` should be 8 because the following
     * anonymous designator initializer should be 8.
     */
    private final int newCounter;

    private DesignatorPositionList(List<DesignatorPosition> pList, int pCounter) {
      positionList = pList;
      newCounter = pCounter;
    }

    static DesignatorPositionList of(List<DesignatorPosition> pList, int pCounter) {
      return new DesignatorPositionList(pList, pCounter);
    }

    /**
     * If no valid designator position is found, we call this method to return the empty result.
     */
    static DesignatorPositionList of() {
      return new DesignatorPositionList(Lists.<DesignatorPosition>newArrayList(), 0);
    }

    int getSize() {
      return positionList.size();
    }

    List<DesignatorPosition> getPositionList() {
      return ImmutableList.copyOf(positionList);
    }

    int getNewCounter() {
      return newCounter;
    }
  }

  /**
   * Different shape state determines a new counter for anonymous initializer, and thus we should
   * not just use a single, global counter for all states.
   */
  private static class StateAndCounter {

    private final ShapeState innerState;

    private final int counter;

    private StateAndCounter(ShapeState pState, int pCounter) {
      innerState = pState;
      counter = pCounter;
    }

    static StateAndCounter valueOf(ShapeState pState, int pCounter) {
      return new StateAndCounter(pState, pCounter);
    }

    ShapeState getState() {
      return innerState;
    }

    int getCounter() {
      return counter;
    }

  }

  /* ************************ */
  /* checker-relevant methods */
  /* ************************ */

  @Override
  public Collection<ErrorReport> getErrorReports() {
    return checkerManager.getErrorReportInChecker();
  }

  @Override
  public void resetErrorReports() {
    checkerManager.resetErrorReportInChecker();
  }

  @Override
  public Collection<ErrorReport> dumpErrorsAfterAnalysis() {
    return checkerManager.dumpErrors();
  }

  @Override
  public Collection<? extends AbstractState> checkAndRefineExpression(
      AbstractState preState,
      List<AbstractState> preOtherState,
      Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    ShapeState shapeState = (ShapeState) preState;
    if (shapeState.getInvalidFreeStatus() || shapeState.getInvalidReadStatus() ||
        shapeState.getInvalidWriteStatus()) {
      // if any of these error occurs, the consequent program execution would become undefined
      return Collections.emptySet();
    }
    if (shapeState.getMemoryLeakStatus()) {
      // prevent duplicated memory leak error reports
      shapeState.resetMemoryLeakStatus();
    }
    if (shapeState.getStackAddressReturn()) {
      // prevent duplicated stack address return error reports
      shapeState.resetStackAddressReturn();
    }
    return Collections.singleton(shapeState);
  }

  @Override
  public Collection<? extends AbstractState> checkAndRefineState(
      AbstractState postState,
      List<AbstractState> postOtherStates,
      Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    ShapeState shapeState = (ShapeState) postState;
    Collection<ShapeState> resultStates = new ArrayList<>();
    checkerManager.checkState(shapeState, postOtherStates, cfaEdge, resultStates);
    return resultStates;
  }

}
