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
package org.sosy_lab.cpachecker.cpa.range;

import static org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator.AMPER;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.range.util.RangeFunctionAdapter;
import org.sosy_lab.cpachecker.cpa.range.util.Ranges;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public final class ExpressionRangeVisitor extends DefaultCExpressionVisitor<Range,
    UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Range, UnrecognizedCCodeException> {

  private final RangeState readableState;
  private final MachineModel model;

  // other components of abstract states are required for precise reasoning of program semantics
  private final List<AbstractState> otherStates;

  // range computations are purely arithmetic in summary derivation
  private final boolean forSummary;

  private final boolean truncatedCastNotAllowed;

  public ExpressionRangeVisitor(RangeState pState, MachineModel pModel, boolean pForSummary) {
    readableState = pState;
    Preconditions.checkNotNull(pModel);
    model = pModel;
    otherStates = null;
    forSummary = pForSummary;
    truncatedCastNotAllowed = false;
  }

  public ExpressionRangeVisitor(
      RangeState pState,
      List<AbstractState> pOtherStates,
      MachineModel pModel,
      boolean pForSummary) {
    readableState = pState;
    Preconditions.checkNotNull(pModel);
    model = pModel;
    otherStates = pOtherStates;
    forSummary = pForSummary;
    truncatedCastNotAllowed = false;
  }

  ExpressionRangeVisitor(
      RangeState pState,
      List<AbstractState> pOtherStates,
      MachineModel pModel,
      boolean pForSummary,
      boolean pTruncateNotAllowed) {
    readableState = pState;
    Preconditions.checkNotNull(pModel);
    model = pModel;
    otherStates = pOtherStates;
    forSummary = pForSummary;
    truncatedCastNotAllowed = pTruncateNotAllowed;
  }

  public MachineModel getMachineModel() {
    return model;
  }

  public List<AbstractState> getOtherStates() {
    return otherStates;
  }

  @Override
  public Range visit(CArraySubscriptExpression pCArraySubscriptExpression)
      throws UnrecognizedCCodeException {
    // query if this array subscript expression is tracked in current state
    // Note: if the array is dynamically allocated, we should mark the missing information
    Optional<AccessPath> accessPath = pCArraySubscriptExpression.accept(new
        LeftHandAccessPathVisitor(this));
    Range resultRange;
    if (accessPath.isPresent()) {
      AccessPath actualPath = accessPath.get();
      resultRange = readableState.getRange(actualPath, model);
    } else {
      resultRange = Range.UNBOUND;
    }
    if (!forSummary && resultRange.isUnbound()) {
      resultRange = defaultRange(pCArraySubscriptExpression);
    }
    return resultRange;
  }

  @Override
  public Range visit(CBinaryExpression e) throws UnrecognizedCCodeException {
    CExpression op1 = e.getOperand1();
    CExpression op2 = e.getOperand2();
    Range range1 = op1.accept(this);
    Range range2 = op2.accept(this);
    if (range1 == null || range2 == null) {
      // when we could encounter this case?
      return Range.UNBOUND;
    }
    if (range1.isEmpty() || range2.isEmpty()) {
      return Range.EMPTY;
    }
    BinaryOperator binaryOperator = e.getOperator();
    if (binaryOperator.isLogicalOperator()) {
      return Ranges.getLogicalResult(binaryOperator, range1, range2);
    } else {
      // a rare case: two operands are of pointer types && the operator is MINUS
      if (Types.isPointerType(op1.getExpressionType()) && Types.isPointerType
          (op2.getExpressionType()) && binaryOperator == BinaryOperator.MINUS) {
        Range resultRange;
        Pair<AccessPath, Range> pathAndRange1 = getPathAndRange(op1);
        Pair<AccessPath, Range> pathAndRange2 = getPathAndRange(op2);
        AccessPath ap1 = pathAndRange1.getFirst();
        AccessPath ap2 = pathAndRange2.getFirst();
        if (ap1 == null || ap2 == null) {
          resultRange = Range.UNBOUND;
        } else {
          LeftHandAccessPathVisitor accessPathVisitor = new LeftHandAccessPathVisitor(this);
          resultRange = accessPathVisitor.computePointerDiff(ap1, ap2);
        }
        // restrict the range for pointer difference
        if (resultRange.isUnbound()) {
          resultRange = Ranges.getTypeRange(model.getPointerDiffType(), model);
        }
        resultRange = resultRange.plus(pathAndRange1.getSecond()).minus(pathAndRange2.getSecond());
        // restrict the range again for the result of the whole expression
        if (!forSummary && resultRange.isUnbound()) {
          resultRange = defaultRange(e);
        }
        return resultRange;
      } else if (binaryOperator == BinaryOperator.SHIFT_LEFT ||
          binaryOperator == BinaryOperator.SHIFT_RIGHT) {
        // operands of shift operation should NOT be negative
        range1 = range1.intersect(Range.NONNEGATIVE);
        range2 = range2.intersect(Range.NONNEGATIVE);
      }
      // otherwise
      return Ranges.getArithmeticResult(binaryOperator, range1, range2, e, model);
    }
  }

  /**
   * Compute the base access path and offset range of a pointer expression.
   *
   * @return binary tuple consists of (1) base access path, (2) offset range
   */
  @Nonnull
  private Pair<AccessPath, Range> getPathAndRange(CExpression e)
      throws UnrecognizedCCodeException {
    // Precondition: the input expression is of pointer type
    if (e instanceof CLeftHandSide) {
      AccessPath path = ((CLeftHandSide) e).accept(new LeftHandAccessPathVisitor(this))
          .orNull();
      return Pair.of(path, Range.ZERO);
    } else if (e instanceof CUnaryExpression) {
      UnaryOperator operator = ((CUnaryExpression) e).getOperator();
      if (operator == AMPER) {
        CExpression addr = ((CUnaryExpression) e).getOperand();
        if (!(addr instanceof CLeftHandSide)) {
          throw new AssertionError("Operand in addressing expression should be a l-value");
        }
        AccessPath path =
            ((CLeftHandSide) addr).accept(new LeftHandAccessPathVisitor(this)).orNull();
        if (path == null) {
          return Pair.of(null, Range.ZERO);
        }
        path.appendSegment(new AddressingSegment());
        return Pair.of(path, Range.ZERO);
      }
      // other cases are almost impossible, but we leave a solution here
      return Pair.of(null, Range.ZERO);
    } else if (e instanceof CBinaryExpression) {
      BinaryOperator operator = ((CBinaryExpression) e).getOperator();
      CExpression op1 = ((CBinaryExpression) e).getOperand1();
      CExpression op2 = ((CBinaryExpression) e).getOperand2();
      if (Types.isPointerType(op1.getExpressionType())) {
        Pair<AccessPath, Range> pathAndRange = getPathAndRange(op1);
        Range offset = op2.accept(this);
        if (operator == BinaryOperator.MINUS) {
          offset = offset.negate();
        }
        return Pair.of(pathAndRange.getFirst(), offset.plus(pathAndRange.getSecond()));
      } else {
        Preconditions.checkArgument(operator == BinaryOperator.PLUS);
        if (Types.isPointerType(op2.getExpressionType())) {
          Range offset = op1.accept(this);
          Pair<AccessPath, Range> pathAndRange = getPathAndRange(op2);
          return Pair.of(pathAndRange.getFirst(), offset.plus(pathAndRange.getSecond()));
        }
        return Pair.of(null, Range.ZERO);
      }
    } else if (e instanceof CCastExpression) {
      CExpression op = ((CCastExpression) e).getOperand();
      return getPathAndRange(op);
    } else {
      return Pair.of(null, Range.ZERO);
    }
  }

  @Override
  public Range visit(CCastExpression e) throws UnrecognizedCCodeException {
    CType cast = e.getCastType();
    if (!Types.isNumericalType(cast)) {
      return Range.UNBOUND;
    }
    Range operand = e.getOperand().accept(this);
    // truncate range with respect to the cast type
    // TODO: consider wraparound semantics
    if (!truncatedCastNotAllowed) {
      Range typeRange = Ranges.getTypeRange(cast, model);
      if (!typeRange.contains(operand)) {
        // a coarse-grained approximation
        operand = typeRange;
      }
      // if operand has bounds with fractions, then we should carefully truncate fractional parts
      if (!Types.isFloatType(cast)) {
        operand = operand.trunc();
      }
    }
    return operand;
  }

  @Override
  public Range visit(CFieldReference e) throws UnrecognizedCCodeException {
    Optional<AccessPath> accessPath = e.accept(new LeftHandAccessPathVisitor(this));
    Range resultRange;
    if (accessPath.isPresent()) {
      AccessPath actualPath = accessPath.get();
      resultRange = readableState.getRange(actualPath, model);
    } else {
      resultRange = Range.UNBOUND;
    }
    if (!forSummary && resultRange.isUnbound()) {
      resultRange = defaultRange(e);
    }
    return resultRange;
  }

  @Override
  public Range visit(CIdExpression e) throws UnrecognizedCCodeException {
    AccessPath accessPath = new AccessPath(e.getDeclaration());
    Range resultRange = readableState.getRange(accessPath, model);
    if (!forSummary && resultRange.isUnbound()) {
      resultRange = defaultRange(e);
    }
    return resultRange;
  }

  @Override
  public Range visit(CCharLiteralExpression e) throws UnrecognizedCCodeException {
    return new Range((long) e.getCharacter());
  }

  @Override
  public Range visit(CFloatLiteralExpression e) throws UnrecognizedCCodeException {
    // for a float literal, we create a range that cover this floating point
    BigDecimal value = e.getValue();
    return new Range(new CompInteger(value));
  }

  @Override
  public Range visit(CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
    BigInteger value = e.getValue();
    return new Range(new CompInteger(value));
  }

  @Override
  public Range visit(CStringLiteralExpression e) throws UnrecognizedCCodeException {
    // we cannot directly handle string literal (use ADCombinator instead)
    return Range.UNBOUND;
  }

  @Override
  public Range visit(CTypeIdExpression e) throws UnrecognizedCCodeException {
    // TypeId expression consists of an operator and a type identifier, such as {@code sizeof(int)}
    TypeIdOperator operator = e.getOperator();
    CType type = e.getType();
    // Type information can be derived by machine model
    switch (operator) {
      case SIZEOF:
        return new Range(model.getSizeof(type));
      case ALIGNOF:
        return new Range(model.getAlignof(type));
      default:
        throw new AssertionError("unknown typeid keyword: " + operator);
    }
  }

  @Override
  public Range visit(CUnaryExpression e) throws UnrecognizedCCodeException {
    Range range = e.getOperand().accept(this);
    switch (e.getOperator()) {
      case MINUS:
        return range.negate();
      case AMPER:
        // there is one case where the concrete address can be determined: expression with the
        // form &(((A*)0)->field)
        return handleOffsetOf(e.getOperand());
      case TILDE:
        CType eType = e.getExpressionType();
        return Ranges.getTypeRange(eType, model);
      case SIZEOF:
        CType type = e.getOperand().getExpressionType();
        return new Range(model.getSizeof(type));
      case ALIGNOF:
        CType type2 = e.getOperand().getExpressionType();
        return new Range(model.getAlignof(type2));
      default:
        throw new AssertionError("unknown unary operator" + e.getOperator());
    }
  }

  private Range handleOffsetOf(CExpression e) throws UnrecognizedCCodeException {
    // there are many uses of __builtin_offsetof() macros in real-world programs
    if (e instanceof CFieldReference) {
      if (((CFieldReference) e).isPointerDereference()) {
        String field = ((CFieldReference) e).getFieldName();
        CExpression owner = ((CFieldReference) e).getFieldOwner();
        if (owner instanceof CCastExpression) {
          CType cast = ((CCastExpression) owner).getCastType();
          CExpression likelyConst = ((CCastExpression) owner).getOperand();
          CPointerType type = Types.extractPointerType(cast);
          if (type != null) {
            CType coreType = type.getType();
            Long offset = Types.getFieldInfo(coreType, field, model).getFirst();
            if (offset != null) {
              Range likelySingletonRange = RangeState.evaluateRange(readableState, otherStates,
                  likelyConst, model);
              if (likelySingletonRange.isSingletonRange()) {
                return likelySingletonRange.plus(offset);
              }
            }
          }
        }
      }
    }
    return Range.UNBOUND;
  }

  @Override
  public Range visit(CPointerExpression e) throws UnrecognizedCCodeException {
    // NOTE: similarly, pointer dereference should not have overflow error
    Optional<AccessPath> accessPath = e.accept(new LeftHandAccessPathVisitor(this));
    Range resultRange;
    if (accessPath.isPresent()) {
      AccessPath actualPath = accessPath.get();
      resultRange = readableState.getRange(actualPath, model);
    } else {
      resultRange = Range.UNBOUND;
    }
    if (!forSummary && resultRange.isUnbound()) {
      resultRange = defaultRange(e);
    }
    return resultRange;
  }

  @Override
  public Range visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    // NOTE: if we return unbounded range, every function call will have overflow error, but this
    // is impossible, since the returned value has been converted (possibly after truncation).
    List<Range> argumentValues = new ArrayList<>();
    List<CExpression> arguments = pIastFunctionCallExpression.getParameterExpressions();
    for (CExpression argument : arguments) {
      Range range = argument.accept(this);
      argumentValues.add(range);
    }
    return RangeFunctionAdapter.instance(forSummary).evaluateFunctionCallExpression
        (pIastFunctionCallExpression, argumentValues, readableState, null).getResult();
  }

  @Override
  protected Range visitDefault(CExpression exp) throws UnrecognizedCCodeException {
    return Range.UNBOUND;
  }

  /**
   * If we cannot query the value of an expression from range mapping, we use type range instead
   * of unbound range to denote its possible values. In this way, our analysis reflects the
   * actual program execution more precisely.
   */
  private Range defaultRange(CExpression exp) {
    return Ranges.getTypeRange(exp, model);
  }

}
