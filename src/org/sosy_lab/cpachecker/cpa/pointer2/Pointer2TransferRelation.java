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
package org.sosy_lab.cpachecker.cpa.pointer2;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer2.util.AccessSummaryApplicator;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.PointerVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Pointer2TransferRelation extends SingleEdgeTransferRelation {

  private final MachineModel machineModel;

  Pointer2TransferRelation() {
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new IllegalArgumentException("CFA info required for creating pointer transfer "
          + "relation");
    }
    machineModel = cfaInfo.getCFA().getMachineModel();
    // initialize summary applicator
    AccessSummaryApplicator.initialize(machineModel);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    Pointer2State currentState = (Pointer2State) state;
    Pointer2State resultState = getAbstractSuccessor(currentState, otherStates, cfaEdge);
    return resultState == null ? Collections.<AbstractState>emptySet() : Collections.singleton
        (resultState);
  }

  /* ************* */
  /* edge handlers */
  /* ************* */

  @Nullable
  private Pointer2State getAbstractSuccessor(
      Pointer2State pState, List<AbstractState>
      pOtherStates, CFAEdge pCFAEdge) throws CPATransferException {
    Pointer2State resultState = pState;
    switch (pCFAEdge.getEdgeType()) {
      case AssumeEdge:
        resultState = handleAssumeEdge(resultState, pOtherStates, (CAssumeEdge) pCFAEdge);
        break;
      case DeclarationEdge:
        resultState = handleDeclarationEdge(resultState, pOtherStates, (CDeclarationEdge) pCFAEdge);
        break;
      case FunctionCallEdge:
        resultState =
            handleFunctionCallEdge(resultState, pOtherStates, (CFunctionCallEdge) pCFAEdge);
        break;
      case MultiEdge:
        for (CFAEdge edge : ((MultiEdge) pCFAEdge)) {
          resultState = getAbstractSuccessor(resultState, pOtherStates, edge);
          if (resultState == null) {
            return null;
          }
        }
        break;
      case ReturnStatementEdge:
        resultState = handleReturnStatementEdge(resultState, pOtherStates,
            (CReturnStatementEdge) pCFAEdge);
        break;
      case StatementEdge:
        resultState = handleStatementEdge(resultState, pOtherStates, (CStatementEdge) pCFAEdge);
        break;
      case CallToReturnEdge:
        resultState = handleFunctionReturnEdge(resultState, pOtherStates, (CFunctionReturnEdge)
            pCFAEdge);
        break;
      case BlankEdge:
      case FunctionReturnEdge:
        break;
      default:
        throw new UnrecognizedCCodeException("unrecognized CFA edge:", pCFAEdge);
    }
    return resultState;
  }

  private Pointer2State handleReturnStatementEdge(
      Pointer2State pState, List<AbstractState>
      pOtherStates, CReturnStatementEdge pEdge) throws UnrecognizedCCodeException {
    if (!pEdge.getExpression().isPresent()) {
      return pState;
    }
    Optional<? extends AVariableDeclaration> returnVar = pEdge.getSuccessor().getEntryNode()
        .getReturnVariable();
    if (!returnVar.isPresent()) {
      return pState;
    }
    return handleAssignment(pState, pOtherStates, MemoryLocation.valueOf(returnVar.get()
        .getQualifiedName()), pEdge.getExpression().get());
  }

  private Pointer2State handleFunctionReturnEdge(
      Pointer2State pState, List<AbstractState>
      pOtherStates, CFunctionReturnEdge pEdge) throws UnrecognizedCCodeException {
    Pointer2State newState = pState;
    Optional<CVariableDeclaration> returnVar = pEdge.getFunctionEntry().getReturnVariable();
    CFunctionSummaryEdge summaryEdge = pEdge.getSummaryEdge();
    CFunctionCall summaryExpr = summaryEdge.getExpression();
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignment = (CFunctionCallAssignmentStatement) summaryExpr;
      if (returnVar.isPresent()) {
        // compute the location set of LHS
        LocationSet lhsLoc = asLocations(pState, pOtherStates, assignment.getLeftHandSide(), 0,
            machineModel);
        LocationSet rhsLoc = pState.getPointsToSet(MemoryLocation.valueOf(returnVar.get()
            .getQualifiedName()));
        newState = handleAssignment(pState, lhsLoc, rhsLoc);
      }
    }
    // drop stack frame: variables in current function scope should be invalidated
    return newState.dropFrame(pEdge.getFunctionEntry().getFunctionName());
  }

  private Pointer2State handleFunctionCallEdge(
      Pointer2State pState, List<AbstractState>
      pOtherStates, CFunctionCallEdge pEdge) throws UnrecognizedCCodeException {
    Pointer2State newState = pState;
    List<CParameterDeclaration> params = pEdge.getSuccessor()
        .getFunctionParameters();
    List<CExpression> args = pEdge.getArguments();
    // for handling variadic functions, we should limit first-k parameter/argument matches
    int minSize = Math.min(params.size(), args.size());
    params = FluentIterable.from(params).limit(minSize).toList();
    args = FluentIterable.from(args).limit(minSize).toList();
    assert (params.size() == args.size());
    for (Pair<CParameterDeclaration, CExpression> argPair : Pair.zipList(params, args)) {
      CExpression arg = argPair.getSecond();
      CParameterDeclaration param = argPair.getFirstNotNull();
      MemoryLocation location = toLocation(param);
      newState = handleAssignment(newState, pOtherStates, location, arg);
    }
    // handle the remaining parameters: is it possible?
    for (CParameterDeclaration param : FluentIterable.from(params).skip(minSize)) {
      MemoryLocation location = toLocation(param);
      newState = newState.setPointsToInformation(location, LocationSetBot.INSTANCE);
    }
    return newState;
  }

  private Pointer2State handleDeclarationEdge(
      Pointer2State pState, List<AbstractState>
      pOtherStates, CDeclarationEdge pEdge) throws UnrecognizedCCodeException {
    CDeclaration declaration = pEdge.getDeclaration();
    if (!(declaration instanceof CVariableDeclaration)) {
      return pState;
    }
    CVariableDeclaration varDeclaration = (CVariableDeclaration) declaration;
    // register type information of this variable declaration
    String qualifiedName = varDeclaration.getQualifiedName();
    CType declaredType = varDeclaration.getType();
    CInitializer initializer = varDeclaration.getInitializer();
    if (initializer == null) {
      return pState;
    }
    MemoryLocation baseLoc = MemoryLocation.valueOf(qualifiedName);
    return handleInitializer(pState, pOtherStates, baseLoc, declaredType, initializer);
  }

  private Pointer2State handleInitializer(
      Pointer2State pState, List<AbstractState> pOtherStates,
      MemoryLocation basicLoc, CType pType,
      @Nonnull CInitializer pInitializer)
      throws UnrecognizedCCodeException {
    if (pInitializer instanceof CInitializerExpression) {
      CExpression exp = ((CInitializerExpression) pInitializer).getExpression();
      return handleAssignment(pState, pOtherStates, basicLoc, exp);
    } else if (pInitializer instanceof CDesignatedInitializer) {
      CDesignatedInitializer initializer = (CDesignatedInitializer) pInitializer;
      List<CDesignator> designators = initializer.getDesignators();
      CInitializer subInitializer = initializer.getRightHandSide();
      Pointer2State newState = pState;
      CType currentType = pType;
      Collection<MemoryLocation> baseLoc = Collections.singleton(basicLoc);
      for (CDesignator designator : designators) {
        Collection<MemoryLocation> newLoc = new HashSet<>();
        if (designator instanceof CArrayDesignator) {
          CExpression indexExp = ((CArrayDesignator) designator).getSubscriptExpression();
          Long indexValue = evaluateExpression(pOtherStates, indexExp, machineModel);
          if (indexValue == null) {
            // we do not know how to handle this case, for now we just discard the remaining
            // initializers
            return newState;
          }
          CArrayType arrayType = Types.extractArrayType(currentType);
          if (arrayType == null) {
            throw new UnrecognizedCCodeException("illegal designated initializer:", initializer);
          }
          currentType = arrayType.getType();
          int elementSize = machineModel.getSizeof(currentType);
          long delta = elementSize * indexValue;
          for (MemoryLocation location : baseLoc) {
            newLoc.add(MemoryLocation.withOffset(location, delta));
          }
        } else if (designator instanceof CFieldDesignator) {
          String fieldName = ((CFieldDesignator) designator).getFieldName();
          Pair<Long, CType> fieldInfo = Types.getFieldInfo(currentType, fieldName, machineModel);
          Long offset = fieldInfo.getFirst();
          currentType = fieldInfo.getSecond();
          if (offset == null || currentType == null) {
            throw new UnrecognizedCCodeException("illegal designated initializer:", initializer);
          }
          for (MemoryLocation location : baseLoc) {
            newLoc.add(MemoryLocation.withOffset(location, offset));
          }
        } else {
          CArrayRangeDesignator rangeDesignator = (CArrayRangeDesignator) designator;
          CExpression ceil = rangeDesignator.getCeilExpression();
          CExpression floor = rangeDesignator.getFloorExpression();
          Long ceilValue = evaluateExpression(pOtherStates, ceil, machineModel);
          Long floorValue = evaluateExpression(pOtherStates, floor, machineModel);
          if (ceilValue == null || floorValue == null) {
            return newState;
          }
          CArrayType arrayType = Types.extractArrayType(currentType);
          if (arrayType == null) {
            throw new UnrecognizedCCodeException("illegal designated initializer:", initializer);
          }
          currentType = arrayType.getType();
          int elementSize = machineModel.getSizeof(currentType);
          for (MemoryLocation location : baseLoc) {
            for (long i = floorValue; i <= ceilValue; i++) {
              newLoc.add(MemoryLocation.withOffset(location, i * elementSize));
            }
          }
        }
        baseLoc = newLoc;
      }
      for (MemoryLocation location : baseLoc) {
        newState = handleInitializer(newState, pOtherStates, location, currentType, subInitializer);
      }
      return newState;
    } else {
      assert (pInitializer instanceof CInitializerList);
      CInitializerList initList = (CInitializerList) pInitializer;
      // current type could only be array type or composite type
      CCompositeType compositeType = Types.extractCompositeType(pType);
      CArrayType arrayType = Types.extractArrayType(pType);
      if ((compositeType == null) == (arrayType == null)) {
        throw new UnrecognizedCCodeException("illegal initializer list:", initList);
      }
      Pointer2State newState = pState;
      if (arrayType != null) {
        CType elementType = arrayType.getType();
        int elementSize = machineModel.getSizeof(elementType);
        // index value is updated according to index value in designated initializers enclosed by
        // initializer list
        long index = 0;
        for (CInitializer initializer : initList.getInitializers()) {
          if (initializer instanceof CDesignatedInitializer) {
            List<CDesignator> designators = ((CDesignatedInitializer) initializer).getDesignators();
            if (designators.size() > 0) {
              CDesignator firstDesignator = designators.get(0);
              if (firstDesignator instanceof CArrayDesignator) {
                CExpression indexExp = ((CArrayDesignator) firstDesignator)
                    .getSubscriptExpression();
                Long indexValue = evaluateExpression(pOtherStates, indexExp, machineModel);
                if (indexValue == null) {
                  // then, we do not know where to write the new initialized value in the array,
                  // so we set index to -1, a negative value, which would cause the analysis
                  // terminate
                  index = -1;
                  continue;
                }
                index = indexValue + 1;
              } else if (firstDesignator instanceof CArrayRangeDesignator) {
                CExpression ceilExp = ((CArrayRangeDesignator) firstDesignator).getCeilExpression();
                Long ceilValue = evaluateExpression(pOtherStates, ceilExp, machineModel);
                if (ceilValue == null) {
                  index = -1;
                  continue;
                }
                index = ceilValue + 1;
              }
            }
            newState = handleInitializer(newState, pOtherStates, basicLoc, arrayType,
                initializer);
          } else {
            if (index < 0) {
              // a correct over-approximation should update targets for every element of this
              // array with TOP element
              return newState;
            }
            long delta = elementSize * index;
            MemoryLocation newBasicLoc = MemoryLocation.withOffset(basicLoc, delta);
            index++;
            newState = handleInitializer(newState, pOtherStates, newBasicLoc, elementType,
                initializer);
          }
        }
      } else {
        List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
        if (members.size() == 1) {
          CCompositeTypeMemberDeclaration onlyMember = Iterables.getOnlyElement(members);
          CArrayType onlyMemberType = Types.extractArrayType(onlyMember.getType());
          if (onlyMemberType != null) {
            return handleInitializer(newState, pOtherStates, basicLoc, onlyMemberType, initList);
          }
        }
        int index = 0;
        for (CInitializer initializer : initList.getInitializers()) {
          if (initializer instanceof CDesignatedInitializer) {
            List<CDesignator> designators = ((CDesignatedInitializer) initializer).getDesignators();
            if (designators.size() > 0) {
              CDesignator firstDesignator = designators.get(0);
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
            newState = handleInitializer(newState, pOtherStates, basicLoc, compositeType,
                initializer);
          } else {
            CCompositeTypeMemberDeclaration targetMember = members.get(index);
            index++;
            String targetName = targetMember.getName();
            Pair<Long, CType> fieldInfo = Types.getFieldInfo(compositeType, targetName,
                machineModel);
            Long offset = fieldInfo.getFirstNotNull();
            CType memberType = fieldInfo.getSecondNotNull();
            MemoryLocation newBasicLoc = MemoryLocation.withOffset(basicLoc, offset);
            newState = handleInitializer(newState, pOtherStates, newBasicLoc, memberType,
                initializer);
          }
        }
      }
      return newState;
    }
  }

  private Pointer2State handleAssumeEdge(
      Pointer2State pState, List<AbstractState> pOtherStates,
      CAssumeEdge pEdge) throws UnrecognizedCCodeException {
    CExpression assumption = pEdge.getExpression();
    boolean truth = pEdge.getTruthAssumption();
    // perform simplification on assumption expression
    Pair<CExpression, Boolean> simplifyResult = simplifyAssumption(assumption, truth);
    assumption = simplifyResult.getFirstNotNull();
    truth = simplifyResult.getSecondNotNull();
    Pointer2State newState = pState;
    if (assumption instanceof CBinaryExpression) {
      CExpression op1 = ((CBinaryExpression) assumption).getOperand1();
      CExpression op2 = ((CBinaryExpression) assumption).getOperand2();
      BinaryOperator op = ((CBinaryExpression) assumption).getOperator();
      if (!truth) {
        op = op.getOppositeLogicalOperator();
      }
      LocationSet leftLoc = asLocations(newState, pOtherStates, op1, 1, machineModel);
      LocationSet rightLoc = asLocations(newState, pOtherStates, op2, 1, machineModel);
      if (leftLoc.isBot() || rightLoc.isBot()) {
        return newState;
      }
      // now, left/right locations are neither empty
      LocationSet commonLoc = getCommonLocationSet(leftLoc, rightLoc);
      switch (op) {
        case EQUALS:
          if (commonLoc.isBot()) {
            return null;
          }
          leftLoc = commonLoc;
          rightLoc = commonLoc;
          break;
        case NOT_EQUALS:
          if (leftLoc instanceof ExplicitLocationSet &&
              ((ExplicitLocationSet) leftLoc).getSize() == 1) {
            MemoryLocation leftOnlyLoc = Iterables.getOnlyElement(((ExplicitLocationSet) leftLoc)
                .getExplicitSet());
            rightLoc = rightLoc.removeElement(leftOnlyLoc);
          } else if (rightLoc instanceof ExplicitLocationSet &&
              ((ExplicitLocationSet) rightLoc).getSize() == 1) {
            MemoryLocation rightOnlyLoc = Iterables.getOnlyElement(((ExplicitLocationSet)
                rightLoc).getExplicitSet());
            leftLoc = leftLoc.removeElement(rightOnlyLoc);
          }
          if (leftLoc.isBot() || rightLoc.isBot()) {
            return null;
          }
          break;
        default:
          return newState;
      }
      // refine the pointer state
      LocationSet leftOrigLoc = asLocations(newState, pOtherStates, op1, 0, machineModel);
      LocationSet rightOrigLoc = asLocations(newState, pOtherStates, op2, 0, machineModel);
      if (leftOrigLoc instanceof ExplicitLocationSet &&
          ((ExplicitLocationSet) leftOrigLoc).getSize() == 1) {
        MemoryLocation onlyLoc = Iterables.getOnlyElement(((ExplicitLocationSet) leftOrigLoc)
            .getExplicitSet());
        newState = newState.setPointsToInformation(onlyLoc, leftLoc);
      }
      if (rightOrigLoc instanceof ExplicitLocationSet &&
          ((ExplicitLocationSet) rightOrigLoc).getSize() == 1) {
        MemoryLocation onlyLoc = Iterables.getOnlyElement(((ExplicitLocationSet) rightOrigLoc)
            .getExplicitSet());
        newState = newState.setPointsToInformation(onlyLoc, rightLoc);
      }
    }
    return newState;
  }

  private Pair<CExpression, Boolean> simplifyAssumption(CExpression pAssumption, boolean pTruth) {
    if (isBooleanExpression(pAssumption)) {
      CBinaryExpression binExp = (CBinaryExpression) pAssumption;
      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();
      BinaryOperator op = binExp.getOperator();
      if (isBooleanExpression(op1) && op2 instanceof CIntegerLiteralExpression && (
          (CIntegerLiteralExpression) op2).getValue().equals(BigInteger.ZERO)) {
        if (op == BinaryOperator.EQUALS) {
          return simplifyAssumption(op1, !pTruth);
        } else if (op == BinaryOperator.NOT_EQUALS) {
          return simplifyAssumption(op1, pTruth);
        }
      } else if (isBooleanExpression(op2) && op1 instanceof CIntegerLiteralExpression && (
          (CIntegerLiteralExpression) op1).getValue().equals(BigInteger.ZERO)) {
        if (op == BinaryOperator.EQUALS) {
          return simplifyAssumption(op2, !pTruth);
        } else if (op == BinaryOperator.NOT_EQUALS) {
          return simplifyAssumption(op2, pTruth);
        }
      }
    }
    return Pair.of(pAssumption, pTruth);
  }

  private boolean isBooleanExpression(CExpression exp) {
    return (exp instanceof CBinaryExpression) && ((CBinaryExpression) exp).getOperator()
        .isLogicalOperator();
  }

  private LocationSet getCommonLocationSet(LocationSet ls1, LocationSet ls2) {
    if (ls1.isBot() || ls2.isBot()) {
      return LocationSetBot.INSTANCE;
    }
    if (ls1.isTop()) {
      return ls2;
    }
    if (ls2.isTop()) {
      return ls1;
    }
    Set<MemoryLocation> set1 = ((ExplicitLocationSet) ls1).getExplicitSet();
    Set<MemoryLocation> set2 = ((ExplicitLocationSet) ls2).getExplicitSet();
    return ExplicitLocationSet.from(Sets.intersection(set1, set2));
  }

  private Pointer2State handleStatementEdge(
      Pointer2State pState, List<AbstractState>
      pOtherStates, CStatementEdge pEdge) throws UnrecognizedCCodeException {
    CStatement statement = pEdge.getStatement();
    if (statement instanceof CAssignment) {
      CAssignment assignment = (CAssignment) statement;
      return handleAssignment(pState, pOtherStates, assignment.getLeftHandSide(), assignment
          .getRightHandSide());
    }
    return pState;
  }

  private Pointer2State handleAssignment(
      Pointer2State pState, List<AbstractState> pOtherStates,
      CLeftHandSide pLHS, CRightHandSide pRHS)
      throws UnrecognizedCCodeException {
    LocationSet lhsLoc = asLocations(pState, pOtherStates, pLHS, 0, machineModel);
    return handleAssignment(pState, pOtherStates, lhsLoc, pRHS);
  }

  private Pointer2State handleAssignment(
      Pointer2State pState, LocationSet pLocationSet, LocationSet targetLocSet) {
    final Iterable<MemoryLocation> locations;
    if (pLocationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (pLocationSet instanceof ExplicitLocationSet) {
      locations = (ExplicitLocationSet) pLocationSet;
    } else {
      locations = Collections.emptySet();
    }
    Pointer2State newState = pState;
    for (MemoryLocation location : locations) {
      newState = newState.setPointsToInformation(location, targetLocSet);
    }
    return newState;
  }

  private Pointer2State handleAssignment(
      Pointer2State pState, List<AbstractState> pOtherStates,
      LocationSet pLocationSet, CRightHandSide pRHS)
      throws UnrecognizedCCodeException {
    final Set<MemoryLocation> locations;
    boolean isTainted = false;
    if (pLocationSet.isTop()) {
      locations = pState.getKnownLocations();
    } else if (pLocationSet instanceof ExplicitLocationSet) {
      locations = ((ExplicitLocationSet) pLocationSet).getExplicitSet();
      isTainted = ((ExplicitLocationSet) pLocationSet).isTainted();
    } else {
      locations = Collections.emptySet();
    }
    LocationSet rhsLoc = asLocations(pState, pOtherStates, pRHS, 1, machineModel);
    Pointer2State newState = pState;
    // strong update is performed if `locations` contains only one location and it is not tainted
    if (!isTainted && locations.size() == 1) {
      MemoryLocation leftLocation = Iterables.getOnlyElement(locations);
      newState = newState.setPointsToInformation(leftLocation, rhsLoc);
    } else {
      for (MemoryLocation location : locations) {
        newState = newState.addPointsToInformation(location, rhsLoc);
      }
    }
    return newState;
  }

  private Pointer2State handleAssignment(
      Pointer2State pState, List<AbstractState> pOtherStates,
      MemoryLocation baseLocation, CRightHandSide exp)
      throws UnrecognizedCCodeException {
    return pState.setPointsToInformation(baseLocation, asLocations(pState, pOtherStates, exp, 1,
     machineModel));
  }

  /* **************** */
  /* location utility */
  /* **************** */

  private MemoryLocation toLocation(CSimpleDeclaration pDeclaration) {
    return MemoryLocation.valueOf(pDeclaration.getQualifiedName());
  }

  /**
   * Evaluate the given right-hand-side as the set of memory locations.
   * 1. when the level equals 0, then only left-hand-side expressions have location set;
   * 2. when the level is greater than 0, non-lhs could also have location set, such as function
   * call expression `malloc(size)`.
   */
  static LocationSet asLocations(
      final Pointer2State pState, final List<AbstractState>
      pOtherStates, final CRightHandSide pRightHandSide, final int pLevel, MachineModel pModel)
      throws UnrecognizedCCodeException {
    if (refLevelUnmatched(pRightHandSide.getExpressionType(), pLevel)) {
      return LocationSetBot.INSTANCE;
    }
    LocationSetVisitor visitor = new LocationSetVisitor(pState, pOtherStates, pLevel, pModel);
    LocationSet locations = pRightHandSide.accept(visitor);
    int newLevel = visitor.refLevel;
    // Consider the assignment x = e where e is of array type, then it is equivalent with x =
    // &e[0]. Since the memory location of e[0] is the location of e, we should decrement
    // reference level by 1.
    if (Types.extractArrayType(pRightHandSide.getExpressionType()) != null && newLevel > 0) {
      newLevel--;
    }
    return unwrapReference(pState, locations, newLevel);
  }

  private static boolean refLevelUnmatched(CType pType, int pLevel) {
    CType currentType = pType;
    while (currentType instanceof CTypedefType) {
      currentType = ((CTypedefType) currentType).getRealType();
    }
    boolean broken = false;
    while (pLevel > 0) {
      CType wrappedType = Types.unwrapType(currentType);
      if (wrappedType == null) {
        broken = true;
        break;
      }
      currentType = wrappedType;
      --pLevel;
    }
    return broken && (!(currentType instanceof CFunctionType) || !(pLevel == 1));
  }

  private static LocationSet unwrapReference(Pointer2State pState, LocationSet pLocationSet, int
      pLevel) {
    if (pLocationSet.isTop() || pLocationSet.isBot()) {
      return pLocationSet;
    }
    if (!(pLocationSet instanceof ExplicitLocationSet)) {
      return LocationSetTop.INSTANCE;
    }
    Collection<MemoryLocation> result = new HashSet<>();
    Iterables.addAll(result, (ExplicitLocationSet) pLocationSet);
    for (int level = pLevel; level > 0 && !result.isEmpty(); --level) {
      Collection<MemoryLocation> newResult = new HashSet<>();
      for (MemoryLocation location : result) {
        LocationSet targets = pState.getPointsToSet(location);
        if (targets.isTop()) {
          return targets;
        }
        if (targets.isBot()) {
          continue;
        }
        if (!(targets instanceof ExplicitLocationSet)) {
          return LocationSetTop.INSTANCE;
        }
        Iterables.addAll(newResult, (ExplicitLocationSet) targets);
      }
      result = newResult;
    }
    return ExplicitLocationSet.from(result);
  }

  /**
   * Evaluate side-effect-free expression using information from value analyses.
   *
   * @param otherStates other state components
   * @param exp         expression to be evaluated
   * @param model       machine model specified for analysis
   * @return evaluation result
   */
  @Nullable
  private static Long evaluateExpression(List<AbstractState> otherStates, CExpression exp,
                                      MachineModel model)
      throws UnrecognizedCCodeException {
    if (exp instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) exp).getValue().longValue();
    }
    RangeState rangeState = AbstractStates.extractStateByType(otherStates, RangeState.class);
    if (rangeState != null) {
      Range range = RangeState.evaluateRange(rangeState, otherStates, exp, model);
      if (range.isSingletonRange()) {
        CompInteger value = range.getLow();
        if (value.getStatus() == IntegerStatus.NORM && !value.isFloating()) {
          return value.longValue();
        }
      }
    }
    // TODO: support other value analyses
    return null;
  }


  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }

  /* *********************** */
  /* memory location visitor */
  /* *********************** */

  private static class LocationSetVisitor implements CRightHandSideVisitor<LocationSet,
      UnrecognizedCCodeException> {

    private final Pointer2State state;
    private final List<AbstractState> otherStates;
    private int refLevel;
    private final MachineModel model;

    LocationSetVisitor(Pointer2State pState, List<AbstractState> pOtherStates, int pLevel,
                       MachineModel pModel) {
      state = pState;
      otherStates = pOtherStates;
      refLevel = pLevel;
      model = pModel;
    }

    /* **************** */
    /* override methods */
    /* **************** */

    @Override
    public LocationSet visit(CFunctionCallExpression e) throws UnrecognizedCCodeException {
      if (refLevel == 0) {
        return LocationSetBot.INSTANCE;
      } else {
        LocationSet result = Pointer2FunctionAdapter.instance().evaluateFunctionCallExpression(e,
            state, otherStates, null);
        refLevel--;
        return result;
      }
    }

    @Override
    public LocationSet visit(CBinaryExpression e) throws UnrecognizedCCodeException {
      if (refLevel == 0) {
        return LocationSetBot.INSTANCE;
      } else {
        refLevel--;
        CExpression op1 = e.getOperand1();
        CExpression op2 = e.getOperand2();
        BinaryOperator op = e.getOperator();
        CType type1 = op1.getExpressionType();
        CType type2 = op2.getExpressionType();
        CPointerType ptrType1 = Types.extractPointerType(type1);
        CPointerType ptrType2 = Types.extractPointerType(type2);
        CArrayType arrayType1 = Types.extractArrayType(type1);
        CArrayType arrayType2 = Types.extractArrayType(type2);
        if (ptrType1 != null) {
          return handlePointerArithmetic(op1, ptrType1, op, op2, model);
        } else if (arrayType1 != null) {
          return handleArrayArithmetic(op1, arrayType1, op, op2, model);
        } else if (ptrType2 != null && op == BinaryOperator.PLUS) {
          return handlePointerArithmetic(op2, ptrType2, op1, true, model);
        } else if (arrayType2 != null && op == BinaryOperator.PLUS) {
          return handleArrayArithmetic(op2, arrayType2, op1, true, model);
        }
        return LocationSetBot.INSTANCE;
      }
    }

    @Override
    public LocationSet visit(CCastExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    @Override
    public LocationSet visit(CCharLiteralExpression e) throws UnrecognizedCCodeException {
      return LocationSetBot.INSTANCE;
    }

    @Override
    public LocationSet visit(CFloatLiteralExpression e) throws UnrecognizedCCodeException {
      return LocationSetBot.INSTANCE;
    }

    @Override
    public LocationSet visit(CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
      if (refLevel > 0 && e.getValue().signum() == 0) {
        // NULL object
        refLevel--;
        return ExplicitLocationSet.from(MemoryLocation.NULL_OBJECT);
      }
      return LocationSetBot.INSTANCE;
    }

    @Override
    public LocationSet visit(CStringLiteralExpression e) throws UnrecognizedCCodeException {
      if (refLevel == 0) {
        return LocationSetBot.INSTANCE;
      } else {
        refLevel--;
        String literalName = String.format(PointerVisitor.LITERAL_TEMPLATE, e.getValue());
        MemoryLocation loc = MemoryLocation.valueOf(literalName);
        return ExplicitLocationSet.from(loc);
      }
    }

    @Override
    public LocationSet visit(CTypeIdExpression e) throws UnrecognizedCCodeException {
      return LocationSetBot.INSTANCE;
    }

    @Override
    public LocationSet visit(CUnaryExpression e) throws UnrecognizedCCodeException {
      if (refLevel == 0) {
        return LocationSetBot.INSTANCE;
      } else {
        UnaryOperator op = e.getOperator();
        if (op == UnaryOperator.AMPER) {
          refLevel--;
          return e.getOperand().accept(this);
        }
        // other cases are generally impossible
        return LocationSetBot.INSTANCE;
      }
    }

    @Override
    public LocationSet visit(CImaginaryLiteralExpression e) throws UnrecognizedCCodeException {
      return LocationSetBot.INSTANCE;
    }

    @Override
    public LocationSet visit(CAddressOfLabelExpression e) throws UnrecognizedCCodeException {
      return LocationSetBot.INSTANCE;
    }

    @Override
    public LocationSet visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
      CExpression array = e.getArrayExpression();
      CExpression subscript = e.getSubscriptExpression();
      CArrayType arrayType = Types.extractArrayType(array.getExpressionType());
      CPointerType pointerType = Types.extractPointerType(array.getExpressionType());
      if (arrayType != null) {
        return handleArrayArithmetic(array, arrayType, subscript, true, model);
      } else {
        assert (pointerType != null);
        return handlePointerArithmetic(array, pointerType, subscript, true, model);
      }
    }

    @Override
    public LocationSet visit(CFieldReference e) throws UnrecognizedCCodeException {
      CExpression owner = e.getFieldOwner();
      String name = e.getFieldName();
      LocationSet ownerLoc;
      CType ownerType = owner.getExpressionType();
      if (e.isPointerDereference()) {
        ownerLoc = asLocations(state, otherStates, owner, 1, model);
        ownerType = Types.dereferenceType(ownerType);
      } else {
        ownerLoc = asLocations(state, otherStates, owner, 0, model);
      }
      if (ownerLoc.isTop() || ownerLoc.isBot()) {
        return ownerLoc;
      }
      if (!(ownerLoc instanceof ExplicitLocationSet)) {
        return LocationSetTop.INSTANCE;
      }
      Pair<Long, CType> fieldInfo = Types.getFieldInfo(ownerType, name, model);
      Long offset = fieldInfo.getFirst();
      if (offset == null) {
        return LocationSetTop.INSTANCE;
      } else if (offset == 0) {
        return ownerLoc;
      } else {
        Set<MemoryLocation> result = new HashSet<>();
        for (MemoryLocation location : (ExplicitLocationSet) ownerLoc) {
          result.add(MemoryLocation.withOffset(location, offset));
        }
        return ExplicitLocationSet.from(result);
      }
    }

    @Override
    public LocationSet visit(CIdExpression e) throws UnrecognizedCCodeException {
      CSimpleDeclaration declaration = e.getDeclaration();
      MemoryLocation location;
      if (declaration != null) {
        location = MemoryLocation.valueOf(declaration.getQualifiedName());
      } else {
        location = MemoryLocation.valueOf(e.getName());
      }
      return ExplicitLocationSet.from(location);
    }

    @Override
    public LocationSet visit(CPointerExpression e) throws UnrecognizedCCodeException {
      refLevel++;
      return e.getOperand().accept(this);
    }

    @Override
    public LocationSet visit(CComplexCastExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    /* *************** */
    /* utility methods */
    /* *************** */

    private LocationSet handleArrayArithmetic(
        CExpression pArray,
        CArrayType pType,
        CExpression pIndex,
        boolean pIsAdd,
        MachineModel pModel)
        throws UnrecognizedCCodeException {
      Long index = evaluateExpression(otherStates, pIndex, pModel);
      if (index != null) {
        CType elementType = pType.getType();
        int elementSize = pModel.getSizeof(elementType);
        long delta = index * elementSize;
        if (!pIsAdd) {
          delta = -delta;
        }
        LocationSet arrayLoc = asLocations(state, otherStates, pArray, 0, pModel);
        if (arrayLoc.isTop() || arrayLoc.isBot()) {
          return arrayLoc;
        }
        if (!(arrayLoc instanceof ExplicitLocationSet)) {
          return LocationSetTop.INSTANCE;
        }
        Set<MemoryLocation> result = new HashSet<>();
        if (delta == 0) {
          return arrayLoc;
        } else {
          for (MemoryLocation location : (ExplicitLocationSet) arrayLoc) {
            result.add(MemoryLocation.withOffset(location, delta));
          }
        }
        return ExplicitLocationSet.from(result);
      } else {
        return LocationSetTop.INSTANCE;
      }
    }

    private LocationSet handlePointerArithmetic(
        CExpression pBasePtr,
        CPointerType pType,
        CExpression pIndex,
        boolean pIsAdd,
        MachineModel pModel)
        throws UnrecognizedCCodeException {
      Long index = evaluateExpression(otherStates, pIndex, pModel);
      if (index != null) {
        CType starredType = pType.getType();
        int elementSize = pModel.getSizeof(starredType);
        long delta = elementSize * index;
        if (!pIsAdd) {
          delta = -delta;
        }
        LocationSet pointerLoc = asLocations(state, otherStates, pBasePtr, 1, pModel);
        if (pointerLoc.isTop() || pointerLoc.isBot()) {
          return pointerLoc;
        }
        if (!(pointerLoc instanceof ExplicitLocationSet)) {
          return LocationSetTop.INSTANCE;
        }
        Set<MemoryLocation> result = new HashSet<>();
        if (delta == 0) {
          return pointerLoc;
        } else {
          for (MemoryLocation location : (ExplicitLocationSet) pointerLoc) {
            result.add(MemoryLocation.withOffset(location, delta));
          }
        }
        return ExplicitLocationSet.from(result);
      } else {
        return LocationSetTop.INSTANCE;
      }
    }

    private LocationSet handleArrayArithmetic(
        CExpression pArray, CArrayType pType,
        BinaryOperator pOp, CExpression pIndex, MachineModel pModel)
        throws UnrecognizedCCodeException {
      switch (pOp) {
        case PLUS:
          return handleArrayArithmetic(pArray, pType, pIndex, true, pModel);
        case MINUS:
          return handleArrayArithmetic(pArray, pType, pIndex, false, pModel);
        default:
          return LocationSetBot.INSTANCE;
      }
    }

    private LocationSet handlePointerArithmetic(
        CExpression pBasePtr, CPointerType pType,
        BinaryOperator pOp, CExpression pIndex, MachineModel pModel)
        throws UnrecognizedCCodeException {
      switch (pOp) {
        case PLUS:
          return handlePointerArithmetic(pBasePtr, pType, pIndex, true, pModel);
        case MINUS:
          return handlePointerArithmetic(pBasePtr, pType, pIndex, false, pModel);
        default:
          return LocationSetBot.INSTANCE;
      }
    }

  }


}
