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
package org.sosy_lab.cpachecker.cpa.range.summary;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.summary.computer.RangeSummaryComputer;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithNarrowingSupport;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessResult;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessSummaryStore;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionPrecondition;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.manage.FunctionSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider;
import org.sosy_lab.cpachecker.cpa.bind.BindState;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.ExpressionRangeVisitor;
import org.sosy_lab.cpachecker.cpa.range.LeftHandAccessPathVisitor;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.TypeRangeVisitor;
import org.sosy_lab.cpachecker.cpa.range.checker.RangeRefineVisitor;
import org.sosy_lab.cpachecker.cpa.range.util.BindRefinePair;
import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;
import org.sosy_lab.cpachecker.cpa.range.util.RangeFunctionAdapter;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree.PersistentTreeNode;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Options(prefix = "cpa.range.summary")
public class RangeSummaryTransferRelation extends SingleEdgeTransferRelation
    implements TransferRelationWithNarrowingSupport {

  @Option(secure = true, description = "maximum number of array elements tracked for each array "
      + "object")
  private int maxTrackedArrayElements = 100;

  private final MachineModel machineModel;

  // perform value refinement along def-use chain (strong value update?)
  private final List<BindRefinePair> forFurtherRefine;

  private RangeSummaryComputer summaryComputer;

  /* **************** */
  /* loop information */
  /* **************** */

  private static Map<CFAEdge, Loop> entry2Loop = null;
  private static Map<CFAEdge, Loop> exit2Loop = null;

  /* ************* */
  /* summary store */
  /* ************* */

  // summary of current function (computed at the function exit)
  // to be cleared after finished a function entry
  private RangeFunctionInstance functionSummary = null;

  // loop entry -> its successor (must be in certain loop, and possibly a loop head)
  // to be cleared after finished a function entry
  private final Map<CFAEdge, CFANode> entry2Node = new HashMap<>();
  // loop node -> range state
  // to be cleared after finished a function entry
  private final Map<CFANode, RangeStateStore> node2State = new HashMap<>();
  // the state on the successor of loop exit (for external loop summary)
  // to be cleared after finished a function entry
  private final Map<CFAEdge, RangeState> exit2State = new HashMap<>();

  // the set of all global names
  // this should never be cleared
  private static Set<String> globalNames = null;

  // we store the state after global declarations
  // if the main function is analyzed for multiple times, we skip traversing the global
  // declarations to improve the performance
  // Note: we do not use multi-map here for performance reason
  // this should never be cleared
  private static final HashMap<CFAEdge, RangeState> globalDeclarationStateMap = Maps.newHashMap();

  /* Function summary is stored in the summary computer. For each function, its summary is
  updated soon after the computer finishes handling it. */

  public RangeSummaryTransferRelation(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new InvalidConfigurationException("CFA required for range summary analysis");
    }
    CFA cfa = cfaInfo.getCFA();
    machineModel = cfa.getMachineModel();
    // build loop information
    LoopStructure loops = cfa.getLoopStructure().orNull();
    ImmutableMap.Builder<CFAEdge, Loop> loopEntry = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> loopExit = ImmutableMap.builder();
    if (entry2Loop == null || exit2Loop == null) {
      if (loops != null) {
        for (final Loop loop : loops.getAllLoops()) {
          Iterable<CFAEdge> incomingEdges = FluentIterable.from(loop.getIncomingEdges()).filter
              (new Predicate<CFAEdge>() {
                @Override
                public boolean apply(CFAEdge pEdge) {
                  if (pEdge instanceof CFunctionReturnEdge) {
                    CFANode caller = ((CFunctionReturnEdge) pEdge).getSummaryEdge()
                        .getPredecessor();
                    return !loop.getLoopNodes().contains(caller);
                  }
                  return true;
                }
              });
          Iterable<CFAEdge> outgoingEdges = Iterables.filter(loop.getOutgoingEdges(),
              Predicates.not(Predicates.instanceOf(CFunctionCallEdge.class)));
          for (CFAEdge edge : incomingEdges) {
            loopEntry.put(edge, loop);
          }
          for (CFAEdge edge : outgoingEdges) {
            loopExit.put(edge, loop);
          }
        }
      }
      entry2Loop = loopEntry.build();
      exit2Loop = loopExit.build();
    }
    forFurtherRefine = Lists.newArrayList();
    collectGlobalNames(cfa.getMainFunction());
  }

  /**
   * Collect global names by traversing all global declarations in the main function block.
   */
  private void collectGlobalNames(CFANode node) {
    if (globalNames != null) {
      return;
    }
    globalNames = new HashSet<>();
    CFANode currentNode = node;
    while (currentNode.getNumLeavingEdges() == 1) {
      CFAEdge nextEdge = currentNode.getLeavingEdge(0);
      if (nextEdge instanceof BlankEdge) {
        currentNode = nextEdge.getSuccessor();
      } else if (nextEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) nextEdge).getDeclaration();
        if (!declaration.isGlobal()) {
          break;
        }
        if (declaration instanceof CVariableDeclaration) {
          globalNames.add(declaration.getQualifiedName());
        }
        currentNode = nextEdge.getSuccessor();
      } else {
        break;
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    Collection<RangeState> results;
    if (cfaEdge instanceof MultiEdge) {
      MultiEdge multiEdge = (MultiEdge) cfaEdge;
      Queue<RangeState> processQueue = new ArrayDeque<>();
      Queue<RangeState> resultQueue = new ArrayDeque<>();
      processQueue.add((RangeState) state);
      for (CFAEdge edge : multiEdge) {
        while (!processQueue.isEmpty()) {
          RangeState nextState = processQueue.poll();
          Collection<RangeState> nextResults = getAbstractSuccessorsForEdge0(nextState,
              otherStates, edge, false);
          resultQueue.addAll(nextResults);
        }
        while (!resultQueue.isEmpty()) {
          processQueue.add(resultQueue.poll());
        }
      }
      results = ImmutableSet.copyOf(processQueue);
    } else {
      results = getAbstractSuccessorsForEdge0((RangeState) state, otherStates, cfaEdge, false);
    }

    return results;
  }

  private Collection<RangeState> getAbstractSuccessorsForEdge0(
      RangeState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pCFAEdge,
      boolean forNarrow)
      throws CPATransferException {
    Collection<RangeState> successors;
    switch (pCFAEdge.getEdgeType()) {
      case DeclarationEdge:
        CDeclarationEdge declEdge = (CDeclarationEdge) pCFAEdge;
        CDeclaration declaration = declEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration && declaration.isGlobal()) {
          if (globalDeclarationStateMap.containsKey(pCFAEdge)) {
            RangeState result = globalDeclarationStateMap.get(pCFAEdge);
            return Collections.singleton(result);
          } else {
            successors = handleDeclaration(pState, pOtherStates, (CDeclarationEdge) pCFAEdge);
            if (successors.size() > 0) {
              globalDeclarationStateMap.put(pCFAEdge, successors.iterator().next());
            }
            return successors;
          }
        }
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
        successors = handleFunctionReturn(pState, (CFunctionReturnEdge) pCFAEdge, forNarrow);
        break;
      case ReturnStatementEdge:
        successors = handleReturnStatement(pState, pOtherStates, (CReturnStatementEdge) pCFAEdge,
            forNarrow);
        break;
      default:
        successors = ImmutableList.of(pState);
    }
    // handle loop summary here
    return forNarrow ? successors : collectLoopSummary(successors, pCFAEdge);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsUnderNarrowing(
      AbstractState state, List<AbstractState> otherStates, Precision precision)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("Computing successors without edge not supported");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdgeUnderNarrowing(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    // under narrowing, it is unnecessary to collect summary info
    // the transfer relation should be the same as the ordinary range transfer
    Collection<RangeState> results;
    if (cfaEdge instanceof MultiEdge) {
      MultiEdge multiEdge = (MultiEdge) cfaEdge;
      Queue<RangeState> processQueue = new ArrayDeque<>();
      Queue<RangeState> resultQueue = new ArrayDeque<>();
      processQueue.add((RangeState) state);
      for (CFAEdge edge : multiEdge) {
        while (!processQueue.isEmpty()) {
          RangeState nextState = processQueue.poll();
          Collection<RangeState> nextResults = getAbstractSuccessorsForEdge0(nextState,
              otherStates, edge, true);
          resultQueue.addAll(nextResults);
        }
        while (!resultQueue.isEmpty()) {
          processQueue.add(resultQueue.poll());
        }
      }
      results = ImmutableSet.copyOf(processQueue);
    } else {
      results = getAbstractSuccessorsForEdge0((RangeState) state, otherStates, cfaEdge, true);
    }

    return results;
  }

  /* ***************** */
  /* CFA edge handlers */
  /* ***************** */

  private Collection<RangeState> handleDeclaration(
      RangeState pState, List<AbstractState>
      pOtherStates, CDeclarationEdge pEdge) throws CPATransferException {
    CDeclaration declaration = pEdge.getDeclaration();
    if (declaration instanceof CVariableDeclaration) {
      // we should create a back-up copy of the original state, otherwise the old state would be
      // overwritten
      RangeState newState = RangeState.copyOf(pState);
      CVariableDeclaration varDecl = (CVariableDeclaration) declaration;
      CType declaredType = varDecl.getType();
      AccessPath declarationPath = getAccessPath(varDecl);
      boolean isGlobal = varDecl.isGlobal();
      TypeRangeVisitor typeRangeVisitor = new TypeRangeVisitor(declarationPath,
          maxTrackedArrayElements, machineModel, isGlobal);
      PathCopyingPersistentTree<String, Range> newRanges = declaredType.accept(typeRangeVisitor);
      if (newRanges.isEmpty()) {
        // add necessary declaration to prevent false undeclared error
        newState.addRange(declarationPath, Range.UNBOUND, true);
      } else {
        newState.addAllRanges(newRanges);
      }
      // if the initializer exists, we should refine ranges of some fields according to the
      // expressions in the initializer
      CInitializer initializer = varDecl.getInitializer();
      if (initializer != null) {
        handleInitializer(newState, pOtherStates, Lists.newArrayList(declarationPath),
            declaredType, initializer);
      }
      return Collections.singleton(newState);
    }
    // no need to copy range state here
    return Collections.singleton(pState);
  }

  /**
   * Update the state (as the first parameter of this method) by computing expressions in the
   * initializer.
   *
   * @param newState         the state to be updated
   * @param otherStates      other components of the state
   * @param declarationPaths the prefix access path
   * @param declaredType     the type of current prefix access path
   * @param pInitializer     the initializer to be handled
   */
  private void handleInitializer(
      RangeState newState,
      List<AbstractState> otherStates,
      List<AccessPath> declarationPaths,
      CType declaredType,
      CInitializer pInitializer)
      throws UnrecognizedCCodeException {
    if (pInitializer instanceof CInitializerExpression) {
      CExpression exp = ((CInitializerExpression) pInitializer).getExpression();
      // If the initializer is a string literal, we should treat it as an array list initializer.
      if (exp instanceof CStringLiteralExpression) {
        String content = ((CStringLiteralExpression) exp).getContentString();
        for (int i = 0; i < content.length(); i++) {
          char ch = content.charAt(i);
          Range chRange = new Range(ch);
          for (AccessPath singlePath : declarationPaths) {
            AccessPath newPath = AccessPath.copyOf(singlePath);
            newPath.appendSegment(new ArrayConstIndexSegment(i));
            newState.addRange(newPath, chRange, true);
          }
        }
        // Don't forget to append a ZERO segment at the tail of string
        for (AccessPath singlePath : declarationPaths) {
          AccessPath newPath = AccessPath.copyOf(singlePath);
          newPath.appendSegment(new ArrayConstIndexSegment(content.length()));
          newState.addRange(newPath, Range.ZERO, true);
        }
      } else {
        for (AccessPath singlePath : declarationPaths) {
          addRange(newState, otherStates, singlePath, exp, null);
        }
      }
    } else if (pInitializer instanceof CDesignatedInitializer) {
      // three kinds of designators:
      // (1) {@link CArrayDesignator}: [2]
      // (2) {@link CFieldDesignator}: .name
      // (3) {@link CArrayRangeDesignator}: [2 ... 4]
      CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) pInitializer;
      List<CDesignator> designators = designatedInitializer.getDesignators();
      CInitializer rightHandSide = designatedInitializer.getRightHandSide();
      // Note: multiple access paths are led by array range designator
      List<AccessPath> accumulatedPaths = new ArrayList<>(declarationPaths);
      for (CDesignator designator : designators) {
        List<AccessPath> resultPaths = new ArrayList<>();
        if (designator instanceof CArrayDesignator) {
          CExpression indexExp = ((CArrayDesignator) designator).getSubscriptExpression();
          Range indexRange = evaluateRange(newState, otherStates, indexExp).compress();
          CompInteger intNum = indexRange.numOfIntegers();
          if (intNum.equals(CompInteger.ONE)) {
            Long concreteIndex = indexRange.getLow().longValue();
            if (concreteIndex == null) {
              // we don't know what to do
              // so we just give up traversal of designators
              return;
            }
            ArrayConstIndexSegment newSegment = new ArrayConstIndexSegment(concreteIndex);
            for (AccessPath singlePath : accumulatedPaths) {
              AccessPath newPath = AccessPath.copyOf(singlePath);
              newPath.appendSegment(newSegment);
              resultPaths.add(newPath);
            }
          } else {
            return;
          }
          // for now, we do not support array subscript with uncertain index value
          CArrayType arrayType = Types.extractArrayType(declaredType);
          if (arrayType == null) {
            throw new UnsupportedOperationException("Unsupported type for array " + declaredType);
          }
          declaredType = arrayType.getType();
        } else if (designator instanceof CFieldDesignator) {
          String fieldName = ((CFieldDesignator) designator).getFieldName();
          FieldAccessSegment newSegment = new FieldAccessSegment(fieldName);
          for (AccessPath singlePath : accumulatedPaths) {
            AccessPath newPath = AccessPath.copyOf(singlePath);
            newPath.appendSegment(newSegment);
            resultPaths.add(newPath);
          }
          CCompositeType compositeType = Types.extractCompositeType(declaredType);
          if (compositeType == null) {
            throw new UnsupportedOperationException("Unsupported type for structure " +
                declaredType);
          }
          CCompositeTypeMemberDeclaration targetMember = Types.retrieveMemberByName
              (compositeType, fieldName);
          if (targetMember == null) {
            throw new UnsupportedOperationException("Specified field " + fieldName + " not found");
          }
          declaredType = targetMember.getType();
        } else {
          CArrayRangeDesignator rangeDesignator = (CArrayRangeDesignator) designator;
          CExpression floor = rangeDesignator.getFloorExpression();
          CExpression ceil = rangeDesignator.getCeilExpression();
          Range floorRange = evaluateRange(newState, otherStates, floor).compress();
          Range ceilRange = evaluateRange(newState, otherStates, ceil).compress();
          if (floorRange.numOfIntegers().equals(CompInteger.ONE) &&
              ceilRange.numOfIntegers().equals(CompInteger.ONE)) {
            Long floorValue = floorRange.getLow().longValue();
            Long ceilValue = ceilRange.getLow().longValue();
            if (floorValue == null || ceilValue == null) {
              return;
            }
            int numOfTrackedValues = 1;
            for (long i = floorValue; i <= ceilValue; i++) {
              ArrayConstIndexSegment newSegment = new ArrayConstIndexSegment(i);
              for (AccessPath singlePath : accumulatedPaths) {
                AccessPath newPath = AccessPath.copyOf(singlePath);
                newPath.appendSegment(newSegment);
                resultPaths.add(newPath);
              }
              numOfTrackedValues++;
              if (numOfTrackedValues > maxTrackedArrayElements) {
                // if the number of tracked elements exceeds the threshold, we stop adding more values
                break;
              }
            }
          } else {
            return;
          }
          CArrayType arrayType = Types.extractArrayType(declaredType);
          if (arrayType == null) {
            throw new UnsupportedOperationException("Unsupported type for array " + declaredType);
          }
          declaredType = arrayType.getType();
        }
        accumulatedPaths.clear();
        accumulatedPaths.addAll(resultPaths);
      }
      handleInitializer(newState, otherStates, accumulatedPaths, declaredType, rightHandSide);
    } else {
      CCompositeType compositeType = Types.extractCompositeType(declaredType);
      CArrayType arrayType = Types.extractArrayType(declaredType);
      if ((compositeType == null) == (arrayType == null)) {
        // unexpected case
        return;
      }
      CInitializerList initializerList = (CInitializerList) pInitializer;
      List<CInitializer> initializers = initializerList.getInitializers();
      if (arrayType != null) {
        // then, each value is treated as array element
        CType elementType = arrayType.getType();
        /* FIX: index records the position for upcoming initializer without any designators
        /* Example: struct pointer { int x; int y };
        /* struct pointer array[10] = { [2 ... 4].x = 3, [2 ... 3].y = 4, {8, 9} }
        /* Here, the last pointer structure writes on the slot of index 4! */
        // NOTE: if index is -1, then we do not know the index of upcoming member without
        // explicit designator(s), and thus we exit this function
        long index = 0;
        for (CInitializer initializer : initializers) {
          if (initializer instanceof CDesignatedInitializer) {
            List<CDesignator> designatorList = ((CDesignatedInitializer) initializer)
                .getDesignators();
            if (designatorList.size() > 0) {
              CDesignator firstDesignator = designatorList.get(0);
              if (firstDesignator instanceof CArrayDesignator) {
                CExpression indexExp = ((CArrayDesignator) firstDesignator)
                    .getSubscriptExpression();
                Range indexRange = evaluateRange(newState, otherStates, indexExp).compress();
                if (indexRange.numOfIntegers().equals(CompInteger.ONE)) {
                  Long indexValue = indexRange.getLow().longValue();
                  if (indexValue == null) {
                    index = -1;
                    continue;
                  }
                  index = indexValue + 1;
                } else {
                  index = -1;
                }
              } else if (firstDesignator instanceof CArrayRangeDesignator) {
                CExpression ceilExp = ((CArrayRangeDesignator) firstDesignator).getCeilExpression();
                Range ceilRange = evaluateRange(newState, otherStates, ceilExp).compress();
                if (ceilRange.numOfIntegers().equals(CompInteger.ONE)) {
                  Long indexValue = ceilRange.getLow().longValue();
                  if (indexValue == null) {
                    index = -1;
                    continue;
                  }
                  index = indexValue + 1;
                } else {
                  index = -1;
                }
              }
            }
            handleInitializer(newState, otherStates, declarationPaths, arrayType, initializer);
          } else {
            // the case without any designator
            if (index < 0) {
              // that really confuses us
              return;
            }
            List<AccessPath> copiedPaths = new ArrayList<>(declarationPaths.size());
            ArrayConstIndexSegment newSegment = new ArrayConstIndexSegment(index);
            for (AccessPath copiedPath : declarationPaths) {
              AccessPath newPath = AccessPath.copyOf(copiedPath);
              newPath.appendSegment(newSegment);
              copiedPaths.add(newPath);
            }
            index++;
            handleInitializer(newState, otherStates, copiedPaths, elementType, initializer);
          }
        }
      } else {
        Preconditions.checkNotNull(compositeType);
        List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
        int index = 0;
        for (CInitializer initializer : initializers) {
          if (initializer instanceof CDesignatedInitializer) {
            List<CDesignator> designatorList = ((CDesignatedInitializer) initializer)
                .getDesignators();
            if (designatorList.size() > 0) {
              CDesignator firstDesignator = designatorList.get(0);
              if (firstDesignator instanceof CFieldDesignator) {
                String fieldName = ((CFieldDesignator) firstDesignator).getFieldName();
                for (int i = 0; i < members.size(); i++) {
                  String memberName = members.get(i).getName();
                  if (memberName.equals(fieldName)) {
                    index = i + 1;
                    break;
                  }
                }
              }
            }
            handleInitializer(newState, otherStates, declarationPaths, compositeType, initializer);
          } else {
            CCompositeTypeMemberDeclaration targetMember = members.get(index);
            index++;
            String targetName = targetMember.getName();
            FieldAccessSegment newSegment = new FieldAccessSegment(targetName);
            List<AccessPath> copiedPaths = new ArrayList<>(declarationPaths.size());
            for (AccessPath singlePath : declarationPaths) {
              AccessPath newPath = AccessPath.copyOf(singlePath);
              newPath.appendSegment(newSegment);
              copiedPaths.add(newPath);
            }
            handleInitializer(newState, otherStates, copiedPaths, targetMember.getType(),
                initializer);
          }
        }
      }
    }
  }

  private Collection<RangeState> handleStatement(
      RangeState pState, List<AbstractState>
      pOtherStates, CStatementEdge pEdge) throws CPATransferException {
    RangeState newState = RangeState.copyOf(pState);
    CStatement statement = pEdge.getStatement();
    // If we encounter an function call, we never enter this function. Since the boundary analysis
    // skips this function automatically, the resultant state should be the state after the
    // function. We try to find the summary for certain function. If the summary is found, we
    // use summary information to update the state after skipping the function. Otherwise, we
    // assume that the returned value is TOP.
    if (statement instanceof CFunctionCall) {
      // no assignment, but possibly contains side-effect on (global variables)
      CFunctionCallExpression callExp = ((CFunctionCall) statement).getFunctionCallExpression();
      CExpression nameExp = callExp.getFunctionNameExpression();
      // try to get the name of the function
      String funcName = null;
      CFunctionDeclaration funcDecl = callExp.getDeclaration();
      if (funcDecl != null) {
        funcName = funcDecl.getName();
      } else {
        if (nameExp instanceof CIdExpression) {
          funcName = ((CIdExpression) nameExp).getName();
        }
      }
      boolean needRelax = false;
      if (funcName != null) {
        // 1. handle stop function
        if (GlobalInfo.getInstance().queryStopFunction(funcName)) {
          return Collections.emptySet();
        }
        // 2. if current function is registered in the function adapter, we directly evaluate
        // this function expression using function adapter
        if (RangeFunctionAdapter.instance(true).isRegistered(callExp)) {
          if (statement instanceof CFunctionCallStatement) {
            // we simply discard range for return variable
            evaluateRange(newState, pOtherStates, callExp);
          } else {
            CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();
            AccessPath leftPath = getAccessPath(newState, pOtherStates, lhs);
            if (leftPath != null) {
              addRange(newState, pOtherStates, leftPath, callExp, null);
            }
          }
          return Collections.singleton(newState);
        }
        // 3. unknown function: we change the state by applying function summary
        needRelax =
            applyFunctionSummary(newState, pOtherStates, funcName, (CFunctionCall) statement);
      }
      if (funcName == null || needRelax) {
        if (statement instanceof CFunctionCallAssignmentStatement) {
          CLeftHandSide lhs = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();
          AccessPath leftPath = getAccessPath(newState, pOtherStates, lhs);
          if (leftPath != null) {
            TypeRangeVisitor typeRangeVisitor = new TypeRangeVisitor(leftPath,
                maxTrackedArrayElements, machineModel, false);
            CType leftType = lhs.getExpressionType();
            PathCopyingPersistentTree<String, Range> newRanges = leftType.accept(typeRangeVisitor);
            newState.addAllRanges(newRanges);
          }
        }
      }
    } else if (statement instanceof CAssignment) {
      // the RHS of the assignment should NOT an expression with side-effects (i.e. containing
      // function call)
      CLeftHandSide lhs = ((CAssignment) statement).getLeftHandSide();
      CRightHandSide rhs = ((CAssignment) statement).getRightHandSide();
      AccessPath leftPath = getAccessPath(newState, pOtherStates, lhs);
      if (leftPath == null) {
        return Collections.singleton(newState);
      }
      addRange(newState, pOtherStates, leftPath, rhs, null);
    }
    return Collections.singleton(newState);
  }

  private Collection<RangeState> handleAssumption(
      RangeState state, List<AbstractState>
      otherStates, CAssumeEdge cfaEdge) throws CPATransferException {
    boolean truth = cfaEdge.getTruthAssumption();
    // assumption should be a logical expression ranging in [0,1]
    CExpression assumption = cfaEdge.getExpression();
    RangeState newState = RangeState.copyOf(state);
    Range assumeRange = evaluateRange(newState, otherStates, assumption);
    if (assumeRange.isEmpty() || (truth ? Range.ZERO : Range.ONE).equals(assumeRange)) {
      return Collections.emptySet();
    }
    BinaryOperator op = ((CBinaryExpression) assumption).getOperator();
    CExpression op1 = ((CBinaryExpression) assumption).getOperand1();
    CExpression op2 = ((CBinaryExpression) assumption).getOperand2();
    if (!truth) {
      op = op.getOppositeLogicalOperator();
    }
    ExpressionRangeVisitor visitor = new ExpressionRangeVisitor(newState, otherStates,
        machineModel, true);
    Range range1 = op1.accept(visitor);
    Range range2 = op2.accept(visitor);
    Range restrict1, restrict2;
    switch (op) {
      case LESS_THAN:
        restrict1 = range1.limitUpperBoundBy(range2.minus(1L));
        restrict2 = range2.limitLowerBoundBy(range1.plus(1L));
        newState = op1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, true));
        newState = op2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, true));
        // refine more expressions according to def-use chain
        forFurtherRefine.add(new BindRefinePair(op1, restrict1));
        forFurtherRefine.add(new BindRefinePair(op2, restrict2));
        break;
      case LESS_EQUAL:
        restrict1 = range1.limitUpperBoundBy(range2);
        restrict2 = range2.limitLowerBoundBy(range1);
        newState = op1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, true));
        newState = op2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, true));
        forFurtherRefine.add(new BindRefinePair(op1, restrict1));
        forFurtherRefine.add(new BindRefinePair(op2, restrict2));
        break;
      case GREATER_THAN:
        restrict1 = range1.limitLowerBoundBy(range2.plus(1L));
        restrict2 = range2.limitUpperBoundBy(range1.minus(1L));
        newState = op1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, true));
        newState = op2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, true));
        forFurtherRefine.add(new BindRefinePair(op1, restrict1));
        forFurtherRefine.add(new BindRefinePair(op2, restrict2));
        break;
      case GREATER_EQUAL:
        restrict1 = range1.limitLowerBoundBy(range2);
        restrict2 = range2.limitUpperBoundBy(range1);
        newState = op1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, true));
        newState = op2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, true));
        forFurtherRefine.add(new BindRefinePair(op1, restrict1));
        forFurtherRefine.add(new BindRefinePair(op2, restrict2));
        break;
      case EQUALS:
        restrict1 = range1.intersect(range2);
        restrict2 = range2.intersect(range1);
        newState = op1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, true));
        newState = op2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, true));
        forFurtherRefine.add(new BindRefinePair(op1, restrict1));
        forFurtherRefine.add(new BindRefinePair(op2, restrict2));
        break;
      case NOT_EQUALS:
        Pair<Range, Range> splitResult = splitRanges(range1, range2);
        restrict1 = splitResult.getFirst();
        restrict2 = splitResult.getSecond();
        if (restrict1 != null) {
          // not null: the first range could be refined with restrict range 1
          newState = op1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
              machineModel, true));
          forFurtherRefine.add(new BindRefinePair(op1, restrict1));
        }
        if (restrict2 != null) {
          newState = op2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
              machineModel, true));
          forFurtherRefine.add(new BindRefinePair(op2, restrict2));
        }
        break;
      default:
        throw new UnrecognizedCCodeException("unexpected operator in assumption", cfaEdge,
            assumption);
    }
    return Collections.singleton(newState);
  }

  private Collection<RangeState> handleFunctionCall(
      RangeState state, List<AbstractState>
      otherStates, CFunctionCallEdge cfaEdge) throws CPATransferException {
    RangeState newState = RangeState.copyOf(state);
    final List<CExpression> args = cfaEdge.getArguments();
    final CFunctionEntryNode entryNode = cfaEdge.getSuccessor();
    final List<CParameterDeclaration> params = entryNode.getFunctionParameters();
    // the size of arguments should be no fewer than the size of parameter declarations
    final CFunctionSummaryEdge summaryEdge = cfaEdge.getSummaryEdge();
    final CFunctionCall call = summaryEdge.getExpression();
    final String funcName = entryNode.getFunctionName();
    // 1. collect precondition of the function
    for (int i = 0; i < params.size(); i++) {
      CParameterDeclaration param = params.get(i);
      CExpression arg = args.get(i);
      AccessPath paramPath = getAccessPath(param);
      addRange(newState, otherStates, paramPath, arg, param.getType());
    }
    RangeState preState = new RangeState();
    // transfer ranges on parameters
    for (CParameterDeclaration param : params) {
      transferDeclarations(newState, preState, param);
    }
    // collect information of global variables
    // we do not collect global values for performance reason, for now
    // update precondition
    RangeFunctionPrecondition.updatePrecondition(funcName, preState);
    // 2. skip this function using existing range summary or access summary
    if (applyFunctionSummary(newState, otherStates, funcName, call)) {
      // if we reach here, we need to relax the return value
      if (call instanceof CAssignment) {
        CLeftHandSide lhs = ((CAssignment) call).getLeftHandSide();
        AccessPath leftPath = getAccessPath(newState, otherStates, lhs);
        if (leftPath != null) {
          TypeRangeVisitor trVisitor = new TypeRangeVisitor(leftPath, maxTrackedArrayElements,
              machineModel, false);
          CType leftType = lhs.getExpressionType();
          PathCopyingPersistentTree<String, Range> newRanges = leftType.accept(trVisitor);
          newState.addAllRanges(newRanges);
        }
      }
    }
    return Collections.singleton(newState);
  }

  private Collection<RangeState> handleFunctionReturn(
      RangeState state, CFunctionReturnEdge
      cfaEdge, boolean forNarrow)
      throws CPATransferException {
    // no successor is computed for function return edge
    // we create a new function summary instance for collecting invariant
    String funcName = cfaEdge.getFunctionEntry().getFunctionName();
    collectFunctionSummary(funcName, state,
        cfaEdge.getFunctionEntry().getReturnVariable().orNull(), forNarrow);
    return Collections.emptySet();
  }

  private Collection<RangeState> handleReturnStatement(
      RangeState state, List<AbstractState>
      otherStates, CReturnStatementEdge cfaEdge, boolean forNarrow) throws CPATransferException {
    RangeState newState = RangeState.copyOf(state);
    Optional<CAssignment> orAssign = cfaEdge.asAssignment();
    if (orAssign.isPresent()) {
      CAssignment assign = orAssign.get();
      CLeftHandSide leftHand = assign.getLeftHandSide();
      CType returnType = leftHand.getExpressionType();
      AccessPath path = getAccessPath(newState, otherStates, leftHand);
      // in general, the LHS should be a virtual return variable
      assert (path != null);
      addRange(newState, otherStates, path, assign.getRightHandSide(), returnType);
    }
    // if the current function is main (which does not have function return edge), we collect the
    // function summary here
    if (cfaEdge.getSuccessor().getNumLeavingEdges() == 0) {
      CFunctionEntryNode entry = (CFunctionEntryNode) cfaEdge.getSuccessor().getEntryNode();
      collectFunctionSummary(entry.getFunctionName(), newState, entry.getReturnVariable().orNull
          (), forNarrow);
    }
    return Collections.singleton(newState);
  }

  /* *************** */
  /* utility methods */
  /* *************** */

  private AccessPath getAccessPath(CSimpleDeclaration declaration) {
    return new AccessPath(declaration);
  }

  @Nullable
  private AccessPath getAccessPath(
      RangeState state, List<AbstractState> otherStates, CExpression
      expression) {
    if (expression instanceof CLeftHandSide) {
      LeftHandAccessPathVisitor visitor = new LeftHandAccessPathVisitor(new
          ExpressionRangeVisitor(state, otherStates, machineModel, true));
      AccessPath path;
      try {
        path = ((CLeftHandSide) expression).accept(visitor).orNull();
      } catch (UnrecognizedCCodeException e) {
        path = null;
      }
      return path;
    }
    // otherwise, there is no access path
    return null;
  }

  private void addRange(
      RangeState newState, List<AbstractState> pOtherStates, @Nullable AccessPath
      pPath, CRightHandSide e, @Nullable CType restrictType)
      throws UnrecognizedCCodeException {
    if (pPath != null) {
      AccessPath path = AccessPath.copyOf(pPath);
      if (e instanceof CStringLiteralExpression) {
        String content = ((CStringLiteralExpression) e).getContentString();
        for (int i = 0; i < content.length(); i++) {
          AccessPath newPath = AccessPath.copyOf(path);
          newPath.appendSegment(new ArrayConstIndexSegment(i));
          addRange(newState, newPath, new Range(content.charAt(i)));
        }
        // Moreover, append '\0' at the end
        AccessPath newPath = AccessPath.copyOf(path);
        newPath.appendSegment(new ArrayConstIndexSegment(content.length()));
        addRange(newState, newPath, Range.ZERO);
      } else if (e instanceof CLeftHandSide) {
        AccessPath rightPath = getAccessPath(newState, pOtherStates, (CLeftHandSide) e);
        if (rightPath != null) {
          AccessPath newPath = AccessPath.copyOf(path);
          newState.replaceAndCopy(rightPath, newPath, true);
          Range updatedRange = newState.getRange(newPath, machineModel);
          if (restrictType != null) {
            Range typeRange = Ranges.getTypeRange(restrictType, machineModel);
            if (!typeRange.contains(updatedRange)) {
              newState.addRange(newPath, typeRange, true);
            }
          }
        }
      } else {
        // other cases are trivial
        Range resultRange = evaluateRange(newState, pOtherStates, e);
        // we should sanitize values such as function parameters and return values
        if (restrictType != null) {
          Range typeRange = Ranges.getTypeRange(restrictType, machineModel);
          if (!typeRange.contains(resultRange)) {
            resultRange = typeRange;
          }
        }
        AccessPath newPath = AccessPath.copyOf(path);
        addRange(newState, newPath, resultRange);
      }
      // In most cases, we migrate the range information of this path to the left one
      // However, if the access path contains 'union' (union data structure), we should relax
      // fields in this union that have larger size than current union member.
      List<CType> typeList = path.parseTypeList();
      int lastPos = typeList.size() - 1;
      AccessPath previousPath = AccessPath.copyOf(path);
      for (int i = lastPos; i >= 0; i--) {
        CType currentType = typeList.get(i);
        CCompositeType compositeType = Types.extractCompositeType(currentType);
        if (compositeType != null && compositeType.getKind() == ComplexTypeKind.UNION &&
            i < lastPos) {
          // relax union members
          CType nextType = typeList.get(i + 1);
          int currentSize = machineModel.getSizeof(nextType);
          List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
          for (CCompositeTypeMemberDeclaration member : members) {
            if (member.getName().equals(previousPath.getLastSegment().getName())) {
              // avoid to re-compute the known access path
              continue;
            }
            CType memberType = member.getType();
            int memberSize = machineModel.getSizeof(memberType);
            if (memberSize <= currentSize) {
              // we update this field if and only if:
              // (1) current type and specified type are equivalent
              // (2) current and specified types are both numerical types AND value override
              // corrupts all bits of the original value
              if (Types.isEquivalent(memberType, nextType) ||
                  (Types.isNumericalType(nextType) && Types.isNumericalType(memberType))) {
                AccessPath newPath = AccessPath.copyOf(path);
                newPath.appendSegment(new FieldAccessSegment(member.getName()));
                newState.replaceAndCopy(previousPath, newPath, true);
              }
            } else {
              // The data of this member is corrupted. Although we can restore its value
              // precisely in few cases, for efficiency purpose we fully relax this field
              AccessPath newPath = AccessPath.copyOf(path);
              newPath.appendSegment(new FieldAccessSegment(member.getName()));
              TypeRangeVisitor typeVisitor = new TypeRangeVisitor(newPath,
                  maxTrackedArrayElements, machineModel, false);
              PathCopyingPersistentTree<String, Range> newRanges = memberType.accept(typeVisitor);
              newState.addAllRanges(newRanges);
            }
          }
        }
        previousPath = AccessPath.copyOf(path);
        path.removeLastSegment();
      }
    }
  }

  private void addRange(RangeState newState, @Nullable AccessPath path, Range range) {
    if (path != null) {
      newState.addRange(path, range, true);
    }
  }

  private Range evaluateRange(
      RangeState readableState, List<AbstractState> pOtherStates,
      CRightHandSide expression)
      throws UnrecognizedCCodeException {
    return expression.accept(new ExpressionRangeVisitor(readableState, pOtherStates,
        machineModel, true));
  }

  private Pair<Range, Range> splitRanges(Range r1, Range r2) {
    Range rs1 = null, rs2 = null;
    if (r1.isEmpty() || r2.isEmpty()) {
      // if one operand has empty range, the total range should be empty either and it is
      // unnecessary to split the range now
      return Pair.of(null, null);
    }
    if (r1.getLow().equals(r1.getHigh())) {
      CompInteger point = r1.getLow();
      Range partOne = r2.intersect(Range.upperBoundedRange(point.subtract(
          CompIntegers.ALMOST_ZERO_DELTA)));
      Range partTwo = r2.intersect(Range.lowerBoundedRange(point.add(CompIntegers
          .ALMOST_ZERO_DELTA)));
      boolean isEmptyOne = partOne.isEmpty();
      boolean isEmptyTwo = partTwo.isEmpty();
      if (isEmptyOne && isEmptyTwo) {
        rs2 = Range.EMPTY;
      } else if (isEmptyOne) {
        rs2 = partTwo;
      } else if (isEmptyTwo) {
        rs2 = partOne;
      }
    }
    if (r2.getLow().equals(r2.getHigh())) {
      CompInteger point = r2.getLow();
      Range partOne = r1.intersect(Range.upperBoundedRange(point.subtract(CompIntegers
          .ALMOST_ZERO_DELTA)));
      Range partTwo = r1.intersect(Range.lowerBoundedRange(point.add(CompIntegers
          .ALMOST_ZERO_DELTA)));
      boolean isEmptyOne = partOne.isEmpty();
      boolean isEmptyTwo = partTwo.isEmpty();
      if (isEmptyOne && isEmptyTwo) {
        rs1 = Range.EMPTY;
      } else if (isEmptyOne) {
        rs1 = partTwo;
      } else if (isEmptyTwo) {
        rs1 = partOne;
      }
    }
    // check if the refined range is strictly smaller than the original one
    if (rs1 != null) {
      if (rs1.equals(r1)) {
        rs1 = null;
      }
    }
    if (rs2 != null) {
      if (rs2.equals(r2)) {
        rs2 = null;
      }
    }
    return Pair.of(rs1, rs2);
  }

  /**
   * Transfer range tree of specific declaration.
   */
  private void transferDeclarations(RangeState fromState, RangeState toState, CSimpleDeclaration
      pDeclaration) {
    String qualifiedName = pDeclaration.getQualifiedName();
    PersistentTreeNode<String, Range> subtree = fromState.removeRangesWithPrefix(qualifiedName);
    if(subtree != null) {
      toState.addRangesWithPrefix(qualifiedName, subtree);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    if (cfaEdge == null) {
      return null;
    }
    RangeState rangeState = (RangeState) state;
    if (!forFurtherRefine.isEmpty()) {
      BindState bindState = AbstractStates.extractStateByType(otherStates, BindState.class);
      if (bindState != null) {
        for (BindRefinePair pair : forFurtherRefine) {
          CExpression expression = pair.getExpression();
          Range restrict = pair.getRestrictRange();
          AccessPath path = getAccessPath(rangeState, otherStates, expression);
          if (path != null) {
            List<CRightHandSide> associates = bindState.getBindedExpression(path, otherStates, 3);
            for (CRightHandSide exp : associates) {
              rangeState = exp.accept(new RangeRefineVisitor(rangeState, otherStates, restrict,
                  machineModel, true));
            }
          }
        }
      }
      forFurtherRefine.clear();
    }
    return Collections.singleton(rangeState);
  }

  /* ******************* */
  /* summary computation */
  /* ******************* */

  /**
   * apply function summary to change `state` (which means `state` is mutable in this method)
   *
   * @return whether the return value should be relaxed
   */
  private boolean applyFunctionSummary(
      RangeState state, List<AbstractState> otherStates, String
      funcName, CFunctionCall funCall) throws CPATransferException {
    // 1. get function summary
    RangeFunctionInstance summary = summaryComputer.getFunctionSummary(funcName);
    if (summary != null) {
      RangeState stateForApply = summary.apply();
      // if this is an assignment, we should also update the range of LHS
      if (funCall instanceof CAssignment) {
        CLeftHandSide lhs = ((CAssignment) funCall).getLeftHandSide();
        AccessPath leftPath = getAccessPath(state, otherStates, lhs);
        if (leftPath != null) {
          PathCopyingPersistentTree<String, Range> returnTree = summary.getReturnSummary();
          PersistentTreeNode<String, Range> returnRoot = returnTree.getRoot();
          if (returnRoot != null) {
            Set<String> keys = returnRoot.getKeys();
            String returnKey = Iterables.getOnlyElement(keys);
            PersistentTreeNode<String, Range> subtree = returnRoot.getChild(returnKey);
            stateForApply.addRangesWithPrefix(leftPath, subtree);
          }
        }
      }
      state.forcedUpdate(stateForApply);
      // do not relax return value
      return false;
    } else {
      // 2. no function summary
      // return value -> type range
      // global variables -> from access summary
      List<FunctionSummaryStore<?>> summaryStoreList = SummaryProvider.getFunctionSummary();
      AccessSummaryStore accessStore = null;
      for (FunctionSummaryStore<?> summaryStore : summaryStoreList) {
        if (summaryStore instanceof AccessSummaryStore) {
          accessStore = (AccessSummaryStore) summaryStore;
          break;
        }
      }
      if (accessStore == null) {
        throw new IllegalArgumentException("Access summary required for computing range summary");
      }
      AccessFunctionInstance instance = accessStore.query(funcName);
      if (instance != null) {
        AccessResult accessTree = instance.apply();
        // consider write-tree here
        for (AccessPath accessPath : accessTree.writes) {
          if (accessPath.isGlobal()) {
            TypeRangeVisitor trVisitor = new TypeRangeVisitor(accessPath,
                maxTrackedArrayElements, machineModel, false);
            List<CType> typeList = accessPath.parseTypeList();
            CType lastType = typeList.get(typeList.size() - 1);
            PathCopyingPersistentTree<String, Range> newRanges = lastType.accept(trVisitor);
            state.addAllRanges(newRanges);
          }
        }
      }
      // relax return value
      return true;
    }
  }

  private void collectFunctionSummary(
      String funcName, RangeState state,
      @Nullable CVariableDeclaration returnVar,
      boolean forNarrow) {
    // traverse all the declarations in the given state, and extract information on global
    // variables and the return value
    RangeFunctionInstance newInstance = new RangeFunctionInstance(funcName);
    // we collect no global values for performance reason
    if (returnVar != null) {
      String retName = returnVar.getQualifiedName();
      PersistentTreeNode<String, Range> retTree = state.getSubTree(retName);
      if (retTree != null) {
        newInstance.addReturnSummary(retName, retTree);
      }
    }
    // merge with the existing function summary
    // Note: if the transfer works under the narrowing mode, the function summary will be
    // forcibly overwritten
    if (functionSummary == null || forNarrow) {
      functionSummary = newInstance;
    } else {
      functionSummary = functionSummary.merge(newInstance);
    }
  }

  private Collection<RangeState> collectLoopSummary(
      Collection<RangeState> states,
      CFAEdge cfaEdge) {
    // if multiple successors are derived, we should merge them into one before proceeding
    if (states.isEmpty()) {
      return states;
    }
    RangeState newState;
    if (states.size() == 1) {
      newState = Iterables.getOnlyElement(states);
    } else {
      final Iterator<RangeState> it = states.iterator();
      newState = it.next();
      while (it.hasNext()) {
        newState = newState.join(it.next());
      }
    }

    if (entry2Loop.containsKey(cfaEdge)) {
      // current edge is loop entry edge
      // such edge is traversed for only once, and the state here represents the constraint
      // before running any times of iterations
      CFANode entryNode = cfaEdge.getSuccessor();
      entry2Node.put(cfaEdge, entryNode);
      RangeStateStore stateStore = node2State.get(entryNode);
      if (stateStore == null) {
        // create a new state store here
        stateStore = new RangeStateStore(newState);
        node2State.put(entryNode, stateStore);
      } else {
        // it occurs when the current loop is enclosed by other loops
        if (!stateStore.initializeState(newState)) {
          return Collections.emptySet();
        }
      }
    } else if (exit2Loop.containsKey(cfaEdge)) {
      // current edge is loop exit edge
      RangeState oldState = exit2State.get(cfaEdge);
      if (oldState == null) {
        exit2State.put(cfaEdge, newState);
      } else {
        // the exit constraint grows larger
        exit2State.put(cfaEdge, oldState.join(newState));
      }
    } else {
      CFANode successorLoc = cfaEdge.getSuccessor();
      // If the successor location is a possible implicit loop head, then we treat it as a loop
      // head. (This is necessary because CPAchecker may fail to recognize a real loop)
      if (isPossibleLoopHead(successorLoc)) {
        if (node2State.containsKey(successorLoc)) {
          RangeStateStore stateStore = node2State.get(successorLoc);
          stateStore.updateState(newState);
          // get the widened state
          newState = stateStore.getState();
        } else {
          RangeStateStore stateStore = new RangeStateStore(newState);
          node2State.put(successorLoc, stateStore);
        }
      }
    }
    return Collections.singleton(newState);
  }

  private boolean isPossibleLoopHead(CFANode loc) {
    // (1) more than 1 edge can lead to certain location,
    // (2) certain location must have at least one successor,
    // (3) function entry/exit should not be loop head. (Otherwise, widening occurs in handling
    // function entry edge.)
    return !(loc instanceof FunctionEntryNode) && !(loc instanceof FunctionExitNode)
        && loc.getNumEnteringEdges() > 1 && loc.getNumLeavingEdges() > 0 &&
        CFAUtils.allSuccessorsOf(loc).anyMatch(FORWARD_REACHABLE(loc));
  }

  private Predicate<CFANode> FORWARD_REACHABLE(final CFANode to) {
    return new Predicate<CFANode>() {
      @Override
      public boolean apply(CFANode pCFANode) {
        Set<CFANode> visited = new HashSet<>();
        Queue<CFANode> waitlist = new ArrayDeque<>();
        waitlist.offer(pCFANode);
        while (!waitlist.isEmpty()) {
          CFANode current = waitlist.poll();
          if (current.equals(to)) {
            return true;
          }
          if (visited.add(current)) {
            for (CFANode successor : CFAUtils.allSuccessorsOf(current)) {
              if (successor.getFunctionName().equals(to.getFunctionName())) {
                // such successor is not out of the current function scope
                waitlist.offer(successor);
              }
            }
          }
        }
        return false;
      }
    };
  }

  /**
   * Set the instance of range summary computer for storing summary info.
   */
  void setSummaryComputer(@Nonnull RangeSummaryComputer pComputer) {
    summaryComputer = pComputer;
  }

  void getLoopSummary(
      Multimap<Loop, Pair<CFAEdge, RangeState>> pRawInternalSummary,
      Multimap<Loop, Pair<CFAEdge, RangeState>> pRawExternalSummary) {
    for (Entry<CFAEdge, CFANode> entry : entry2Node.entrySet()) {
      CFAEdge entryEdge = entry.getKey();
      CFANode entryNode = entry.getValue();
      Loop loop = entry2Loop.get(entryEdge);
      RangeState state = node2State.get(entryNode).getTotalState();
      pRawInternalSummary.put(loop, Pair.of(entryEdge, state));
    }
    for (Entry<CFAEdge, RangeState> entry : exit2State.entrySet()) {
      CFAEdge exitEdge = entry.getKey();
      RangeState state = entry.getValue();
      Loop loop = exit2Loop.get(exitEdge);
      pRawExternalSummary.put(loop, Pair.of(exitEdge, state));
    }
  }

  @Nullable
  RangeFunctionInstance getFunctionSummary() {
    // a function should have at least one exit edge
    return functionSummary;
  }

  void clearPerFunction() {
    functionSummary = null;
    entry2Node.clear();
    node2State.clear();
    exit2State.clear();
  }

  RangeState initializeDeclaration(RangeState oldState, CSimpleDeclaration declaration)
      throws UnrecognizedCCodeException {
    AccessPath declaredPath = new AccessPath(declaration);
    TypeRangeVisitor visitor = new TypeRangeVisitor(declaredPath, maxTrackedArrayElements,
        machineModel, false);
    CType declaredType = declaration.getType();
    PathCopyingPersistentTree<String, Range> rangeTree = declaredType.accept(visitor);
    RangeState newState = RangeState.copyOf(oldState);
    newState.addAllRanges(rangeTree);
    return newState;
  }

  void loadLoopSummary(CFAInfo cfaInfo, String funcName) {
    CFA cfa = cfaInfo.getCFA();
    LoopStructure loopStructure = cfa.getLoopStructure().orNull();
    if (loopStructure == null) {
      return;
    }
    Collection<Loop> loopInFunc = loopStructure.getLoopsForFunction(funcName);
    for (Loop loop : loopInFunc) {
      RangeInternalLoopInstance internalInstance = summaryComputer.getInternalLoopSummary(loop);
      if (internalInstance != null) {
        Map<CFAEdge, RangeState> innerSummary = internalInstance.getLoopSummary();
        for (Entry<CFAEdge, RangeState> entry : innerSummary.entrySet()) {
          CFAEdge entryEdge = entry.getKey();
          RangeState entryState = entry.getValue();
          entry2Node.put(entryEdge, entryEdge.getSuccessor());
          node2State.put(entryEdge.getSuccessor(), new RangeStateStore(entryState));
        }
      }
      RangeExternalLoopInstance externalInstance = summaryComputer.getExternalLoopSummary(loop);
      if (externalInstance != null) {
        Map<CFAEdge, RangeState> innerSummary = externalInstance.getLoopSummary();
        for (Entry<CFAEdge, RangeState> entry : innerSummary.entrySet()) {
          CFAEdge exitEdge = entry.getKey();
          RangeState exitState = entry.getValue();
          exit2State.put(exitEdge, exitState);
        }
      }
    }
  }

}
