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

import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus;
import org.sosy_lab.cpachecker.cpa.range.ExpressionRangeVisitor;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

@Options(prefix = "checker.range.overflow")
public class IntegerOverflowChecker implements ExpressionChecker<RangeState, Range>,
                                               CheckerWithInstantErrorReport {

  @Option(secure = true, name = "refine", description = "whether the given state will be refined")
  private boolean refine = true;

  @Option(secure = true, description = "if this flag is on, overflow errors on function argument "
      + "would cause a strong sanitization which prevents the further analysis")
  private boolean sanitizeFunctionArg = false;

  private final MachineModel machineModel;
  private final List<ErrorReport> errorStore;

  public IntegerOverflowChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    errorStore = new ArrayList<>();
    // we register range state later lazily
    if (GlobalInfo.getInstance().getCFAInfo().isPresent()) {
      machineModel = GlobalInfo.getInstance().getCFAInfo().get().getCFA().getMachineModel();
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
    // TODO: now we use a default implementation
    ARGState lastState = path.getLastState();
    return Collections.singletonList(lastState);
  }

  @Override
  public ExpressionCell<RangeState, Range> checkAndRefine(
      CRightHandSide rightHand, ExpressionCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws CPATransferException {

    if (rightHand instanceof CBinaryExpression) {
      if (cell.getNumOfOperands() != 2) {
        throw new UnsupportedOperationException("Binary expression requires two abstract domain "
            + "elements");
      }
      return handleBinaryExpression((CBinaryExpression) rightHand, cell, Ranges.getTypeRange
          (rightHand, machineModel), cfaEdge);
    } else if (rightHand instanceof CFloatLiteralExpression) {
      return handleFloatLiteralExpression((CFloatLiteralExpression) rightHand, cell, Ranges
          .getTypeRange(rightHand, machineModel), cfaEdge);
    } else if (rightHand instanceof CIntegerLiteralExpression) {
      return handleIntegerLiteralExpression((CIntegerLiteralExpression) rightHand, cell, Ranges
          .getTypeRange(rightHand, machineModel), cfaEdge);
    } else if (rightHand instanceof CUnaryExpression) {
      if (cell.getNumOfOperands() != 1) {
        throw new UnsupportedOperationException("Unary expression requires one abstract domain "
            + "element");
      }
      return handleUnaryExpression((CUnaryExpression) rightHand, cell, Ranges.getTypeRange
          (rightHand, machineModel), cfaEdge);
    } else if (rightHand instanceof CFunctionCallExpression) {
      if (cell.getNumOfOperands() != ((CFunctionCallExpression) rightHand).getParameterExpressions
          ().size()) {
        throw new UnsupportedOperationException("Function call requires sufficient argument "
            + "values");
      }
      // this function is to refine value of function call by summary
      return handleFunctionCallExpression((CFunctionCallExpression) rightHand, cell);
    } else {
      // otherwise, nothing should be changed
      return cell;
    }

  }

  @Override
  public AssignmentCell<RangeState, Range> checkAndRefine(
      CAssignment assignment, AssignmentCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws CPATransferException {
    // in this checker, we change nothing when processing assignment
    return cell;
  }

  private ExpressionCell<RangeState, Range> handleBinaryExpression
      (
          CBinaryExpression e, ExpressionCell<RangeState, Range> cell, Range restriction, @Nullable
          CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {
    BinaryOperator operator = e.getOperator();
    Range r1 = cell.getOperand(0);
    Range r2 = cell.getOperand(1);
    if (operator.isLogicalOperator()) {
      return cell;
    } else {
      Range resultRange = Ranges.getArithmeticResult(operator, r1, r2, e, machineModel);
      if (restriction.contains(resultRange)) {
        return cell;
      } else {
        IntegerOverflowErrorReport error = new IntegerOverflowErrorReport(e, cfaEdge, this);
        errorStore.add(error);

        IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
        if (info != null) {
          CompInteger lower = resultRange.getLow();
          CompInteger upper = resultRange.getHigh();
          if (lower.getStatus() == IntegerStatus.NORM && upper.getStatus() == IntegerStatus.NORM) {
            CSimpleType newType = CTypes.getTypeFromRange(machineModel, lower.getValue(), upper
                .getValue());
            // for bit-shift operation, we should guarantee that the left operand is unsigned
            // at least, we eliminate the potential undefined behaviors
            if (newType == null && operator.isBitShiftOperator()) {
              newType = Types.toSimpleType(e.getOperand1().getExpressionType());
              if (newType != null) {
                newType = machineModel.getPromotedCType(newType);
                newType = Types.toUnsignedType(newType);
              }
            }
            if (newType != null) {
              // to prevent overflow issue, two operands should have the integer type larger than
              // the merged type (aka. type derived from result range)
              CExpression op1 = e.getOperand1();
              AccessPath path1;
              if (op1 instanceof CLeftHandSide) {
                path1 = RangeState.getAccessPath(cell.getState(), cell.getOtherStates(),
                    (CLeftHandSide) op1, machineModel);
                if (path1 != null) {
                  info.addTypeConstraint(IntegerTypePredicate.COVER, path1, newType, true);
                }
              }
              info.addCandidateFix(e.getOperand1().getFileLocation(), IntegerFixMode.CAST,
                  newType);
              // only the first operand is considered for bit shift operation
              if (!operator.isBitShiftOperator()) {
                CExpression op2 = e.getOperand2();
                AccessPath path2;
                if (op2 instanceof CLeftHandSide) {
                  path2 = RangeState.getAccessPath(cell.getState(), cell.getOtherStates(),
                      (CLeftHandSide) op2, machineModel);
                  if (path2 != null) {
                    info.addTypeConstraint(IntegerTypePredicate.COVER, path2, newType, true);
                  }
                }
                info.addCandidateFix(e.getOperand2().getFileLocation(), IntegerFixMode.CAST,
                    newType);
              }
            } else {
              info.incPunishCount();
            }
          }
        }

        boolean sanitize = sanitizeFunctionArg && isCriticalEdge(cfaEdge);
        if (refine || sanitize) {
          resultRange = sanitize ? Range.EMPTY : resultRange.intersect(restriction);
          RangeState newState = e.accept(new RangeRefineVisitor(cell.getState(), cell
              .getOtherStates(), resultRange, machineModel, false));
          // update ranges of two operands
          Range newR1 = evaluateRange(newState, cell.getOtherStates(), e.getOperand1());
          Range newR2 = evaluateRange(newState, cell.getOtherStates(), e.getOperand2());
          // generate a new expression cell
          return new ExpressionCell<>(newState, cell
              .getOtherStates(), Lists.newArrayList(newR1, newR2), resultRange);
        }
        return new ExpressionCell<>(cell.getState(), cell.getOtherStates(), cell.getOperands(),
            resultRange);
      }
    }
  }

  private ExpressionCell<RangeState, Range> handleFloatLiteralExpression
      (
          CFloatLiteralExpression e, ExpressionCell<RangeState, Range> cell, Range restriction,
          @Nullable CFAEdge cfaEdge) {
    Range resultRange = cell.getResult();
    if (restriction.contains(resultRange)) {
      return cell;
    } else {
      IntegerOverflowErrorReport error = new IntegerOverflowErrorReport(e, cfaEdge, this);
      errorStore.add(error);
      return cell;
    }
  }

  private ExpressionCell<RangeState, Range> handleIntegerLiteralExpression
      (
          CIntegerLiteralExpression e, ExpressionCell<RangeState, Range> cell, Range restriction,
          @Nullable CFAEdge cfaEdge) {
    Range resultRange = cell.getResult();
    if (restriction.contains(resultRange)) {
      return cell;
    } else {
      IntegerOverflowErrorReport error = new IntegerOverflowErrorReport(e, cfaEdge, this);
      errorStore.add(error);
      return cell;
    }
  }

  private ExpressionCell<RangeState, Range> handleUnaryExpression
      (
          CUnaryExpression e, ExpressionCell<RangeState, Range> cell, Range restriction, @Nullable
          CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {
    Range r = cell.getOperand(0);
    CExpression operand = e.getOperand();
    UnaryOperator operator = e.getOperator();
    Range resultRange = Ranges.getUnaryResult(operator, r, e, machineModel);
    if (resultRange.isUnbound()) {
      // non-numerical value
      return cell;
    }
    if (restriction.contains(resultRange)) {
      return cell;
    } else {
      IntegerOverflowErrorReport error = new IntegerOverflowErrorReport(e, cfaEdge, this);
      errorStore.add(error);

      IntegerFixInfo info = (IntegerFixInfo) FixProvider.getFixInfo(BugCategory.INTEGER);
      if (info != null) {
        CompInteger lower = resultRange.getLow();
        CompInteger upper = resultRange.getHigh();
        if (lower.getStatus() == IntegerStatus.NORM && upper.getStatus() == IntegerStatus.NORM) {
          CSimpleType newType = CTypes.getTypeFromRange(machineModel, lower.getValue(), upper
              .getValue());
          if (newType != null) {
            CExpression op = e.getOperand();
            AccessPath opPath;
            if (op instanceof CLeftHandSide) {
              opPath = RangeState.getAccessPath(cell.getState(), cell.getOtherStates(),
                  (CLeftHandSide) op, machineModel);
              if (opPath != null) {
                info.addTypeConstraint(IntegerTypePredicate.COVER, opPath, newType, true);
              }
            }
            info.addCandidateFix(e.getOperand().getFileLocation(), IntegerFixMode.CAST, newType);
          } else {
            info.incPunishCount();
          }
        }
      }

      boolean sanitize = sanitizeFunctionArg && isCriticalEdge(cfaEdge);
      if (refine || sanitize) {
        resultRange = sanitize ? Range.EMPTY : resultRange.intersect(restriction);
        RangeState newState = e.accept(new RangeRefineVisitor(cell.getState(), cell
            .getOtherStates(), resultRange, machineModel, false));
        Range newR = evaluateRange(newState, cell.getOtherStates(), operand);
        return new ExpressionCell<>(newState, cell.getOtherStates(), Lists.newArrayList(newR),
            resultRange);
      }
      return cell;
    }
  }

  private ExpressionCell<RangeState, Range> handleFunctionCallExpression
      (CFunctionCallExpression e, ExpressionCell<RangeState, Range> cell) {
    return cell;
  }

  private Range evaluateRange(
      RangeState rangeState, List<AbstractState> otherStates,
      CRightHandSide rightHand)
      throws UnrecognizedCCodeException {
    return rightHand.accept(new ExpressionRangeVisitor(rangeState, otherStates, machineModel,
        false));
  }

  /**
   * Examine whether the given CFA edge is critical (i.e. possibly requiring strong sanitization).
   */
  private boolean isCriticalEdge(CFAEdge cfaEdge) {
    switch (cfaEdge.getEdgeType()) {
      case StatementEdge:
        CStatement statement = ((CStatementEdge) cfaEdge).getStatement();
        if (statement instanceof CFunctionCall) {
          return true;
        }
        break;
      default:
        break;
    }
    return false;
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
    return Weakness.INTEGER_OVERFLOW;
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return RangeState.class;
  }
}
