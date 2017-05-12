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
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
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
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

@Options(prefix = "checker.range.divbyzero")
public class DividedByZeroChecker implements ExpressionChecker<RangeState, Range>,
                                             CheckerWithInstantErrorReport {

  @Option(secure = true, name = "refine", description = "whether the given state will be refined")
  private boolean refine = false;

  private final MachineModel machineModel;
  private final List<ErrorReport> errorStore;

  public DividedByZeroChecker(Configuration pConfig) throws InvalidConfigurationException {
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
    // TODO: a critical state should be the nearest one that makes the dividend be zero
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
      return handleBinaryExpression((CBinaryExpression) rightHand, cell, cfaEdge);
    } else {
      return cell;
    }
  }

  private ExpressionCell<RangeState, Range> handleBinaryExpression
      (CBinaryExpression e, ExpressionCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {
    BinaryOperator operator = e.getOperator();
    CExpression operand2 = e.getOperand2();
    Range r1 = cell.getOperand(0);
    Range r2 = cell.getOperand(1);
    if (operator == BinaryOperator.DIVIDE || operator == BinaryOperator.MODULO) {
      if (r2.contains(Range.ZERO)) {
        // divided-by-zero error possibly occurs here
        DividedByZeroErrorReport error = new DividedByZeroErrorReport(operand2, cfaEdge, this);
        errorStore.add(error);
        if (refine) {
          // for now we do not split a range state since some issues have not been carefully
          // considered yet
          if (r2.getLow().equals(CompInteger.ZERO) || r2.getHigh().equals(CompInteger.ZERO)) {
            r2 = r2.complement(Range.ZERO);
            RangeState newState = operand2.accept(new RangeRefineVisitor(cell.getState(), cell
                .getOtherStates(), r2, machineModel, false));
            List<Range> newOperands = Lists.newArrayList(r1, r2);
            Range resultRange = Ranges.getArithmeticResult(operator, r1, r2, e, machineModel);
            return new ExpressionCell<>(newState, cell.getOtherStates(), newOperands, resultRange);
          }
        }
      }
    }
    return cell;
  }

  @Override
  public AssignmentCell<RangeState, Range> checkAndRefine(
      CAssignment assignment, AssignmentCell<RangeState, Range> cell, @Nullable CFAEdge cfaEdge)
      throws CPATransferException {
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
    return Weakness.DIVIDED_BY_ZERO;
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return RangeState.class;
  }
}
