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

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.range.ArrayUncertainIndexSegment;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.util.RangeFunctionAdapter;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.PathSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The state refiner class
 */
public final class RangeRefineVisitor
    extends DefaultCExpressionVisitor<RangeState, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<RangeState, UnrecognizedCCodeException> {

  private RangeState internalState;

  private final Range restrictRange;
  private final MachineModel machineModel;
  private final List<AbstractState> otherStates;

  private final boolean forSummary;

  public RangeRefineVisitor(
      RangeState state, List<AbstractState> others,
      Range restrict, MachineModel model, boolean pForSummary) {
    internalState = state;
    otherStates = others;
    restrictRange = restrict;
    machineModel = model;
    forSummary = pForSummary;
  }

  @Override
  public RangeState visit(CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    AccessPath path = RangeState.getAccessPath(internalState, otherStates, e, machineModel);
    if (path == null) {
      return internalState;
    } else {
      if (isSingletonAccessPath(path)) {
        Range refinedRange = internalState.getRange(path, machineModel);
        refinedRange = refinedRange.intersect(restrictRange);
        internalState.addRange(path, refinedRange, forSummary);
      }
      return internalState;
    }
  }

  @Override
  public RangeState visit(CBinaryExpression e) throws UnrecognizedCCodeException {
    CExpression operand1 = e.getOperand1();
    CExpression operand2 = e.getOperand2();
    if (restrictRange.isEmpty()) {
      // if the restrict range is empty, we should restrict operands with empty ranges as well
      internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates, Range
          .EMPTY, machineModel, forSummary));
      internalState = operand2.accept(new RangeRefineVisitor(internalState, otherStates, Range
          .EMPTY, machineModel, forSummary));
      return internalState;
    }
    BinaryOperator operator = e.getOperator();
    Range range1 = RangeState.evaluateRange(internalState, otherStates, operand1, machineModel);
    Range range2 = RangeState.evaluateRange(internalState, otherStates, operand2, machineModel);
    if (range1.isEmpty() || range2.isEmpty()) {
      // If any operand is empty, then the result is empty (which is definitely contained by any
      // restriction range)
      return internalState;
    }
    switch (operator) {
      case PLUS: {
        Range newRange1, newRange2;
        newRange1 = new Range(restrictRange.getLow().subtract(range2.getHigh()), restrictRange
            .getHigh().subtract(range2.getLow()));
        newRange2 = new Range(restrictRange.getLow().subtract(range1.getHigh()), restrictRange
            .getHigh().subtract(range1.getLow()));
        range1 = range1.intersect(newRange1);
        range2 = range2.intersect(newRange2);
        // follow these two sub-expressions
        internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates, range1,
            machineModel, forSummary));
        internalState = operand2.accept(new RangeRefineVisitor(internalState, otherStates, range2,
            machineModel, forSummary));
        return internalState;
      }
      case MINUS: {
        Range newRange1, newRange2;
        newRange1 = new Range(restrictRange.getLow().add(range2.getLow()), restrictRange
            .getHigh().add(range2.getHigh()));
        newRange2 = new Range(range1.getLow().subtract(restrictRange.getHigh()), range1.getHigh
            ().subtract(restrictRange.getLow()));
        range1 = range1.intersect(newRange1);
        range2 = range2.intersect(newRange2);
        internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates, range1,
            machineModel, forSummary));
        internalState = operand2.accept(new RangeRefineVisitor(internalState, otherStates, range2,
            machineModel, forSummary));
        return internalState;
      }
      case MULTIPLY: {
          /*
          Assume we have two expressions A and B, and we have:
          A: [x1, y1]; B: [x2, y2]
          This can be depicted by the following figure.
          y2---------------------------
                |                     |
                |                     |
          x2---------------------------
                |                     |
                x1                    y1
           If an edge (consider x2-y2 for example) is outside the area of A * B: [X, Y],
           we can move this edge (i.e. changing the value of x1) to refine ranges.

           ADDITIONAL NOTES: we only move the PURE edge, which does not cross over border figures.
           */
        /*if (range1.equals(Range.ZERO) && range2.equals(Range.ZERO)) {
          // a very extreme case: both ranges are zero singleton
          if (restrictRange.in(CompInteger.ZERO)) {
            return internalState;
          } else {
            internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates, Range
                .EMPTY, machineModel, forSummary));
            internalState = operand2.accept(new RangeRefineVisitor(internalState, otherStates, Range
                .EMPTY, machineModel, forSummary));
          }
          return internalState;
        }
        // the pure (movable) status of four borders
        Boolean bottom = null, top = null, left = null, right = null;
        boolean zeroRangeUnmovable = restrictRange.in(CompInteger.ZERO);
        if (range1.getLow().equals(CompInteger.ZERO)) {
          left = !zeroRangeUnmovable;
        }
        if (range1.getHigh().equals(CompInteger.ZERO)) {
          right = !zeroRangeUnmovable;
        }
        if (range2.getLow().equals(CompInteger.ZERO)) {
          bottom = !zeroRangeUnmovable;
        }
        if (range2.getHigh().equals(CompInteger.ZERO)) {
          top = !zeroRangeUnmovable;
        }
        List<CompInteger> horizon = new ArrayList<>(4);
        List<CompInteger> vertical = new ArrayList<>(4);
        if (bottom == null) {
          CompInteger cross1 = restrictRange.getLow().divide(range2.getLow());
          CompInteger cross2 = restrictRange.getHigh().divide(range2.getLow());
          horizon.add(cross1);
          horizon.add(cross2);
          Range bottomRange = new Range(cross1, cross2);
          if (range1.intersects(bottomRange)) {
            bottom = false;
          } else {
            bottom = true;
          }
        }
        if (top == null) {
          CompInteger cross1 = restrictRange.getLow().divide(range2.getHigh());
          CompInteger cross2 = restrictRange.getHigh().divide(range2.getHigh());
          horizon.add(cross1);
          horizon.add(cross2);
          Range topRange = new Range(cross1, cross2);
          if (range1.intersects(topRange)) {
            top = false;
          } else {
            top = true;
          }
        }
        if (left == null) {
          CompInteger cross1 = restrictRange.getLow().divide(range1.getLow());
          CompInteger cross2 = restrictRange.getHigh().divide(range1.getLow());
          vertical.add(cross1);
          vertical.add(cross2);
          Range leftRange = new Range(cross1, cross2);
          if (range2.intersects(leftRange)) {
            left = false;
          } else {
            left = true;
          }
        }
        if (right == null) {
          CompInteger cross1 = restrictRange.getLow().divide(range1.getHigh());
          CompInteger cross2 = restrictRange.getHigh().divide(range1.getHigh());
          vertical.add(cross1);
          vertical.add(cross2);
          Range rightRange = new Range(cross1, cross2);
          if (range2.intersects(rightRange)) {
            right = false;
          } else {
            right = true;
          }
        }
        // if we reach here, the pure status of each border should be clear
        Preconditions.checkNotNull(left);
        Preconditions.checkNotNull(right);
        Preconditions.checkNotNull(top);
        Preconditions.checkNotNull(bottom);
        Range newRange1 = range1, newRange2 = range2;
        if (left) {
          // left border is movable
          if (horizon.isEmpty()) {
            newRange1 = Range.EMPTY;
          } else {
            CompInteger target = Collections.min(horizon);
            if (newRange1.in(target)) {
              newRange1 = new Range(target, newRange1.getHigh());
            } else {
              newRange1 = Range.EMPTY;
            }
          }
        }
        if (!newRange1.isEmpty() && right) {
          CompInteger target = Collections.max(horizon);
          if (newRange1.in(target)) {
            newRange1 = new Range(newRange1.getLow(), target);
          } else {
            newRange1 = Range.EMPTY;
          }
        }
        if (bottom) {
          // bottom border is movable
          if (vertical.isEmpty()) {
            newRange2 = Range.EMPTY;
          } else {
            CompInteger target = Collections.min(vertical);
            if (newRange2.in(target)) {
              newRange2 = new Range(target, newRange2.getHigh());
            } else {
              newRange2 = Range.EMPTY;
            }
          }
        }
        if (!newRange2.isEmpty() && top) {
          CompInteger target = Collections.max(vertical);
          if (newRange2.in(target)) {
            newRange2 = new Range(newRange2.getLow(), target);
          } else {
            newRange2 = Range.EMPTY;
          }
        }
        if (!newRange1.equals(range1)) {
          internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates,
              newRange1, machineModel, forSummary));
        }
        if (!newRange2.equals(range2)) {
          internalState = operand2.accept(new RangeRefineVisitor(internalState, otherStates,
              newRange2, machineModel, forSummary));
        }*/
        return internalState;
      }
      case SHIFT_LEFT: {
        // this case is similar with multiplication
        // if the offset is negative, we will not prune it in this checker. It is the
        // responsibility of undefined behavior checker to refine the value of offset. However,
        // since the shift operation with negative offset cannot be completed and continue
        // the following execution, we discard negative values in computing resultant ranges.

        // first, we restrict the range to non-negative
        /*restrictRange = restrictRange.intersect(Range.NONNEGATIVE);
        if (restrictRange.isEmpty()) {
          return internalState;
        }
        // then, we restrict ranges of operands: the first one should be non-negative, the
        // second should range in 0 to INT_MAX.
        range1 = range1.intersect(Range.NONNEGATIVE);
        range2 = range2.intersect(Range.NONNEGATIVE);
        if (range1.isEmpty() || range2.isEmpty()) {
          return internalState;
        }
        Range intMaxRange = new Range(Integer.MAX_VALUE);
        if (range2.isGreaterThan(intMaxRange)) {
          // range1 should be refined as zero singleton
          range1 = range1.intersect(Range.ZERO);
          internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates,
              range1, machineModel));
          return internalState;
        }
        Boolean bottom = null, top = null, left = null, right;
        List<CompInteger> horizon = new ArrayList<>(4);
        List<CompInteger> vertical = new ArrayList<>(4);
        if (range2.mayBeGreaterThan(intMaxRange)) {
          // in this special case, the pure status of borders can be directly determined
          if (!range1.getLow().equals(CompInteger.ZERO)) {
            range2 = range2.intersect(new Range(0L, Integer.MAX_VALUE));
          } else {
            top = false;
            left = false;
            bottom = false;
            // right border could be moved, in some cases
          }
        }
        // compute pure status of borders
        Range backUpRange1 = range1;
        Range backUpRange2 = range2.intersect(new Range(0L, Integer.MAX_VALUE));
        if (bottom == null) {
          CompInteger cross1 = restrictRange.getLow().shiftRight(backUpRange2.getLow().intValue
              ());
          CompInteger cross2 = restrictRange.getHigh().shiftRight(backUpRange2.getLow()
              .intValue());
          horizon.add(cross1);
          horizon.add(cross2);
          Range bottomRange = new Range(cross1, cross2);
          if (backUpRange1.intersects(bottomRange)) {
            bottom = false;
          } else {
            bottom = true;
          }
        }
        if (top == null) {
          CompInteger cross1 = restrictRange.getLow().shiftRight(backUpRange2.getHigh()
              .intValue());
          CompInteger cross2 = restrictRange.getHigh().shiftRight(backUpRange2.getHigh()
              .intValue());
          horizon.add(cross1);
          horizon.add(cross2);
          Range topRange = new Range(cross1, cross2);
          if (backUpRange1.intersects(topRange)) {
            top = false;
          } else {
            top = true;
          }
        }
        if (left == null) {
          if (backUpRange1.getLow().equals(CompInteger.ZERO)) {
            if (restrictRange.getLow().equals(CompInteger.ZERO)) {
              left = false;
            } else {
              left = true;
            }
          } else {
            CompInteger division1 = restrictRange.getLow().divide(backUpRange1.getLow());
            CompInteger division2 = restrictRange.getHigh().divide(backUpRange1.getLow());
            CompInteger cross1, cross2;
            if (division1.equals(CompInteger.ZERO)) {
              cross1 = CompInteger.ZERO;
            } else {
              double divisionDouble = division1.getValue().doubleValue();
              double cross1Double = Math.log(divisionDouble) / Math.log(2);
              cross1Double = Math.floor(cross1Double);
              cross1 = new CompInteger(BigDecimal.valueOf(cross1Double).toBigInteger());
            }
            if (division2.equals(CompInteger.ZERO)) {
              cross2 = CompInteger.ZERO;
            } else {
              double divisionDouble = division2.getValue().doubleValue();
              double cross2Double = Math.log(divisionDouble) / Math.log(2);
              cross2Double = Math.floor(cross2Double);
              cross2 = new CompInteger(BigDecimal.valueOf(cross2Double).toBigInteger());
            }
            vertical.add(cross1);
            vertical.add(cross2);
            Range leftRange = new Range(cross1, cross2);
            if (backUpRange2.intersects(leftRange)) {
              left = false;
            } else {
              left = true;
            }
          }
        }
        {
          // the flag {@code right} should always be null here
          if (backUpRange1.getHigh().equals(CompInteger.ZERO)) {
            if (restrictRange.getLow().equals(CompInteger.ZERO)) {
              right = false;
            } else {
              right = true;
            }
          } else {
            CompInteger division1 = restrictRange.getLow().divide(backUpRange1.getHigh());
            CompInteger division2 = restrictRange.getHigh().divide(backUpRange1.getHigh());
            CompInteger cross1, cross2;
            if (division1.equals(CompInteger.ZERO)) {
              cross1 = CompInteger.ZERO;
            } else {
              double divisionDouble = division1.getValue().doubleValue();
              double cross1Double = Math.log(divisionDouble) / Math.log(2);
              cross1Double = Math.ceil(cross1Double);
              cross1 = new CompInteger(BigDecimal.valueOf(cross1Double).toBigInteger());
            }
            if (division2.equals(CompInteger.ZERO)) {
              cross2 = CompInteger.ZERO;
            } else {
              double divisionDouble = division2.getValue().doubleValue();
              double cross2Double = Math.log(divisionDouble) / Math.log(2);
              cross2Double = Math.ceil(cross2Double);
              cross2 = new CompInteger(BigDecimal.valueOf(cross2Double).toBigInteger());
            }
            vertical.add(cross1);
            vertical.add(cross2);
            Range rightRange = new Range(cross1, cross2);
            if (backUpRange2.intersects(rightRange)) {
              right = false;
            } else {
              right = true;
            }
          }
        }
        // move borders with respect of pure status
        Range newRange1 = range1, newRange2 = range2;
        if (left) {
          if (horizon.isEmpty()) {
            // this should not be reached
            newRange1 = Range.EMPTY;
          } else {
            CompInteger target = Collections.min(horizon);
            if (newRange1.in(target)) {
              newRange1 = new Range(target, newRange1.getHigh());
            } else {
              newRange1 = Range.EMPTY;
            }
          }
        }
        if (!newRange1.isEmpty() && right) {
          CompInteger target = Collections.max(horizon);
          if (newRange1.in(target)) {
            newRange1 = new Range(newRange1.getLow(), target);
          } else {
            newRange1 = Range.EMPTY;
          }
        }
        if (bottom) {
          if (vertical.isEmpty()) {
            newRange2 = Range.EMPTY;
          } else {
            CompInteger target = Collections.min(vertical);
            if (newRange2.in(target)) {
              newRange2 = new Range(target, newRange2.getHigh());
            } else {
              newRange2 = Range.EMPTY;
            }
          }
        }
        if (!newRange2.isEmpty() && top) {
          CompInteger target = Collections.max(vertical);
          if (newRange2.in(target)) {
            newRange2 = new Range(newRange2.getLow(), target);
          } else {
            newRange2 = Range.EMPTY;
          }
        }
        if (!newRange1.equals(range1)) {
          internalState = operand1.accept(new RangeRefineVisitor(internalState, otherStates,
              newRange1, machineModel));
        }
        if (!newRange2.equals(range2)) {
          internalState = operand2.accept(new RangeRefineVisitor(internalState, otherStates,
              newRange2, machineModel));
        }*/
        return internalState;
      }
      default: {
        // for other cases, there is no need to refine
        // Although division possibly introduces overflow, such as INT_MAX / -1, however even
        // for this extreme case we have no way to refine the state.
        return internalState;
      }
    }
  }

  @Override
  public RangeState visit(CCastExpression e) throws UnrecognizedCCodeException {
    // for cast expression, we just pass the restriction to the operand
    CType castType = e.getCastType();
    Range castRange = Ranges.getTypeRange(castType, machineModel);
    if (castRange.contains(restrictRange)) {
      // for example, the cast expression is (T)e and the restrict range R is within T, then the
      // value of e must be in R, otherwise (T)e will exceed R
      CExpression operand = e.getOperand();
      internalState = operand.accept(new RangeRefineVisitor(internalState, otherStates,
          restrictRange, machineModel, forSummary));
    }
    // otherwise, we do nothing
    return internalState;
  }

  @Override
  public RangeState visit(CFieldReference e) throws UnrecognizedCCodeException {
    AccessPath path = RangeState.getAccessPath(internalState, otherStates, e, machineModel);
    if (path == null) {
      return internalState;
    } else {
      Range refinedRange = internalState.getRange(path, machineModel);
      refinedRange = refinedRange.intersect(restrictRange);
      internalState.addRange(path, refinedRange, forSummary);
    }
    return internalState;
  }

  @Override
  public RangeState visit(CIdExpression e) throws UnrecognizedCCodeException {
    AccessPath path = RangeState.getAccessPath(internalState, otherStates, e, machineModel);
    Preconditions.checkNotNull(path);
    Range refinedRange = internalState.getRange(path, machineModel);
    refinedRange = refinedRange.intersect(restrictRange);
    internalState.addRange(path, refinedRange, forSummary);
    return internalState;
  }

  @Override
  public RangeState visit(CUnaryExpression e) throws UnrecognizedCCodeException {
    CExpression operand = e.getOperand();
    UnaryOperator operator = e.getOperator();
    switch (operator) {
      case MINUS:
        Range range = RangeState.evaluateRange(internalState, otherStates, operand, machineModel);
        Range negativeTarget = restrictRange.negate();
        if (!negativeTarget.contains(range)) {
          range = range.intersect(negativeTarget);
          internalState = operand.accept(new RangeRefineVisitor(internalState, otherStates, range,
              machineModel, forSummary));
        }
        return internalState;
      default:
        return internalState;
    }
  }

  @Override
  public RangeState visit(CPointerExpression e) throws UnrecognizedCCodeException {
    AccessPath path = RangeState.getAccessPath(internalState, otherStates, e, machineModel);
    if (path == null) {
      return internalState;
    } else {
      Range refinedRange = internalState.getRange(path, machineModel);
      refinedRange = refinedRange.intersect(restrictRange);
      internalState.addRange(path, refinedRange, forSummary);
    }
    return internalState;
  }

  @Override
  public RangeState visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    return RangeFunctionAdapter.instance(forSummary).refineFunctionCallExpression
        (pIastFunctionCallExpression, restrictRange, internalState, otherStates);
  }

  @Override
  protected RangeState visitDefault(CExpression exp) throws UnrecognizedCCodeException {
    // by default, no refinement is required
    return internalState;
  }

  private boolean isSingletonAccessPath(AccessPath path) {
    List<PathSegment> segments = path.path();
    for (PathSegment segment : segments) {
      if (segment instanceof ArrayUncertainIndexSegment) {
        Range indexRange = ((ArrayUncertainIndexSegment) segment).getIndexRange();
        if (!indexRange.isSingletonRange()) {
          return false;
        }
      }
    }
    return true;
  }
}
