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
package org.sosy_lab.cpachecker.cpa.range.checker;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFixInfo;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerTypeConstraint.IntegerTypePredicate;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.AssignmentCell;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerWithInstantErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorSpot;
import org.sosy_lab.cpachecker.core.interfaces.checker.ExpressionCell;
import org.sosy_lab.cpachecker.core.interfaces.checker.ExpressionChecker;
import org.sosy_lab.cpachecker.core.interfaces.checker.PL;
import org.sosy_lab.cpachecker.core.phase.fix.util.CastFixMetaInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

@Options(prefix = "checker.range.conversion")
public class IntegerConversionChecker implements ExpressionChecker<RangeState, Range>,
                                                 CheckerWithInstantErrorReport {

  @Option(secure = true, name = "refine", description = "whether the given state will be refined")
  private boolean refine = true;

  @Option(secure = true, name = "truncateAsError", description = "whether we treat a truncating "
      + "type cast as error")
  private boolean treatTruncatingCastAsError = false;

  private final MachineModel machineModel;
  private final List<ErrorReport> errorStore;

  public IntegerConversionChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    errorStore = new ArrayList<>();
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    if (cfaInfo.isPresent()) {
      machineModel = cfaInfo.get().getCFA().getMachineModel();
    } else {
      throw new InvalidConfigurationException("A valid CFA is required to determine machine model");
    }
  }

  @Override
  public Collection<ErrorReport> getErrorReport() {
    return errorStore;
  }

  @Override
  public void resetErrorReport() {
    errorStore.clear();
  }

  @Override
  public List<ARGState> getInverseCriticalStates(ARGPath path, ErrorSpot nodeInError) {
    ARGState lastState = path.getLastState();
    return Collections.singletonList(lastState);
  }

  @Override
  public ExpressionCell<RangeState, Range> checkAndRefine(
      CRightHandSide rightHand, ExpressionCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws CPATransferException {
    // we only need to check the function call expression
    if (rightHand instanceof CFunctionCallExpression) {
      if (cell.getNumOfOperands() != ((CFunctionCallExpression) rightHand).getParameterExpressions
          ().size()) {
        throw new UnsupportedOperationException("Function call requires sufficient argument "
            + "values");
      }
      return handleFunctionCallExpression((CFunctionCallExpression) rightHand, cell, cfaEdge);
    } else if (rightHand instanceof CBinaryExpression) {
      if (cell.getNumOfOperands() != 2) {
        throw new UnsupportedOperationException("Binary expression requires two abstract domain "
            + "elements");
      }
      return handleBinaryExpression((CBinaryExpression) rightHand, cell, cfaEdge);
    } else if (rightHand instanceof CCastExpression && treatTruncatingCastAsError) {
      if (cell.getNumOfOperands() != 1) {
        throw new UnsupportedOperationException("Cast expression requires one abstract domain "
            + "values");
      }
      return handleCastExpression((CCastExpression) rightHand, cell, cfaEdge);
    } else if (rightHand instanceof CArraySubscriptExpression) {
      if (cell.getNumOfOperands() != 1) {
        throw new UnsupportedOperationException("Array subscript requires one abstract domain "
            + "value");
      }
      return handleArraySubscriptExpression((CArraySubscriptExpression) rightHand, cell);
    } else {
      return cell;
    }
  }

  private ExpressionCell<RangeState, Range> handleFunctionCallExpression
      (
          CFunctionCallExpression pCFunctionCallExpression, ExpressionCell<RangeState, Range> cell,
          @Nullable CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {
    CExpression functionName = pCFunctionCallExpression.getFunctionNameExpression();
    List<CExpression> arguments = pCFunctionCallExpression.getParameterExpressions();
    CType functionType = functionName.getExpressionType();
    List<Range> newOperands = new ArrayList<>(arguments.size());
    RangeState newState = cell.getState();
    if (functionType instanceof CFunctionType) {
      List<CType> parameters = ((CFunctionType) functionType).getParameters();
      if (parameters.size() == arguments.size()) {
        // functions taking variant arguments are not considered
        for (int i = 0; i < parameters.size(); i++) {
          CType parameter = parameters.get(i);
          Range argument = cell.getOperand(i);
          // consider the type range of argument expression
          Range restriction = Ranges.getTypeRange(parameter, machineModel);
          if (!restriction.contains(argument)) {
            // conversion error occurs
            CExpression arg = arguments.get(i);
            IntegerConversionErrorReport error = new IntegerConversionErrorReport(arg, cfaEdge,
                this);
            errorStore.add(error);
            // also, we introduce sanity check fix to protect function arguments
            IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
            if (info != null) {
              info.addCandidateFix(arg.getFileLocation(), IntegerFixMode.CHECK_CONV,
                  Types.toSimpleType(parameter));
            }

            if (refine) {
              argument = argument.intersect(restriction);
              newState = arguments.get(i).accept(new RangeRefineVisitor(cell.getState(),
                  cell.getOtherStates(), argument, machineModel, false));
            }
          }
          newOperands.add(argument);
        }
        return new ExpressionCell<>(newState, cell.getOtherStates(), newOperands, cell.getResult());
      }
    }
    // no adjustment here
    return cell;
  }

  private ExpressionCell<RangeState, Range> handleBinaryExpression(
      CBinaryExpression e, ExpressionCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {
    BinaryOperator operator = e.getOperator();
    Range r1 = cell.getOperand(0);
    Range r2 = cell.getOperand(1);
    RangeState newState = cell.getState();
    if (!operator.isLogicalOperator()) {
      // for bit-shift operation, we should ensure that both operands are non-negative
      if (operator == BinaryOperator.SHIFT_LEFT || operator == BinaryOperator.SHIFT_RIGHT) {
        IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
        if (!Range.NONNEGATIVE.contains(r1)) {
          CExpression op = e.getOperand1();
          IntegerConversionErrorReport error = new IntegerConversionErrorReport(op, cfaEdge,
              this);
          errorStore.add(error);
          if (info != null) {
            info.addCandidateFix(op.getFileLocation(), IntegerFixMode.CHECK_CONV,
                CNumericTypes.UNSIGNED_LONG_LONG_INT);
          }
          if (refine) {
            r1 = r1.intersect(Range.NONNEGATIVE);
            newState = op.accept(new RangeRefineVisitor(newState, cell.getOtherStates(), Range
                .NONNEGATIVE, machineModel, false));
          }
        }
        if (!Range.NONNEGATIVE.contains(r2)) {
          CExpression op = e.getOperand2();
          IntegerConversionErrorReport error = new IntegerConversionErrorReport(op, cfaEdge,
              this);
          errorStore.add(error);
          if (info != null) {
            info.addCandidateFix(op.getFileLocation(), IntegerFixMode.CHECK_CONV,
                CNumericTypes.UNSIGNED_LONG_LONG_INT);
          }
          if (refine) {
            r2 = r2.intersect(Range.NONNEGATIVE);
            newState = op.accept(new RangeRefineVisitor(newState, cell.getOtherStates(), Range
                .NONNEGATIVE, machineModel, false));
          }
        }
        return new ExpressionCell<>(newState, cell.getOtherStates(), Lists.newArrayList(r1, r2),
            cell.getResult());
      }
      return cell;
    } else {
      // the binary operation is logical, not arithmetic
      CSimpleType t1 = Types.toIntegerType(e.getOperand1().getExpressionType());
      CSimpleType t2 = Types.toIntegerType(e.getOperand2().getExpressionType());
      if (t1 != null && t2 != null) {
        CSimpleType mergedType = CBinaryExpressionBuilder.getCommonSimpleTypeForBinaryOperation
            (machineModel, t1, t2);
        Range mergedTr = Ranges.getTypeRange(mergedType, machineModel);
        boolean outOfBound1 = !mergedTr.contains(r1);
        boolean outOfBound2 = !mergedTr.contains(r2);
        if (outOfBound1 || outOfBound2) {
          // conversion error occurs in logical operation, usually in branching condition
          IntegerConversionErrorReport error = new IntegerConversionErrorReport(e, cfaEdge, this);
          errorStore.add(error);

          // we can explicitly cast two operands to a more wider type to prevent unexpected cast
          IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
          if (info != null) {
            Range union = r1.union(r2);
            CompInteger lower = union.getLow();
            CompInteger upper = union.getHigh();
            if (lower.getStatus() == IntegerStatus.NORM
                && upper.getStatus() == IntegerStatus.NORM) {
              CSimpleType newType = CTypes.getTypeFromRange(machineModel, lower.getValue(), upper
                  .getValue());
              if (newType != null) {
                // we first perform integer promotion on the operand type
                newType = machineModel.getPromotedCType(newType);
                CExpression op1 = e.getOperand1();
                CExpression op2 = e.getOperand2();
                AccessPath path1, path2;
                if (op1 instanceof CLeftHandSide) {
                  path1 = RangeState.getAccessPath(newState, cell.getOtherStates(),
                      (CLeftHandSide) op1, machineModel);
                  if (path1 != null) {
                    info.addTypeConstraint(IntegerTypePredicate.COVER, path1, newType);
                  }
                }
                if (op2 instanceof CLeftHandSide) {
                  path2 = RangeState.getAccessPath(newState, cell.getOtherStates(),
                      (CLeftHandSide) op2, machineModel);
                  if (path2 != null) {
                    info.addTypeConstraint(IntegerTypePredicate.COVER, path2, newType);
                  }
                }
                if (outOfBound1) {
                  info.addCandidateFix(op1.getFileLocation(), newType, CastFixMetaInfo.convertOf
                      (op1, mergedType));
                }
                if (outOfBound2) {
                  info.addCandidateFix(op2.getFileLocation(), newType, CastFixMetaInfo.convertOf
                      (op2, mergedType));
                }
              } else {
                // operands should be sanitized if necessary
                CExpression op1 = e.getOperand1();
                CExpression op2 = e.getOperand2();
                info.addCandidateFix(op1.getFileLocation(), IntegerFixMode.CHECK_CONV, mergedType);
                info.addCandidateFix(op2.getFileLocation(), IntegerFixMode.CHECK_CONV, mergedType);
              }
            }
          }

          if (refine) {
            Range commonRange = Ranges.getTypeRange(mergedType, machineModel);
            Range nr1 = r1.intersect(commonRange);
            Range nr2 = r2.intersect(commonRange);

            if (!nr1.equals(r1)) {
              newState = e.getOperand1().accept(new RangeRefineVisitor(newState, cell
                  .getOtherStates(), nr1, machineModel, false));
            }
            if (!nr2.equals(r2)) {
              newState = e.getOperand2().accept(new RangeRefineVisitor(newState, cell
                  .getOtherStates(), nr2, machineModel, false));
            }
            return new ExpressionCell<>(newState, cell.getOtherStates(), Lists.newArrayList(nr1,
                nr2), cell.getResult());
          }
        }
      }
      return cell;
    }
  }

  private ExpressionCell<RangeState, Range> handleArraySubscriptExpression(
      CArraySubscriptExpression e, ExpressionCell<RangeState, Range> cell)
      throws UnrecognizedCCodeException {
    IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
    if (info != null) {
      CExpression indexExp = e.getSubscriptExpression();
      Range indexRange = cell.getOperand(0);
      CType indexType = indexExp.getExpressionType();
      Range typeRange = Ranges.getTypeRange(indexType, machineModel);
      if (!typeRange.contains(indexRange)) {
        // possible overflow occurs in array subscript, we attempt to add sanitization here
        CompInteger lower = indexRange.getLow();
        CompInteger upper = indexRange.getHigh();
        if (lower.getStatus() == IntegerStatus.NORM && upper.getStatus() == IntegerStatus.NORM) {
          CSimpleType newType = CTypes.getTypeFromRange(machineModel, lower.getValue(), upper
              .getValue());
          if (newType == null) {
            // DOUBLE type denotes "type accepting values of infinite length"
            newType = CNumericTypes.DOUBLE;
          }
          info.addCandidateFix(indexExp.getFileLocation(), IntegerFixMode.CHECK_CONV, newType);
        }
      }
    }
    return cell;
  }

  private ExpressionCell<RangeState, Range> handleCastExpression(
      CCastExpression e, ExpressionCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {
    CType castType = e.getCastType();
    Range r = cell.getOperand(0);
    RangeState newState = cell.getState();
    Range castRange = Ranges.getTypeRange(castType, machineModel);
    if (!castRange.contains(r)) {
      IntegerConversionErrorReport error = new IntegerConversionErrorReport(e, cfaEdge, this);
      errorStore.add(error);

      IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
      if (info != null) {
        CompInteger lower = r.getLow();
        CompInteger upper = r.getHigh();
        if (lower.getStatus() == IntegerStatus.NORM && upper.getStatus() == IntegerStatus.NORM) {
          CSimpleType newType = CTypes.getTypeFromRange(machineModel, lower.getValue(), upper
              .getValue());
          if (newType != null) {
            newType = machineModel.getPromotedCType(newType);
            CExpression op = e.getOperand();
            AccessPath path;
            if (op instanceof CLeftHandSide) {
              path = RangeState.getAccessPath(newState, cell.getOtherStates(), (CLeftHandSide) op,
                  machineModel);
              if (path != null) {
                info.addTypeConstraint(IntegerTypePredicate.COVER, path, newType);
              }
            }
            info.addCandidateFix(op.getFileLocation(), newType, CastFixMetaInfo.convertOf(op,
                castType));
          }
        }
      }

      if (refine) {
        newState = e.getOperand().accept(new RangeRefineVisitor(newState, cell.getOtherStates(),
            castRange, machineModel, false));
        return new ExpressionCell<>(newState, cell.getOtherStates(), Lists.newArrayList(castRange),
            cell.getResult());
      }
    }

    return cell;
  }

  @Override
  public AssignmentCell<RangeState, Range> checkAndRefine(
      CAssignment assignment, AssignmentCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws CPATransferException {
    // for assignment, we need to check if the range of right-hand expression is in the range of
    // the type of the left-hand expression
    CLeftHandSide leftHandSide = assignment.getLeftHandSide();
    CRightHandSide rightHandSide = assignment.getRightHandSide();
    CType leftType = assignment.getLeftHandSide().getExpressionType();
    Range leftRange = Ranges.getTypeRange(leftType, machineModel);
    if (!leftRange.isUnbound()) {
      // left operand has numerical type
      Range rightRange = cell.getRightValue();
      if (!leftRange.contains(rightRange)) {
        // If the condition holds, then the left range is bounded, which means the left type is
        // an integer type.
        // conversion error occurs
        IntegerConversionErrorReport error = new IntegerConversionErrorReport(rightHandSide,
            cfaEdge, this);
        errorStore.add(error);
        // tolerable flag indicates that whether current range state should be refined
        // for example, the statement x = e where x is global has conversion error, then the
        // range of e should be refined because type constraint around x is hard
        boolean tolerable = true;

        IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
        if (info != null) {
          // add type constraint for assignment such that the actual type of left-hand-side
          // should be large enough to cover all the possible values of the right-hand-side
          AccessPath leftPath = RangeState.getAccessPath(cell.getState(), cell.getOtherStates(),
              leftHandSide, machineModel);
          if (leftPath != null) {
            if (leftPath.isDeclarationPath()) {
              // if the left-hand-side is a global variable or a function parameter, then we add
              // sanitizing routine on the right-hand-side
              if (leftPath.isGlobal() || leftPath.isParameter() || leftPath.isReturnValue()) {
                info.addCandidateFix(rightHandSide.getFileLocation(), IntegerFixMode
                    .CHECK_CONV, Types.toSimpleType(leftType));
                tolerable = false;
              } else {
                CompInteger lower = rightRange.getLow();
                CompInteger upper = rightRange.getHigh();
                if (lower.getStatus() == IntegerStatus.NORM
                    && upper.getStatus() == IntegerStatus.NORM) {
                  CSimpleType rightType = CTypes.getTypeFromRange(machineModel, lower.getValue(),
                      upper.getValue());
                  if (rightType != null) {
                    info.addTypeConstraint(IntegerTypePredicate.COVER, leftPath, rightType);
                  }
                }
              }
              info.addLeftName(leftPath);
            } else {
              // Otherwise, the access path of left-hand-side contains structure-specific segment.
              // We should add sanitizing routine enclosed the right-hand-side.
              info.addCandidateFix(rightHandSide.getFileLocation(), IntegerFixMode.CHECK_CONV,
                  Types.toSimpleType(leftType));
            }
          }
        }

        if (refine && !tolerable) {
          if (rightHandSide instanceof CExpression) {
            rightRange = rightRange.intersect(leftRange);
            CExpression rightExpression = (CExpression) rightHandSide;
            RangeState newState = rightExpression.accept(new RangeRefineVisitor(cell.getState(),
                cell.getOtherStates(), rightRange, machineModel, false));
            return new AssignmentCell<>(newState, cell.getOtherStates(), rightRange);
          }
        }
      }
    }
    // no adjustment here
    return cell;
  }

  @Override
  public Class<Range> getAbstractDomainClass() {
    return Range.class;
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.INTEGER_CONVERSION;
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return RangeState.class;
  }
}
