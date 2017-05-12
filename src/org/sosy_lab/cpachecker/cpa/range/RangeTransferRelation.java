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
package org.sosy_lab.cpachecker.cpa.range;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.bugfix.FixInformation;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFixInfo;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerTypeConstraint.IntegerTypePredicate;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithCheck;
import org.sosy_lab.cpachecker.core.interfaces.checker.AssignmentCell;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerManager;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ExpressionCell;
import org.sosy_lab.cpachecker.core.phase.fix.IntegerFixApplicationPhase;
import org.sosy_lab.cpachecker.core.phase.fix.IntegerFixGenerationPhase;
import org.sosy_lab.cpachecker.cpa.bind.BindState;
import org.sosy_lab.cpachecker.cpa.range.checker.DividedByZeroChecker;
import org.sosy_lab.cpachecker.cpa.range.checker.IntegerConversionChecker;
import org.sosy_lab.cpachecker.cpa.range.checker.IntegerOverflowChecker;
import org.sosy_lab.cpachecker.cpa.range.checker.RangeRefineVisitor;
import org.sosy_lab.cpachecker.cpa.range.util.AccessSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.range.util.BindRefinePair;
import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;
import org.sosy_lab.cpachecker.cpa.range.util.RangeFunctionAdapter;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Options(prefix = "cpa.range")
public final class RangeTransferRelation
    extends ForwardingTransferRelation<Collection<RangeState>, RangeState, Precision>
    implements TransferRelationWithCheck {

  private final LogManager logger;

  @Option(secure = true, description = "maximum number of array elements we should track")
  private int maxTrackedArrayElements = 100;

  @Option(secure = true, description = "whether value truncation is allowed in type casting")
  private boolean truncatedCastNotAllowed = false;

  private final MachineModel machineModel;
  private final CheckerManager<RangeState> checkerManager;

  private final List<BindRefinePair> forFurtherRefine;

  private IntegerFixInfo fixInfo;

  public RangeTransferRelation(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    // load machine model information from CFA
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      machineModel = cfaInfo.get().getCFA().getMachineModel();
    } else {
      // this branch should not be reached
      throw new InvalidConfigurationException("A valid CFA is required to determine machine model");
    }

    // binding refine pairs are initialized once
    forFurtherRefine = new ArrayList<>();

    // initialize the fixInfo as null by default, in case when there is no checker is registered
    fixInfo = null;

    // initialize checker manager
    checkerManager = new CheckerManager<>(pConfig, RangeState.class);
    // register checkers to checker manager
    checkerManager.register(IntegerOverflowChecker.class);
    checkerManager.register(IntegerConversionChecker.class);
    checkerManager.register(DividedByZeroChecker.class);

    // initialize access summary applicator
    AccessSummaryApplicator.initialize(maxTrackedArrayElements, machineModel);
  }

  @Override
  protected Collection<RangeState> handleSimpleEdge(final CFAEdge pCFAEdge)
      throws CPATransferException {
    switch (pCFAEdge.getEdgeType()) {
      case DeclarationEdge:
        final CDeclarationEdge declarationEdge = (CDeclarationEdge) pCFAEdge;
        return handleDeclarationEdge(declarationEdge, declarationEdge.getDeclaration());
      case StatementEdge:
        final CStatementEdge statementEdge = (CStatementEdge) pCFAEdge;
        return handleStatementEdge(statementEdge, statementEdge.getStatement());
      case ReturnStatementEdge:
        final CReturnStatementEdge returnEdge = (CReturnStatementEdge) pCFAEdge;
        return handleReturnStatementEdge(returnEdge);
      case BlankEdge:
        return handleBlankEdge((BlankEdge) pCFAEdge);
      case CallToReturnEdge:
        return handleFunctionSummaryEdge((CFunctionSummaryEdge) pCFAEdge);
      default:
        throw new UnrecognizedCFAEdgeException(pCFAEdge);
    }
  }

  @Override
  protected Collection<RangeState> handleDeclarationEdge(
      CDeclarationEdge cfaEdge, CDeclaration decl) throws CPATransferException {
    if (decl instanceof CVariableDeclaration) {
      RangeState newState = RangeState.copyOf(state);
      CVariableDeclaration variableDeclaration = (CVariableDeclaration) cfaEdge.getDeclaration();
      CType declaredType = variableDeclaration.getType();
      AccessPath declarationPath = getAccessPath(variableDeclaration);
      // global variables are zero-initialized
      boolean isGlobal = variableDeclaration.isGlobal();
      // FIRST, we initialize all members of data structure (relax)
      TypeRangeVisitor typeRangeVisitor = new TypeRangeVisitor(declarationPath,
          maxTrackedArrayElements, machineModel, isGlobal);
      PathCopyingPersistentTree<String, Range> newRanges = declaredType.accept(typeRangeVisitor);
      if (newRanges.isEmpty()) {
        newState.addRange(declarationPath, Range.UNBOUND, false);
      } else {
        newState.addAllRanges(newRanges);
      }
      // SECOND, if initializer exists, we refine some member ranges according to value of
      // initializer expressions
      CInitializer initializer = variableDeclaration.getInitializer();
      if (initializer != null) {
        handleInitializer(newState, Lists.newArrayList(declarationPath), declaredType, initializer);
      }
      return Collections.singleton(newState);
    }
    // no need to copy the state here
    return Collections.singleton(state);
  }

  private void handleInitializer(
      RangeState newState,
      List<AccessPath> declarationPaths,
      CType declaredType,
      CInitializer pInitializer)
      throws UnrecognizedCCodeException {
    if (pInitializer instanceof CInitializerExpression) {
      CExpression exp = ((CInitializerExpression) pInitializer).getExpression();
      if (exp instanceof CStringLiteralExpression) {
        String content = ((CStringLiteralExpression) exp).getContentString();
        for (int i = 0; i < content.length(); i++) {
          char ch = content.charAt(i);
          Range chRange = new Range(ch);
          for (AccessPath singlePath : declarationPaths) {
            AccessPath newPath = AccessPath.copyOf(singlePath);
            newPath.appendSegment(new ArrayConstIndexSegment(i));
            newState.addRange(newPath, chRange, false);
          }
        }
        // Don't forget to append a ZERO segment at the tail of string
        for (AccessPath singlePath : declarationPaths) {
          AccessPath newPath = AccessPath.copyOf(singlePath);
          newPath.appendSegment(new ArrayConstIndexSegment(content.length()));
          newState.addRange(newPath, Range.ZERO, false);
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
      handleInitializer(newState, accumulatedPaths, declaredType, rightHandSide);
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
            handleInitializer(newState, declarationPaths, arrayType, initializer);
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
            handleInitializer(newState, copiedPaths, elementType, initializer);
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
            handleInitializer(newState, declarationPaths, compositeType, initializer);
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
            handleInitializer(newState, copiedPaths, targetMember.getType(), initializer);
          }
        }
      }
    }
  }

  @Override
  protected Collection<RangeState> handleStatementEdge(
      CStatementEdge cfaEdge, CStatement statement) throws CPATransferException {
    RangeState newState = RangeState.copyOf(state);
    if (statement instanceof CAssignment) {
      CAssignment assign = (CAssignment) statement;
      CExpression op1 = assign.getLeftHandSide();
      CRightHandSide op2 = assign.getRightHandSide();
      AccessPath leftPath = getAccessPath(op1);
      if (leftPath == null) {
        // in this case, state keeps unchanged since we do not know how state changes
        return Collections.singleton(newState);
      }
      // if op2 is also a left-hand-value, we should search for all access paths the prefix of
      // which is the access path of op2 and then transfer them to op1
      addRange(newState, otherStates, leftPath, op2, null);
    } else if (statement instanceof CFunctionCallStatement) {
      CFunctionCallExpression functionCall = ((CFunctionCallStatement) statement)
          .getFunctionCallExpression();
      // FIX: if we encounter exit(), we should stop the analysis
      CExpression nameExpression = functionCall.getFunctionNameExpression();
      if (nameExpression instanceof CIdExpression && GlobalInfo.getInstance().queryStopFunction
          (((CIdExpression) nameExpression).getName())) {
        return Collections.emptySet();
      }
      evaluateRange(newState, otherStates, functionCall);
    }
    return Collections.singleton(newState);
  }

  @Override
  protected Collection<RangeState> handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {
    RangeState newState = RangeState.copyOf(state);
    Optional<CAssignment> orAssign = cfaEdge.asAssignment();
    if (orAssign.isPresent()) {
      CAssignment assign = orAssign.get();
      CLeftHandSide leftHand = assign.getLeftHandSide();
      CType returnType = leftHand.getExpressionType();
      AccessPath path = getAccessPath(leftHand);
      // it is unclear if forced conversion leads to runtime errors
      addRange(newState, otherStates, path, assign.getRightHandSide(), returnType);
    }
    return Collections.singleton(newState);
  }

  @Override
  protected Collection<RangeState> handleBlankEdge(BlankEdge cfaEdge) {
    RangeState newState = state;
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      newState = RangeState.copyOf(state);
      newState.dropFrame(functionName);
    }
    return Collections.singleton(newState);
  }

  @Override
  protected Collection<RangeState> handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) {
    // nothing to do
    return Collections.singleton(state);
  }

  @Override
  protected Collection<RangeState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    Range assumeRange = evaluateRange(state, otherStates, expression);
    if (assumeRange.isEmpty() || (truthAssumption ? Range.ZERO : Range.ONE).equals(assumeRange)) {
      // the assumption is unsatisfiable
      return Collections.emptySet();
    }

    // if a IF-branch has a complicated condition expression, it will be decomposed into multiple
    // simple branching conditions in post-processed CFA
    BinaryOperator operator = ((CBinaryExpression) expression).getOperator();
    CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
    if (!truthAssumption) {
      operator = negateOperator(operator);
    }
    RangeState newState = RangeState.copyOf(state);
    ExpressionRangeVisitor visitor = new ExpressionRangeVisitor(state, otherStates, machineModel,
        false);
    Range range1 = operand1.accept(visitor);
    Range range2 = operand2.accept(visitor);
    // NOTE: the following operations are essentially refinements. We should use existing
    // refiner for better implementation
    Range restrict1, restrict2;
    switch (operator) {
      case LESS_THAN:
        restrict1 = range1.limitUpperBoundBy(range2.minus(1L));
        restrict2 = range2.limitLowerBoundBy(range1.plus(1L));
        newState = operand1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, false));
        newState = operand2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, false));
        forFurtherRefine.add(new BindRefinePair(operand1, restrict1));
        forFurtherRefine.add(new BindRefinePair(operand2, restrict2));
        return Collections.singleton(newState);
      case LESS_EQUAL:
        restrict1 = range1.limitUpperBoundBy(range2);
        restrict2 = range2.limitLowerBoundBy(range1);
        newState = operand1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, false));
        newState = operand2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, false));
        forFurtherRefine.add(new BindRefinePair(operand1, restrict1));
        forFurtherRefine.add(new BindRefinePair(operand2, restrict2));
        return Collections.singleton(newState);
      case GREATER_THAN:
        restrict1 = range1.limitLowerBoundBy(range2.plus(1L));
        restrict2 = range2.limitUpperBoundBy(range1.minus(1L));
        newState = operand1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, false));
        newState = operand2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, false));
        forFurtherRefine.add(new BindRefinePair(operand1, restrict1));
        forFurtherRefine.add(new BindRefinePair(operand2, restrict2));
        return Collections.singleton(newState);
      case GREATER_EQUAL:
        restrict1 = range1.limitLowerBoundBy(range2);
        restrict2 = range2.limitUpperBoundBy(range1);
        newState = operand1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, false));
        newState = operand2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, false));
        forFurtherRefine.add(new BindRefinePair(operand1, restrict1));
        forFurtherRefine.add(new BindRefinePair(operand2, restrict2));
        return Collections.singleton(newState);
      case EQUALS:
        restrict1 = range1.intersect(range2);
        restrict2 = range2.intersect(range1);
        newState = operand1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
            machineModel, false));
        newState = operand2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
            machineModel, false));
        forFurtherRefine.add(new BindRefinePair(operand1, restrict1));
        forFurtherRefine.add(new BindRefinePair(operand2, restrict2));
        return Collections.singleton(newState);
      case NOT_EQUALS:
        // FIXME: here the analysis is unsound
        if (range2.getLow().equals(range2.getHigh())) {
          restrict1 = splitRange(range1, range2);
          restrict2 = null;
        } else if (range1.getLow().equals(range1.getHigh())) {
          restrict2 = splitRange(range2, range1);
          restrict1 = null;
        } else {
          Pair<Range, Range> splitResult = splitRanges(range1, range2);
          restrict1 = splitResult.getFirst();
          restrict2 = splitResult.getSecond();
        }
        if (restrict1 != null) {
          newState = operand1.accept(new RangeRefineVisitor(newState, otherStates, restrict1,
              machineModel, false));
          forFurtherRefine.add(new BindRefinePair(operand1, restrict1));
        }
        if (restrict2 != null) {
          newState = operand2.accept(new RangeRefineVisitor(newState, otherStates, restrict2,
              machineModel, false));
          forFurtherRefine.add(new BindRefinePair(operand2, restrict2));
        }
        return Collections.singleton(newState);
      default:
        throw new UnrecognizedCCodeException("unexpected operator in assumption", edge, expression);
    }
  }

  /**
   * Split the first range according to the unit range given by the second argument.
   *
   * @param pRange1 The first range to be split
   * @param pRange2 The second range, should be a unit range
   * @return split range
   */
  private Range splitRange(Range pRange1, Range pRange2) {
    assert (pRange2.getLow().equals(pRange2.getHigh()));
    Range partOne = pRange1.intersect(Range.upperBoundedRange(pRange2.getLow().subtract
        (CompIntegers.ALMOST_ZERO_DELTA)));
    Range partTwo = pRange1.intersect(Range.lowerBoundedRange(pRange2.getLow().add(CompIntegers
        .ALMOST_ZERO_DELTA)));
    // FIXME: unsound approximation, but useful for many cases
    if (!partTwo.isEmpty()) {
      return partTwo;
    } else if (!partOne.isEmpty()) {
      return partOne;
    } else {
      return Range.EMPTY;
    }
  }

  /**
   * Split two ranges. The resultant ranges should not be overlapped.
   */
  private Pair<Range, Range> splitRanges(Range pRange1, Range pRange2) {
    Range sRange1, sRange2;
    sRange1 = pRange1.intersect(Range.upperBoundedRange(pRange2.getLow().subtract(CompIntegers
        .ALMOST_ZERO_DELTA)));
    sRange2 = pRange2.intersect(Range.lowerBoundedRange(pRange1.getHigh().add(CompIntegers
        .ALMOST_ZERO_DELTA)));
    if (!sRange1.isEmpty() && !sRange2.isEmpty()) {
      return Pair.of(sRange1, sRange2);
    }
    sRange1 = pRange1.intersect(Range.lowerBoundedRange(pRange2.getHigh().add(CompIntegers
        .ALMOST_ZERO_DELTA)));
    sRange2 = pRange2.intersect(Range.upperBoundedRange(pRange1.getLow().subtract(CompIntegers
        .ALMOST_ZERO_DELTA)));
    // if there still exists empty range(s) after splitting, we just return them
    return Pair.of(sRange1, sRange2);
  }

  @Override
  protected Collection<RangeState> handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {

    if (cfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert parameters.size() <= arguments.size();
      // logger.log(Level.WARNING, "Ignoring parameters passed as varargs to function", cfaEdge
      // .getSuccessor().getFunctionDefinition().toASTString());
    } else {
      assert parameters.size() == arguments.size();
    }

    RangeState newState = RangeState.copyOf(state);
    for (int i = 0; i < parameters.size(); i++) {
      AccessPath formalParameterPath = getAccessPath(parameters.get(i));
      addRange(newState, otherStates, formalParameterPath, arguments.get(i),
          parameters.get(i).getType());
    }
    return Collections.singleton(newState);
  }

  @Override
  protected Collection<RangeState> handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName) throws CPATransferException {
    RangeState newState = RangeState.copyOf(state);
    Optional<CVariableDeclaration> returnVar = fnkCall.getFunctionEntry().getReturnVariable();
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement functionExpression = (CFunctionCallAssignmentStatement)
          summaryExpr;
      // since this is a function call assignment, there should exist a return variable
      if (returnVar.isPresent()) {
        AccessPath leftPath = getAccessPath(functionExpression.getLeftHandSide());
        addRange(newState, leftPath, returnVar.get());
      }
    } else if (summaryExpr instanceof CFunctionCallStatement) {
      // nothing to do
    } else {
      throw new UnrecognizedCCodeException("on function return", edge, summaryExpr);
    }
    newState.dropFrame(functionName);
    return Collections.singleton(newState);
  }

  @Override
  protected Collection<RangeState> handleMultiEdge(MultiEdge pCfaEdge) throws CPATransferException {
    Collection<RangeState> frontier = Collections.singleton(state);
    for (final CFAEdge innerEdge : pCfaEdge) {
      edge = innerEdge;
      final Collection<RangeState> workQueue = new HashSet<>();
      for (RangeState frontierState : frontier) {
        state = frontierState;
        Collection<RangeState> intermediates = handleSimpleEdge(innerEdge);
        workQueue.addAll(intermediates);
      }
      frontier = workQueue;
    }
    return frontier;
  }

  @Override
  protected Collection<RangeState> postProcessing(@Nullable Collection<RangeState> successor) {
    return successor;
  }

  private static BinaryOperator negateOperator(BinaryOperator operator) {
    switch (operator) {
      case EQUALS:
        return BinaryOperator.NOT_EQUALS;
      case NOT_EQUALS:
        return BinaryOperator.EQUALS;
      case GREATER_EQUAL:
        return BinaryOperator.LESS_THAN;
      case GREATER_THAN:
        return BinaryOperator.LESS_EQUAL;
      case LESS_THAN:
        return BinaryOperator.GREATER_EQUAL;
      case LESS_EQUAL:
        return BinaryOperator.GREATER_THAN;
      default:
        return operator;
    }
  }

  private AccessPath getAccessPath(CSimpleDeclaration declaration) {
    return new AccessPath(declaration);
  }

  @Nullable
  private AccessPath getAccessPath(CLeftHandSide leftHandExpression) {
    LeftHandAccessPathVisitor leftHandVisitor = new LeftHandAccessPathVisitor(new
        ExpressionRangeVisitor(state, otherStates, machineModel, false));
    AccessPath path;
    try {
      path = leftHandExpression.accept(leftHandVisitor).orNull();
    } catch (UnrecognizedCCodeException e) {
      path = null;
    }
    return path;
  }

  @Nullable
  private AccessPath getAccessPath(CExpression expression) {
    if (expression instanceof CLeftHandSide) {
      return getAccessPath((CLeftHandSide) expression);
    }
    // otherwise, there is no access path
    return null;
  }

  /**
   * This function handles more cases, such as updating abstract state with respect to a
   * structure/union/array
   *
   * @param newState abstract state to be updated
   * @param pPath    access path of left-hand-side
   * @param e        the right-hand-side expression
   */
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
        AccessPath rightPath = getAccessPath((CLeftHandSide) e);
        if (rightPath != null) {
          AccessPath newPath = AccessPath.copyOf(path);
          newState.replaceAndCopy(rightPath, newPath, false);
          // if the access path of the RHS corresponds to a leaf node of range tree, we should
          // normalize its range by the restrict type
          Range updatedRange = newState.getRange(newPath, machineModel);
          if (restrictType != null) {
            Range typeRange = Ranges.getTypeRange(restrictType, machineModel);
            if (!typeRange.contains(updatedRange)) {
              newState.addRange(newPath, typeRange, false);
            }
          }
        }
      } else {
        // other cases are trivial
        Range resultRange = evaluateRange(newState, pOtherStates, e);
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
                newState.replaceAndCopy(previousPath, newPath, false);
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

  private void addRange(
      RangeState newState, @Nullable AccessPath path, CVariableDeclaration
      declaration) throws UnrecognizedCCodeException {
    if (path != null) {
      AccessPath rightPath = new AccessPath(declaration);
      newState.replaceAndCopy(rightPath, path, false);
    }
  }

  private void addRange(RangeState newState, @Nullable AccessPath path, Range range) {
    if (path != null) {
      newState.addRange(path, range, false);
    }
  }

  private Range evaluateRange(
      RangeState readableState, List<AbstractState> pOtherStates,
      CRightHandSide expression)
      throws UnrecognizedCCodeException {
    ExpressionRangeVisitor visitor;
    if (truncatedCastNotAllowed) {
      visitor = new ExpressionRangeVisitor(readableState, pOtherStates, machineModel, false,
          true);
    } else {
      visitor = new ExpressionRangeVisitor(readableState, pOtherStates, machineModel, false);
    }
    return expression.accept(visitor);
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

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    if (cfaEdge == null) {
      return null;
    }
    setInfo(pState, otherStates, precision, cfaEdge);
    RangeState rangeState = (RangeState) pState;
    // bind analysis
    if (!forFurtherRefine.isEmpty()) {
      BindState bindState = AbstractStates.extractStateByType(otherStates, BindState.class);
      if (bindState != null) {
        // we can make use of bind CPA here
        for (BindRefinePair pair : forFurtherRefine) {
          CExpression expression = pair.getExpression();
          Range restrict = pair.getRestrictRange();
          if (expression instanceof CLeftHandSide) {
            Optional<AccessPath> orPath = ((CLeftHandSide) expression).accept(
                new LeftHandAccessPathVisitor(
                    new ExpressionRangeVisitor(rangeState, otherStates, machineModel, false)));
            AccessPath path = orPath.orNull();
            if (path != null) {
              List<CRightHandSide> associatedExpressions = bindState.getBindedExpression(path,
                  otherStates, 3);
              for (CRightHandSide singleExp : associatedExpressions) {
                rangeState = singleExp.accept(new RangeRefineVisitor(rangeState,
                    otherStates, restrict, machineModel, false));
              }
            }
          }
        }
      }
      forFurtherRefine.clear();
    }
    resetInfo();
    return Collections.singleton(rangeState);
  }

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

    // load state and precision info into shared variables
    // these info can be overwritten in transfer relation computation
    setInfo(preState, preOtherState, precision, cfaEdge);

    RangeState rangePreState = (RangeState) preState;

    // initialize fix information data first
    if (fixInfo == null) {
      FixInformation generalInfo = FixProvider.getFixInfo(BugCategory.INTEGER);
      if (generalInfo == null) {
        IntegerFixInfo newInfo = new IntegerFixInfo();
        FixProvider.register(BugCategory.INTEGER, newInfo, IntegerFixGenerationPhase.class,
            IntegerFixApplicationPhase.class);
        generalInfo = newInfo;
      }
      fixInfo = (IntegerFixInfo) generalInfo;
    }

    // handle CFA edge by different edge type
    switch (cfaEdge.getEdgeType()) {
      case BlankEdge:
        // do nothing
        return Collections.singleton(preState);
      case AssumeEdge: {
        CExpression condition = ((CAssumeEdge) cfaEdge).getExpression();
        ExpressionCell<RangeState, Range> cell = condition.accept(new RangeCheckExpressionVisitor
            (rangePreState, preOtherState, cfaEdge));
        return Collections.singleton(cell.getState());
      }
      case StatementEdge: {
        CStatement statement = ((CStatementEdge) cfaEdge).getStatement();
        RangeState newState = statement.accept(new RangeCheckStatementVisitor
            (rangePreState, preOtherState, cfaEdge));
        return Collections.singleton(newState);
      }
      case DeclarationEdge: {
        CDeclaration declaration = ((CDeclarationEdge) cfaEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration varDeclaration = (CVariableDeclaration) declaration;
          CType declaredType = varDeclaration.getType();
          CInitializer initializer = varDeclaration.getInitializer();

          // record file location of declaration
          CSimpleType intType = Types.toIntegerType(declaredType);
          if (intType != null) {
            String declaredName = varDeclaration.getQualifiedName();
            fixInfo.addNameLocationBinding(declaredName, varDeclaration.
                getFileLocation());
            fixInfo.addNameTypeBinding(declaredName, intType);
            // add 2 constraints for the declaration as `T x`
            // (1) the actual type of x should be larger than the declared type T
            // (2) the actual type of x should be equivalent to T (so as to prevent unnecessary
            // code modification)
            fixInfo.addTypeConstraint(IntegerTypePredicate.COVER_DECLARE, declaredName,
                intType, true);
            fixInfo.addTypeConstraint(IntegerTypePredicate.EQUAL, declaredName, intType, true);
            if (initializer == null) {
              CStorageClass storageClass = varDeclaration.getCStorageClass();
              if (storageClass == CStorageClass.EXTERN) {
                // external declaration: forced declared type
                fixInfo.addTypeConstraint(IntegerTypePredicate.EQUAL, declaredName, intType, false);
              }
            }
          }

          if (initializer == null) {
            // no error occurs
            return Collections.singleton(rangePreState);
          }
          return checkAndRefineInitializer(rangePreState, preOtherState, varDeclaration,
              declaredType, initializer, cfaEdge);
        }
        // otherwise, we just return the previous state
        return Collections.singleton(rangePreState);
      }
      case ReturnStatementEdge: {
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) cfaEdge;
        Optional<CAssignment> orAssign = returnEdge.asAssignment();
        if (orAssign.isPresent()) {
          CAssignment assignment = orAssign.get();
          RangeState newState = assignment.accept(new RangeCheckStatementVisitor(rangePreState,
              preOtherState, cfaEdge));
          return Collections.singleton(newState);
        }
        return Collections.singleton(rangePreState);
      }
      case FunctionCallEdge: {
        // check the error status of each argument
        // NOTE: parameter type information is required for conversion error detection, however
        // in this case we do not explicitly pass it to checker. Therefore, summary edge should
        // be open for correct analysis. When traversing function summary edge, analyzer treats
        // function call as function call expression, and conversion checker works for this case
        // because expression checker works prior to the computation of transfer relation.
        List<CExpression> arguments = ((CFunctionCallEdge) cfaEdge).getArguments();
        CFunctionEntryNode entryNode = (CFunctionEntryNode) cfaEdge.getSuccessor();
        List<CParameterDeclaration> params = entryNode.getFunctionParameters();
        // record the location of parameter declarations
        for (CParameterDeclaration param : params) {
          fixInfo.addNameLocationBinding(param.getQualifiedName(), param.getFileLocation());
          CType declaredType = param.getType();
          CSimpleType intType = Types.toIntegerType(declaredType);
          if (intType != null) {
            String declaredName = param.getQualifiedName();
            fixInfo.addNameLocationBinding(declaredName, param.getFileLocation());
            fixInfo.addNameTypeBinding(declaredName, intType);
            fixInfo.addTypeConstraint(IntegerTypePredicate.COVER_DECLARE, declaredName, intType,
                true);
            fixInfo.addTypeConstraint(IntegerTypePredicate.EQUAL, declaredName, intType, true);
          }
        }

        RangeState currentState = rangePreState;
        // check function arguments
        // If an argument does not have matching parameter (which is the case when we handle
        // variadic function call), then we do not check if it is safe to pass the argument into
        // the target function.
        for (int i = 0; i < arguments.size(); i++) {
          CExpression argument = arguments.get(i);
          if (i < params.size()) {
            // matching parameter declaration exists
            CParameterDeclaration parameter = params.get(i);
            CIdExpression leftIdentifier = new CIdExpression(FileLocation.DUMMY, parameter);
            // If argument passing has error, then the error location should be the location of
            // the argument expression.
            CExpressionAssignmentStatement dummyAssign = new CExpressionAssignmentStatement
                (argument.getFileLocation(), leftIdentifier, argument);
            currentState = dummyAssign.accept(new RangeCheckStatementVisitor(currentState,
                preOtherState, cfaEdge));
          } else {
            ExpressionCell<RangeState, Range> cell = argument.accept(new
                RangeCheckExpressionVisitor(currentState, preOtherState, cfaEdge));
            currentState = cell.getState();
          }
        }
        return Collections.singleton(currentState);
      }
      case FunctionReturnEdge: {
        FunctionReturnEdge returnEdge = (FunctionReturnEdge) cfaEdge;
        FunctionSummaryEdge summaryEdge = returnEdge.getSummaryEdge();
        CFunctionCall functionCall = (CFunctionCall) summaryEdge.getExpression();
        if (functionCall instanceof CFunctionCallAssignmentStatement) {
          AccessPath leftPath = getAccessPath(((CFunctionCallAssignmentStatement) functionCall)
              .getLeftHandSide());
          if (leftPath != null) {
            // examine if the right-hand-side has integer type
            CSimpleType returnType = Types.toIntegerType(((CFunctionCallAssignmentStatement)
                functionCall).getRightHandSide().getExpressionType());
            if (returnType != null) {
              fixInfo.addTypeConstraint(IntegerTypePredicate.COVER, leftPath, returnType, true);
            }
          }
        }
        return Collections.singleton(rangePreState);
      }
      case MultiEdge: {
        ImmutableList<CFAEdge> edges = ((MultiEdge) cfaEdge).getEdges();
        List<RangeState> initialStates = new ArrayList<>(1);
        initialStates.add(rangePreState);
        for (CFAEdge edge0 : edges) {
          List<RangeState> currentPassStates = new ArrayList<>();
          for (RangeState initialState : initialStates) {
            Collection<? extends AbstractState> nextStates = checkAndRefineExpression(initialState,
                preOtherState, precision, edge0);
            // unsafe conversion, but this is feasible
            Collection<RangeState> nextRangeStates = (Collection<RangeState>) nextStates;
            currentPassStates.addAll(nextRangeStates);
          }
          initialStates.clear();
          initialStates.addAll(currentPassStates);
        }
        return initialStates;
      }
      case CallToReturnEdge: {
        // please refer to "FunctionCallEdge" case for more details
        CFunctionCall functionCall = ((CFunctionSummaryEdge) cfaEdge).getExpression();
        CFunctionCallExpression callExpression = functionCall.getFunctionCallExpression();
        ExpressionCell<RangeState, Range> cell = callExpression.accept(new
            RangeCheckExpressionVisitor(rangePreState, preOtherState, cfaEdge));
        return Collections.singleton(cell.getState());
      }
      default:
        throw new AssertionError("unknown CFA edge type: " + cfaEdge.getEdgeType());
    }

  }

  private Collection<RangeState> checkAndRefineInitializer(
      RangeState readableState,
      List<AbstractState> otherStates,
      @Nullable CVariableDeclaration declaration,
      CType type,
      CInitializer initializer,
      CFAEdge cfaEdge)
      throws CPATransferException {

    if (initializer instanceof CInitializerExpression) {
      CExpression expression = ((CInitializerExpression) initializer).getExpression();
      // we check the assignment of variable and initializer expression
      // Note: name of the left-hand-side is a dummy one only when the qualified name is unavailable
      CIdExpression leftIdentifier;
      if (declaration == null) {
        leftIdentifier = CIdExpression.DUMMY_ID(type);
      } else {
        leftIdentifier = new CIdExpression(FileLocation.DUMMY, declaration);
      }
      CExpressionAssignmentStatement assignment = new CExpressionAssignmentStatement(
          initializer.getFileLocation(), leftIdentifier, expression);
      RangeState newState = assignment.accept(new RangeCheckStatementVisitor(readableState,
          otherStates, cfaEdge));
      return Collections.singleton(newState);
    } else if (initializer instanceof CDesignatedInitializer) {
      CDesignatedInitializer dInitializer = (CDesignatedInitializer) initializer;
      List<CDesignator> designators = dInitializer.getDesignators();
      CInitializer rInitializer = dInitializer.getRightHandSide();
      CType targetType = Types.extractInnerTypeByDesignators(type, designators);
      return checkAndRefineInitializer(readableState, otherStates, null, targetType, rInitializer,
          cfaEdge);
    } else {
      // Note: initializer without designator can implicitly refer to an array element or a
      // structure member
      Preconditions.checkArgument(initializer instanceof CInitializerList);
      RangeState newState = RangeState.copyOf(readableState);
      // index for implicit array element / structure member
      int index = 0;
      CArrayType arrayType = Types.extractArrayType(type);
      CCompositeType compositeType = Types.extractCompositeType(type);
      if ((arrayType == null) == (compositeType == null)) {
        return Collections.singleton(newState);
      }
      CInitializerList iList = (CInitializerList) initializer;
      List<CInitializer> initializers = iList.getInitializers();
      List<RangeState> initialStates = new ArrayList<>(1);
      initialStates.add(newState);
      for (CInitializer singleInitializer : initializers) {
        List<RangeState> currentPassStates = new ArrayList<>();
        CType currentType;
        if (!(singleInitializer instanceof CDesignatedInitializer)) {
          if (arrayType != null) {
            currentType = arrayType.getType();
          } else {
            CCompositeTypeMemberDeclaration member = compositeType.getMembers().get(index);
            if (member != null) {
              currentType = member.getType();
              index++;
            } else {
              // unexpected case
              // FIX: how about skip this one and analyze the next one?
              continue;
            }
          }
        } else {
          // initializer with designator
          currentType = type;
          // however, we should also update index for next non-designator initializer
          List<CDesignator> designatorList = ((CDesignatedInitializer) singleInitializer)
              .getDesignators();
          if (designatorList.size() > 0) {
            CDesignator firstDesignator = designatorList.get(0);
            if (firstDesignator instanceof CArrayDesignator) {
              CExpression indexExp = ((CArrayDesignator) firstDesignator).getSubscriptExpression();
              ExpressionCell<RangeState, Range> cell = indexExp.accept(new
                  RangeCheckExpressionVisitor(newState, otherStates, cfaEdge));
              newState = cell.getState();
            } else if (firstDesignator instanceof CArrayRangeDesignator) {
              CExpression ceilExp = ((CArrayRangeDesignator) firstDesignator).getCeilExpression();
              ExpressionCell<RangeState, Range> ceilCell = ceilExp.accept(new
                  RangeCheckExpressionVisitor(newState, otherStates, cfaEdge));
              newState = ceilCell.getState();
            } else {
              String name = ((CFieldDesignator) firstDesignator).getFieldName();
              Preconditions.checkNotNull(compositeType);
              List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
              for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getName().equals(name)) {
                  index = i + 1;
                  break;
                }
              }
            }
          }
        }
        for (RangeState initialState : initialStates) {
          Collection<RangeState> nextStates = checkAndRefineInitializer(initialState,
              otherStates, null, currentType, singleInitializer, cfaEdge);
          currentPassStates.addAll(nextStates);
        }
        initialStates.clear();
        initialStates.addAll(currentPassStates);
      }
      return initialStates;
    }
  }

  @Override
  public Collection<? extends AbstractState> checkAndRefineState(
      AbstractState postState,
      List<AbstractState> postOtherStates,
      Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    RangeState rangePostState = (RangeState) postState;
    Collection<RangeState> resultStates = new ArrayList<>();
    checkerManager.checkState(rangePostState, postOtherStates, cfaEdge, resultStates);
    return resultStates;

  }

  /**
   * This class is to traverse expression and update expression cell by error checking
   * The main principle is that, the transfer relation computation should NOT be explicitly
   * included in visiting.
   */
  private class RangeCheckExpressionVisitor
      extends DefaultCExpressionVisitor<ExpressionCell<RangeState, Range>,
      CPATransferException>
      implements CRightHandSideVisitor<ExpressionCell<RangeState, Range>,
      CPATransferException> {

    // this state could be changed during analysis of sub-expression
    private RangeState internalState;
    // we do not change other components of abstract state
    private List<AbstractState> otherStates;
    // CFA edge which is used to locate the faulty code
    private CFAEdge cfaEdge;
    // a status of the visitor, indicating whether current visited expression in on the function
    // argument expression
    private boolean onFunctionArg = false;

    RangeCheckExpressionVisitor(
        RangeState pState, List<AbstractState> pOtherStates, CFAEdge
        pEdge) {
      internalState = pState;
      otherStates = pOtherStates;
      cfaEdge = pEdge;
    }

    @Override
    protected ExpressionCell<RangeState, Range> visitDefault(CExpression exp)
        throws CPATransferException {
      // no check is required
      return new ExpressionCell<>(internalState, otherStates, new ArrayList<Range>
          (), Range.UNBOUND);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws CPATransferException {
      // a function call is regarded as N-ary operation, whose operands are arguments
      List<CExpression> arguments = pIastFunctionCallExpression.getParameterExpressions();
      List<Range> operands = new ArrayList<>();
      for (CExpression argument : arguments) {
        onFunctionArg = true;
        ExpressionCell<RangeState, Range> argumentCell = argument.accept(this);
        onFunctionArg = false;
        // update internal state if necessary
        internalState = argumentCell.getState();
        operands.add(argumentCell.getResult());
      }
      // Note: we cannot directly return the product of check manager since function expression
      // possibly has side effects when being evaluated
      RangeState backupState = RangeState.copyOf(internalState);
      ExpressionCell<RangeState, Range> totalCell = RangeFunctionAdapter.instance(false)
          .evaluateFunctionCallExpression(pIastFunctionCallExpression, operands, internalState,
              otherStates);
      ExpressionCell<RangeState, Range> checkResult = checkerManager.checkExpression
          (pIastFunctionCallExpression, Range.class, totalCell, cfaEdge);
      return new ExpressionCell<>(backupState, otherStates, checkResult.getOperands(),
          checkResult.getResult());
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CBinaryExpression pIastBinaryExpression)
        throws CPATransferException {
      // first compute an initial expression cell and then perform expression check
      CExpression operand1 = pIastBinaryExpression.getOperand1();
      CExpression operand2 = pIastBinaryExpression.getOperand2();
      ExpressionCell<RangeState, Range> cell1 = operand1.accept(this);
      internalState = cell1.getState();
      ExpressionCell<RangeState, Range> cell2 = operand2.accept(this);
      internalState = cell2.getState();
      Range range1 = cell1.getResult();
      Range range2 = cell2.getResult();
      BinaryOperator operator = pIastBinaryExpression.getOperator();
      Range resultRange;
      if (operator.isLogicalOperator()) {
        resultRange = Ranges.getLogicalResult(operator, range1, range2);
      } else {
        // handle the rare case where two operands are pointers and operator is MINUS
        if (Types.isPointerType(operand1.getExpressionType()) &&
            Types.isPointerType(operand2.getExpressionType()) &&
            operator == BinaryOperator.MINUS) {
          // Pointer difference could not have integer errors since we just compute the distance
          // of given two pointers. It is possible that the given pointer is invalid (e.g. has
          // negative address). In this case, it is the responsibility of buffer analysis to
          // detect this kind of errors.
          Pair<AccessPath, Range> pathAndRange1 = getPathAndRange(operand1);
          Pair<AccessPath, Range> pathAndRange2 = getPathAndRange(operand2);
          AccessPath ap1 = pathAndRange1.getFirst();
          AccessPath ap2 = pathAndRange2.getFirst();
          if (ap1 == null || ap2 == null) {
            resultRange = Range.UNBOUND;
          } else {
            LeftHandAccessPathVisitor visitor = new LeftHandAccessPathVisitor(new
                ExpressionRangeVisitor(internalState, otherStates, machineModel, false));
            resultRange = visitor.computePointerDiff(ap1, ap2);
          }
          if (resultRange.isUnbound()) {
            resultRange = Ranges.getTypeRange(machineModel.getPointerDiffType(), machineModel);
          }
          resultRange = resultRange.plus(pathAndRange1.getSecond()).minus(pathAndRange2.getSecond
              ());
          if (resultRange.isUnbound()) {
            resultRange = defaultRange(pIastBinaryExpression);
          }
          // no check is required, so we directly return from this function
          return new ExpressionCell<>(internalState, otherStates, Lists.newArrayList(Range
              .UNBOUND, Range.UNBOUND), resultRange);
        } else if (operator == BinaryOperator.SHIFT_LEFT || operator == BinaryOperator
            .SHIFT_RIGHT) {
          // the original range should NOT be overwritten for checking purpose
          Range nr1 = range1.intersect(Range.NONNEGATIVE);
          Range nr2 = range2.intersect(Range.NONNEGATIVE);
          resultRange = Ranges.getArithmeticResult(operator, nr1, nr2, pIastBinaryExpression,
              machineModel);
        } else {
          resultRange = Ranges.getArithmeticResult(operator, range1, range2, pIastBinaryExpression,
              machineModel);
        }
      }
      List<Range> operands = Lists.newArrayList(range1, range2);
      ExpressionCell<RangeState, Range> totalCell = new ExpressionCell<>(internalState,
          otherStates, operands, resultRange);
      return checkerManager.checkExpression(pIastBinaryExpression, Range.class, totalCell, cfaEdge);
    }

    @Nonnull
    private Pair<AccessPath, Range> getPathAndRange(CExpression e)
        throws CPATransferException {
      // Precondition: the input expression has pointer type
      if (e instanceof CLeftHandSide) {
        AccessPath path = RangeState.getAccessPath(internalState, otherStates, (CLeftHandSide) e,
            machineModel);
        return Pair.of(path, Range.ZERO);
      } else if (e instanceof CUnaryExpression) {
        UnaryOperator operator = ((CUnaryExpression) e).getOperator();
        if (operator == UnaryOperator.AMPER) {
          CExpression addr = ((CUnaryExpression) e).getOperand();
          Preconditions.checkArgument(addr instanceof CLeftHandSide);
          AccessPath path = RangeState.getAccessPath(internalState, otherStates, (CLeftHandSide)
              addr, machineModel);
          if (path == null) {
            return Pair.of(null, Range.ZERO);
          }
          path.appendSegment(new AddressingSegment());
          return Pair.of(path, Range.ZERO);
        }
        return Pair.of(null, Range.ZERO);
      } else if (e instanceof CBinaryExpression) {
        BinaryOperator operator = ((CBinaryExpression) e).getOperator();
        CExpression op1 = ((CBinaryExpression) e).getOperand1();
        CExpression op2 = ((CBinaryExpression) e).getOperand2();
        if (Types.isPointerType(op1.getExpressionType())) {
          Pair<AccessPath, Range> pathAndRange = getPathAndRange(op1);
          ExpressionCell<RangeState, Range> cell = op2.accept(this);
          Range offset = cell.getResult();
          if (operator == BinaryOperator.MINUS) {
            offset = offset.negate();
          }
          return Pair.of(pathAndRange.getFirst(), offset.plus(pathAndRange.getSecond()));
        } else {
          Preconditions.checkArgument(operator == BinaryOperator.PLUS);
          Preconditions.checkArgument(Types.isPointerType(op2.getExpressionType()));
          ExpressionCell<RangeState, Range> cell = op1.accept(this);
          Range offset = cell.getResult();
          Pair<AccessPath, Range> pathAndRange = getPathAndRange(op2);
          return Pair.of(pathAndRange.getFirst(), offset.plus(pathAndRange.getSecond()));
        }
      } else if (e instanceof CCastExpression) {
        CExpression op = ((CCastExpression) e).getOperand();
        return getPathAndRange(op);
      } else {
        return Pair.of(null, Range.ZERO);
      }

    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CCastExpression pIastCastExpression)
        throws CPATransferException {
      CExpression operand = pIastCastExpression.getOperand();
      ExpressionCell<RangeState, Range> cell = operand.accept(this);
      internalState = cell.getState();
      CType castType = pIastCastExpression.getCastType();
      Range typeRange = Ranges.getTypeRange(castType, machineModel);
      Range resultRange = cell.getResult();
      if (!truncatedCastNotAllowed) {
        if (!typeRange.contains(resultRange)) {
          resultRange = typeRange;
        }
        if (Types.isNumericalType(castType) && !Types.isFloatType(castType)) {
          resultRange = resultRange.trunc();
        }
      }
      ExpressionCell<RangeState, Range> totalCell = new ExpressionCell<>(internalState,
          otherStates, Lists.newArrayList(cell.getResult()), resultRange);
      // if current cast expression in on the function argument, the lossy truncation is allowed
      // (otherwise, the lossy truncation still occurs even if no explicit cast is written)
      return !onFunctionArg ?
             checkerManager.checkExpression(pIastCastExpression, Range.class, totalCell, cfaEdge) :
             totalCell;
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CCharLiteralExpression pIastCharLiteralExpression)
        throws CPATransferException {
      // this is a base case, we create its expression cell from the scratch
      Range resultRange = new Range((long) pIastCharLiteralExpression.getCharacter());
      return new ExpressionCell<>(internalState, otherStates, new ArrayList<Range>(), resultRange);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
        throws CPATransferException {
      BigDecimal value = pIastFloatLiteralExpression.getValue();
      Range resultRange = new Range(new CompInteger(value));
      ExpressionCell<RangeState, Range> totalCell = new ExpressionCell<>(internalState,
          otherStates, new ArrayList<Range>(), resultRange);
      return checkerManager
          .checkExpression(pIastFloatLiteralExpression, Range.class, totalCell, cfaEdge);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
        throws CPATransferException {
      BigInteger value = pIastIntegerLiteralExpression.getValue();
      Range resultRange = new Range(new CompInteger(value));
      ExpressionCell<RangeState, Range> totalCell = new ExpressionCell<>(internalState,
          otherStates, new ArrayList<Range>(), resultRange);
      return checkerManager
          .checkExpression(pIastIntegerLiteralExpression, Range.class, totalCell, cfaEdge);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CTypeIdExpression pIastTypeIdExpression)
        throws CPATransferException {
      TypeIdOperator operator = pIastTypeIdExpression.getOperator();
      CType type = pIastTypeIdExpression.getType();
      Range resultRange;
      switch (operator) {
        case SIZEOF:
          resultRange = new Range(machineModel.getSizeof(type));
          break;
        case ALIGNOF:
          resultRange = new Range(machineModel.getAlignof(type));
          break;
        default:
          throw new AssertionError("unknown typeid keyword: " + operator);
      }
      return new ExpressionCell<>(internalState, otherStates, new ArrayList<Range>(),
          resultRange);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CUnaryExpression pIastUnaryExpression)
        throws CPATransferException {
      CExpression operand = pIastUnaryExpression.getOperand();
      ExpressionCell<RangeState, Range> cell = operand.accept(this);
      internalState = cell.getState();
      UnaryOperator operator = pIastUnaryExpression.getOperator();
      Range resultRange = Ranges.getUnaryResult(operator, cell.getResult(), pIastUnaryExpression,
          machineModel);
      ExpressionCell<RangeState, Range> totalCell = new ExpressionCell<>(internalState,
          otherStates, Lists.newArrayList(cell.getResult()), resultRange);
      return checkerManager.checkExpression(pIastUnaryExpression, Range.class, totalCell, cfaEdge);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws CPATransferException {
      // a left-hand-side expression should not be checked by checker
      // since its value is directly obtained from range state
      CExpression indexExpression = pIastArraySubscriptExpression.getSubscriptExpression();
      ExpressionCell<RangeState, Range> indexCell = indexExpression.accept(this);
      internalState = indexCell.getState();
      AccessPath path = RangeState.getAccessPath(internalState,
          otherStates, pIastArraySubscriptExpression, machineModel);
      Range resultRange;
      if (path == null) {
        resultRange = Range.UNBOUND;
      } else {
        resultRange = internalState.getRange(path, machineModel);
      }
      if (resultRange.isUnbound()) {
        resultRange = defaultRange(pIastArraySubscriptExpression);
      }
      ExpressionCell<RangeState, Range> totalCell = new ExpressionCell<>(internalState,
          otherStates, Lists.newArrayList(indexCell.getResult()), resultRange);
      return checkerManager.checkExpression(pIastArraySubscriptExpression, Range.class,
          totalCell, cfaEdge);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CFieldReference pIastFieldReference)
        throws CPATransferException {
      AccessPath path = RangeState.getAccessPath(internalState, otherStates, pIastFieldReference,
          machineModel);
      Range resultRange = internalState.getRange(path, machineModel);
      if (resultRange.isUnbound()) {
        resultRange = defaultRange(pIastFieldReference);
      }
      return new ExpressionCell<>(internalState, otherStates, new ArrayList<Range>(), resultRange);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CIdExpression pIastIdExpression)
        throws CPATransferException {
      AccessPath path = RangeState.getAccessPath(internalState, otherStates, pIastIdExpression,
          machineModel);
      Preconditions.checkNotNull(path);
      Range resultRange = internalState.getRange(path, machineModel);
      if (resultRange.isUnbound()) {
        resultRange = defaultRange(pIastIdExpression);
      }
      return new ExpressionCell<>(internalState, otherStates, new ArrayList<Range>(), resultRange);
    }

    @Override
    public ExpressionCell<RangeState, Range> visit(CPointerExpression pointerExpression)
        throws CPATransferException {
      // obtain point-to information by strengthening with shape state
      AccessPath path = RangeState.getAccessPath(internalState, otherStates, pointerExpression,
          machineModel);
      Range resultRange = internalState.getRange(path, machineModel);
      Range typeRange = Ranges.getTypeRange(pointerExpression, machineModel);
      // restrict the range in its type range
      resultRange = resultRange.intersect(typeRange);
      return new ExpressionCell<>(internalState, otherStates, new ArrayList<Range>(), resultRange);
    }

    private Range defaultRange(CExpression exp) {
      return Ranges.getTypeRange(exp, machineModel);
    }

  }

  private class RangeCheckStatementVisitor
      implements CStatementVisitor<RangeState, CPATransferException> {

    private RangeState internalState;
    private List<AbstractState> otherStates;
    private CFAEdge cfaEdge;

    RangeCheckStatementVisitor(RangeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge) {
      internalState = pState;
      otherStates = pOtherStates;
      cfaEdge = pEdge;
    }

    @Override
    public RangeState visit(CExpressionStatement pIastExpressionStatement)
        throws CPATransferException {
      CExpression expression = pIastExpressionStatement.getExpression();
      // an expression statement could change the state, however, it is the developer's
      // responsibility to implement visiting method of function call expression to change the
      // state, not in this function
      ExpressionCell<RangeState, Range> cell = expression.accept(new RangeCheckExpressionVisitor
          (internalState, otherStates, cfaEdge));
      return cell.getState();
    }

    @Override
    public RangeState visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
        throws CPATransferException {
      // For assignment, we first evaluate the right-hand-side expression, and then use check
      // assignment method to refine the assignment cell, finally we change the state before
      // assignment and return the resultant state
      CExpression leftHand = pIastExpressionAssignmentStatement.getLeftHandSide();
      ExpressionCell<RangeState, Range> leftCell = leftHand.accept(new
          RangeCheckExpressionVisitor(internalState, otherStates, cfaEdge));
      internalState = leftCell.getState();
      CExpression rightHand = pIastExpressionAssignmentStatement.getRightHandSide();
      ExpressionCell<RangeState, Range> rightCell = rightHand.accept(new
          RangeCheckExpressionVisitor(internalState, otherStates, cfaEdge));
      internalState = rightCell.getState();
      Range rightRange = rightCell.getResult();
      AssignmentCell<RangeState, Range> initialCell = new AssignmentCell<>(internalState,
          otherStates, rightRange);
      AssignmentCell<RangeState, Range> endCell = checkerManager.checkAssignment
          (pIastExpressionAssignmentStatement, Range.class, initialCell, cfaEdge);
      internalState = endCell.getState();
      return internalState;
    }

    @Override
    public RangeState visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
        throws CPATransferException {
      CExpression leftHand = pIastFunctionCallAssignmentStatement.getLeftHandSide();
      ExpressionCell<RangeState, Range> leftCell = leftHand.accept(new
          RangeCheckExpressionVisitor(internalState, otherStates, cfaEdge));
      internalState = leftCell.getState();
      CFunctionCallExpression rightHand = pIastFunctionCallAssignmentStatement.getRightHandSide();
      ExpressionCell<RangeState, Range> rightCell = rightHand.accept(new
          RangeCheckExpressionVisitor(internalState, otherStates, cfaEdge));
      internalState = rightCell.getState();
      Range rightRange = rightCell.getResult();
      AssignmentCell<RangeState, Range> initialCell = new AssignmentCell<>(internalState,
          otherStates, rightRange);
      AssignmentCell<RangeState, Range> endCell = checkerManager.checkAssignment
          (pIastFunctionCallAssignmentStatement, Range.class, initialCell, cfaEdge);
      internalState = endCell.getState();
      return internalState;
    }

    @Override
    public RangeState visit(CFunctionCallStatement pIastFunctionCallStatement)
        throws CPATransferException {
      // the same as the first visiting method
      CFunctionCallExpression expression = pIastFunctionCallStatement.getFunctionCallExpression();
      ExpressionCell<RangeState, Range> cell = expression.accept(new RangeCheckExpressionVisitor
          (internalState, otherStates, cfaEdge));
      return cell.getState();
    }
  }
}
