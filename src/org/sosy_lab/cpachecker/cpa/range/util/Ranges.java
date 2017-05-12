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
package org.sosy_lab.cpachecker.cpa.range.util;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.function.ADCombinator;
import org.sosy_lab.cpachecker.core.interfaces.function.ADUnit;
import org.sosy_lab.cpachecker.cpa.range.CompInteger;
import org.sosy_lab.cpachecker.cpa.range.ExpressionRangeVisitor;
import org.sosy_lab.cpachecker.cpa.range.LeftHandAccessPathVisitor;
import org.sosy_lab.cpachecker.cpa.range.Range;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

public final class Ranges {

  private Ranges() {
  }

  public static Range getTypeRange(CType type, MachineModel model) {
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CSimpleType) {
      BigInteger min = model.getMinimalIntegerValue((CSimpleType) type);
      BigInteger max = model.getMaximalIntegerValue((CSimpleType) type);
      return new Range(new CompInteger(min), new CompInteger(max));
    } else {
      return Range.UNBOUND;
    }
  }

  public static Range getTypeRange(CRightHandSide expression, MachineModel model) {
    CType type;
    if (expression instanceof CBinaryExpression) {
      type = ((CBinaryExpression) expression).getCalculationType();
    } else {
      type = expression.getExpressionType();
    }
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    // given a numerical type, the type range must be bounded
    if (type instanceof CSimpleType) {
      BigInteger min = model.getMinimalIntegerValue((CSimpleType) type);
      BigInteger max = model.getMaximalIntegerValue((CSimpleType) type);
      return new Range(new CompInteger(min), new CompInteger(max));
    } else {
      return Range.UNBOUND;
    }
  }

  public static Range getLogicalResult(BinaryOperator pOperator, Range range1, Range range2) {
    if (range1.isEmpty() || range2.isEmpty()) {
      return Range.EMPTY;
    }
    switch (pOperator) {
      case EQUALS:
        if (!range1.intersects(range2)) {
          return Range.ZERO;
        } else if (range1.isSingletonRange() && range1.equals(range2)) {
          return Range.ONE;
        } else {
          return Range.BOOL;
        }
      case NOT_EQUALS:
        if (!range1.intersects(range2)) {
          return Range.ONE;
        } else if (range1.isSingletonRange() && range1.equals(range2)) {
          return Range.ZERO;
        } else {
          return Range.BOOL;
        }
      case GREATER_THAN:
        if (range1.isGreaterThan(range2)) {
          return Range.ONE;
        } else if (range2.isGreaterOrEqualThan(range1)) {
          return Range.ZERO;
        } else {
          return Range.BOOL;
        }
      case GREATER_EQUAL:
        return getLogicalResult(BinaryOperator.GREATER_THAN, range1.plus(1L), range2);
      case LESS_THAN:
        return getLogicalResult(BinaryOperator.GREATER_THAN, range2, range1);
      case LESS_EQUAL:
        return getLogicalResult(BinaryOperator.GREATER_THAN, range2.plus(1L), range1);
      default:
        throw new AssertionError("unknown binary operator: " + pOperator);
    }
  }

  public static Range getArithmeticResult(
      BinaryOperator pOperator, Range range1, Range range2,
      CBinaryExpression e, MachineModel model) {
    if (range1.isEmpty() || range2.isEmpty()) {
      return Range.EMPTY;
    }
    switch (pOperator) {
      case PLUS:
        return range1.plus(range2);
      case MINUS:
        return range1.minus(range2);
      case MULTIPLY:
        return range1.times(range2);
      case DIVIDE:
        return range1.divide(range2);
      case SHIFT_LEFT: {
        Range resultRange = range1.shiftLeft(range2);
        CType canonicalType = e.getCalculationType().getCanonicalType();
        if (canonicalType instanceof CSimpleType && ((CSimpleType) canonicalType).isSigned()) {
          // signed left-shift
          return resultRange;
        }
        Range typeRange = Ranges.getTypeRange(e, model);
        // wraparound in shift-left could be benign (bit-vector semantics)
        if (!typeRange.contains(resultRange)) {
          return typeRange;
        } else {
          return resultRange;
        }
      }
      case SHIFT_RIGHT:
        return range1.shiftRight(range2);
      case MODULO:
        return range1.modulo(range2);
      case BINARY_AND: {
        // we can handle one special case carefully
        // if e is unsigned, we compare the highest bit of two operands and refine the result
        int bit1 = getHighestBit(range1);
        int bit2 = getHighestBit(range2);
        if (bit1 < 0 || bit2 < 0) {
          return Ranges.getTypeRange(e, model);
        }
        int lowBit = (bit1 < bit2) ? bit1 : bit2;
        BigInteger newUpper = BigInteger.ONE.shiftLeft(lowBit);
        return new Range(CompInteger.ZERO, new CompInteger(newUpper));
      }
      case BINARY_OR:
        int bit1 = getHighestBit(range1);
        int bit2 = getHighestBit(range2);
        if (bit1 < 0 || bit2 < 0) {
          return Ranges.getTypeRange(e, model);
        }
        int highBit = (bit1 > bit2) ? bit1 : bit2;
        BigInteger newUpper = BigInteger.ONE.shiftLeft(highBit);
        return new Range(CompInteger.ZERO, new CompInteger(newUpper));
      case BINARY_XOR:
        // this is very hard to say
        return Ranges.getTypeRange(e, model);
      default:
        throw new AssertionError("unknown binary operator: " + pOperator);
    }
  }

  /**
   * The highest bit that the value denoted by the given range has
   *
   * @param range given range
   * @return position of the highest bit, -1 for the "as high as possible"
   */
  private static int getHighestBit(Range range) {
    CompInteger low = range.getLow();
    CompInteger high = range.getHigh();
    if (low.signum() < 0 || high.equals(CompInteger.POSITIVE_INF)) {
      return -1;
    }
    BigInteger intHigh = high.getValue();
    return intHigh.bitLength();
  }

  public static Range getUnaryResult(
      UnaryOperator pOperator, Range range, CUnaryExpression e,
      MachineModel model) {
    switch (pOperator) {
      case MINUS:
        return range.negate();
      case AMPER: {
        Range result;
        try {
          result = handleOffsetOf(e.getOperand(), model);
        } catch (UnrecognizedCCodeException ex) {
          result = Range.UNBOUND;
        }
        return result;
      }
      case TILDE:
        return Ranges.getTypeRange(e, model);
      case SIZEOF:
        return new Range(model.getSizeof(e.getOperand().getExpressionType()));
      case ALIGNOF:
        return new Range(model.getAlignof(e.getOperand().getExpressionType()));
      default:
        throw new AssertionError("unknown unary operator: " + pOperator);
    }
  }

  private static Range handleOffsetOf(CExpression e, MachineModel pModel) throws
                                                                          UnrecognizedCCodeException {
    // there are many uses of __builtin_offsetof() macros in real-world programs
    if (e instanceof CFieldReference) {
      if (((CFieldReference) e).isPointerDereference()) {
        String field = ((CFieldReference) e).getFieldName();
        CExpression owner = ((CFieldReference) e).getFieldOwner();
        if (owner instanceof CCastExpression) {
          CType cast = ((CCastExpression) owner).getCastType();
          CExpression likelyConst = ((CCastExpression) owner).getOperand();
          if (likelyConst instanceof CIntegerLiteralExpression) {
            CPointerType type = Types.extractPointerType(cast);
            if (type != null) {
              CType coreType = type.getType();
              Long offset = Types.getFieldInfo(coreType, field, pModel).getFirst();
              if (offset != null) {
                Range singletonRange = new Range(
                    new CompInteger(((CIntegerLiteralExpression) likelyConst).getValue()));
                return singletonRange.plus(offset);
              }
            }
          }
        }
      }
    }
    return Range.UNBOUND;
  }

  public static ADCombinator<Range> createADCombinatorFromExpression
      (
          CExpression argument, RangeState currentState, List<AbstractState> otherStates,
          MachineModel model) throws UnrecognizedCCodeException {

    if (argument instanceof CLeftHandSide) {
      // get the access path
      Optional<AccessPath> orNullPath = ((CLeftHandSide) argument).accept(new
          LeftHandAccessPathVisitor(new ExpressionRangeVisitor(currentState, otherStates, model,
          false)));
      if (!orNullPath.isPresent()) {
        return new ADCombinator<>(null);
      }
      // Note: For pointer expressions, we need to strengthen with shape state to derive correct
      // result. This procedure should be put in access path visitor
      AccessPath actualPath = orNullPath.get();
      return currentState.createADCombinatorFromPrefix(actualPath);
    } else if (argument instanceof CStringLiteralExpression) {
      // an literal expression has no name, so we should create a dummy (prefix) access path for
      // this combinator
      AccessPath dummyPath = AccessPath.createDummyAccessPath(new CPointerType(true, false,
          CNumericTypes.CHAR));
      ADCombinator<Range> combinator = new ADCombinator<>(dummyPath);
      String content = ((CStringLiteralExpression) argument).getContentString();
      for (int i = 0; i < content.length(); i++) {
        List<String> indexSegment = new LinkedList<>();
        indexSegment.add(new ArrayConstIndexSegment(i).getName());
        ADUnit<Range> newUnit = new ADUnit<>(new Range(content.charAt(i)), indexSegment, dummyPath);
        combinator.insertValue(newUnit);
      }
      // insert ZERO at the tail
      List<String> tailSegment = new LinkedList<>();
      tailSegment.add(new ArrayConstIndexSegment(content.length()).getName());
      combinator.insertValue(new ADUnit<>(Range.ZERO, tailSegment, dummyPath));
      return combinator;
    } else {
      // unsupported cases
      return new ADCombinator<>(null);
    }

  }

}
