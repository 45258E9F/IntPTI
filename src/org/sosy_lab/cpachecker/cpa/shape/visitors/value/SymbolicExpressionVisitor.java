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
package org.sosy_lab.cpachecker.cpa.shape.visitors.value;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapePointerAdapter;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapeValueAdapter;
import org.sosy_lab.cpachecker.cpa.shape.util.AssumeEvaluator;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.Symbolizer;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ShapeValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicExpressionAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ValueAndState;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Types;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A visitor very similar to
 * {@link org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.Symbolizer}. However, they have
 * some main differences:
 * (1) this visitor should act as symbolic value visitor, however it construct symbolic
 * expression for irreducible expressions. Symbolizer is designed specifically for handling
 * assumption expression.
 * (2) this visitor can reduce constant expressions.
 */
public class SymbolicExpressionVisitor
    extends DefaultCExpressionVisitor<List<SymbolicExpressionAndState>, CPATransferException>
    implements CRightHandSideVisitor<List<SymbolicExpressionAndState>, CPATransferException> {

  private final CFAEdge cfaEdge;
  private final ShapeState readableState;
  private final List<AbstractState> otherStates;
  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  public SymbolicExpressionVisitor(
      CFAEdge pEdge, ShapeState pState, List<AbstractState>
      pOtherStates, MachineModel pModel, LogManagerWithoutDuplicates pLogger) {
    cfaEdge = pEdge;
    readableState = pState;
    otherStates = pOtherStates;
    machineModel = pModel;
    logger = pLogger;
  }

  /* ************* */
  /* visit methods */
  /* ************* */

  @Override
  protected List<SymbolicExpressionAndState> visitDefault(CExpression exp)
      throws CPATransferException {
    SymbolicExpression unknownSe = SEs.toUnknown(exp);
    return Lists.newArrayList(SymbolicExpressionAndState.of(readableState, unknownSe));
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CArraySubscriptExpression e)
      throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CBinaryExpression e) throws CPATransferException {
    // If two operands are constants, we can reduce the symbolic expression, which could relieve
    // the burden of unnecessary explicit value derivation or SMT solving.
    CExpression operand1 = e.getOperand1();
    CExpression operand2 = e.getOperand2();
    BinaryOperator operator = e.getOperator();
    List<SymbolicExpressionAndState> results = new ArrayList<>();
    // Though this visitor is designed for evaluating non-address values, it is possible that the
    // operands are pointer values. For example, p1 > p2 is a non-pointer value while p1 and p2
    // are pointers.
    List<SymbolicExpressionAndState> seAndStates1 = CoreShapeAdapter.getInstance()
        .evaluateSymbolicExpression(readableState, otherStates, cfaEdge, operand1);
    for (SymbolicExpressionAndState seAndState1 : seAndStates1) {
      SymbolicExpression se1 = seAndState1.getObject();
      ShapeState newState = seAndState1.getShapeState();
      if (SEs.isUnknown(se1)) {
        results.add(SymbolicExpressionAndState.of(newState, SEs.toUnknown(e)));
        continue;
      }
      List<SymbolicExpressionAndState> seAndStates2 = CoreShapeAdapter.getInstance()
          .evaluateSymbolicExpression(newState, otherStates, cfaEdge, operand2);
      for (SymbolicExpressionAndState seAndState2 : seAndStates2) {
        SymbolicExpression se2 = seAndState2.getObject();
        newState = seAndState2.getShapeState();
        if (SEs.isUnknown(se2)) {
          results.add(SymbolicExpressionAndState.of(newState, SEs.toUnknown(e)));
          continue;
        }
        if (se1 instanceof ConstantSE && se2 instanceof ConstantSE) {
          ShapeValueAndState reducedValueAndState = handleBinaryOperation(newState, se1.getValue(),
              se2.getValue(), operator, e);
          newState = reducedValueAndState.getShapeState();
          ShapeValue reducedValue = reducedValueAndState.getObject();
          if (reducedValue != null) {
            results.add(SymbolicExpressionAndState.of(newState, new ConstantSE(reducedValue, e
                .getCalculationType(), e)));
            continue;
          }
        }
        // finally, we store the binary symbolic expression as a result
        results.add(SymbolicExpressionAndState.of(newState, new BinarySE(se1, se2, operator, e
            .getCalculationType(), e)));
      }
    }
    return results;
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CCastExpression e) throws CPATransferException {
    CExpression operand = e.getOperand();
    CType castType = e.getCastType();
    List<SymbolicExpressionAndState> results = new ArrayList<>();
    List<SymbolicExpressionAndState> seAndStates = CoreShapeAdapter.getInstance()
        .evaluateNonAddressValueAsSymbolicExpression(readableState, otherStates, cfaEdge, operand);
    for (SymbolicExpressionAndState seAndState : seAndStates) {
      SymbolicExpression se = seAndState.getObject();
      ShapeState newState = seAndState.getShapeState();
      if (SEs.isUnknown(se)) {
        results.add(SymbolicExpressionAndState.of(newState, SEs.toUnknown(e)));
        continue;
      }
      SymbolicExpression newSe;
      if (se instanceof CastSE) {
        // If the operand is a cast symbolic expression, then we attempt to simplify a series of
        // cast operations.
        newSe = simplifyCast((CastSE) se, castType, e, machineModel);
      } else if (se instanceof ConstantSE) {
        newSe = handleConstantCast((ConstantSE) se, castType, e);
      } else {
        newSe = new CastSE(se, castType, e);
      }
      results.add(SymbolicExpressionAndState.of(newState, newSe));
    }
    return results;
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CFieldReference e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CIdExpression e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CCharLiteralExpression e)
      throws CPATransferException {
    char ch = e.getCharacter();
    return Lists.newArrayList(SymbolicExpressionAndState.of(readableState, new ConstantSE(
        KnownExplicitValue.valueOf(ch), CNumericTypes.CHAR, e)));
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CFloatLiteralExpression e)
      throws CPATransferException {
    SymbolicExpression newSe;
    Value value = new NumericValue(e.getValue());
    CType type = CoreShapeAdapter.getType(e);
    value = AbstractExpressionValueVisitor.castCValue(value, type, machineModel, logger, e
        .getFileLocation());
    if (!value.isNumericValue()) {
      newSe = new ConstantSE(UnknownValue.getInstance(), type, e);
    } else {
      newSe = new ConstantSE(KnownExplicitValue.valueOf(((NumericValue) value).doubleValue()),
          type, e);
    }
    return Lists.newArrayList(SymbolicExpressionAndState.of(readableState, newSe));
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CIntegerLiteralExpression e)
      throws CPATransferException {
    SymbolicExpression newSe;
    Value value = new NumericValue(e.getValue());
    CType type = CoreShapeAdapter.getType(e);
    value = AbstractExpressionValueVisitor.castCValue(value, type, machineModel, logger, e
        .getFileLocation());
    if (!value.isNumericValue()) {
      newSe = new ConstantSE(UnknownValue.getInstance(), type, e);
    } else {
      newSe = new ConstantSE(KnownExplicitValue.valueOf(((NumericValue) value).longValue()),
          type, e);
    }
    return Lists.newArrayList(SymbolicExpressionAndState.of(readableState, newSe));
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CTypeIdExpression e) throws CPATransferException {
    CType type = e.getType();
    TypeIdOperator operator = e.getOperator();
    SymbolicExpression newSe;
    switch (operator) {
      case SIZEOF: {
        int length = CoreShapeAdapter.getInstance().evaluateSizeof(readableState, otherStates,
            cfaEdge, type);
        newSe = new ConstantSE(KnownExplicitValue.valueOf(length), CoreShapeAdapter.getType(e), e);
        break;
      }
      case ALIGNOF: {
        int alignof = machineModel.getAlignof(type);
        newSe = new ConstantSE(KnownExplicitValue.valueOf(alignof), CoreShapeAdapter.getType(e), e);
        break;
      }
      default:
        throw new IllegalArgumentException("Unsupported type-id operator: " + operator);
    }
    return Lists.newArrayList(SymbolicExpressionAndState.of(readableState, newSe));
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CUnaryExpression e) throws CPATransferException {
    CExpression operand = e.getOperand();
    UnaryOperator operator = e.getOperator();
    List<SymbolicExpressionAndState> results = new ArrayList<>();
    if (operator == UnaryOperator.AMPER) {
      // This case is impossible, since its type is T*.
      results.add(SymbolicExpressionAndState.of(readableState, SEs.toUnknown(e)));
      return results;
    } else if (operator == UnaryOperator.SIZEOF) {
      CType type = CoreShapeAdapter.getType(operand);
      int length = CoreShapeAdapter.getInstance().evaluateSizeof(readableState, otherStates,
          cfaEdge, type, e);
      results.add(SymbolicExpressionAndState.of(readableState, SEs.toConstant(KnownExplicitValue
          .valueOf(length), e)));
      return results;
    } else {
      // The operator can only be MINUS or TILDE, and the types of the unary expression and its
      // operand are consistent. Thus, the non-address visitor is still eligible for the operand.
      List<SymbolicExpressionAndState> seAndStates = CoreShapeAdapter.getInstance()
          .evaluateNonAddressValueAsSymbolicExpression(readableState, otherStates, cfaEdge,
              operand);
      for (SymbolicExpressionAndState seAndState : seAndStates) {
        SymbolicExpression se = seAndState.getObject();
        ShapeState newState = seAndState.getShapeState();
        if (SEs.isUnknown(se)) {
          results.add(SymbolicExpressionAndState.of(newState, se));
          continue;
        }
        SymbolicExpression newSe = handleUnaryOperation(se, operator, e);
        results.add(SymbolicExpressionAndState.of(newState, newSe));
      }
      return results;
    }
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CPointerExpression e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public List<SymbolicExpressionAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    List<ShapeState> states = new ArrayList<>();
    // This case is possible though rare. For example: POSIX open().
    if (ShapePointerAdapter.instance().isRegistered(pIastFunctionCallExpression)) {
      AddressValueAndStateList addressAndStates = ShapePointerAdapter.instance()
          .evaluateFunctionCallExpression(pIastFunctionCallExpression, readableState, otherStates,
              cfaEdge);
      states.addAll(FluentIterable.from(addressAndStates.asAddressAndStateList()).transform(
          new Function<AddressAndState, ShapeState>() {
            @Override
            public ShapeState apply(AddressAndState pAddressAndState) {
              return pAddressAndState.getShapeState();
            }
          }).toList());
    } else {
      states.add(readableState);
    }
    List<SymbolicExpressionAndState> results = new ArrayList<>();
    for (ShapeState state : states) {
      List<ValueAndState> expValueAndStates = ShapeValueAdapter.instance()
          .evaluateFunctionCallExpression(pIastFunctionCallExpression, state, otherStates, cfaEdge);
      for (ValueAndState expValueAndState : expValueAndStates) {
        ShapeState newState = expValueAndState.getShapeState();
        Value value = expValueAndState.getObject();
        if (value.isNumericValue()) {
          KnownExplicitValue expValue = KnownExplicitValue.of(value.asNumericValue().getNumber());
          results.add(SymbolicExpressionAndState.of(newState, SEs.toConstant(expValue,
              pIastFunctionCallExpression)));
        } else {
          results.add(SymbolicExpressionAndState.of(newState,
              SEs.toUnknown(pIastFunctionCallExpression)));
        }
      }
    }
    return results;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private List<SymbolicExpressionAndState> handleLeftHandSide(CLeftHandSide e)
      throws CPATransferException {
    SymbolicValueAndStateList results = CoreShapeAdapter.getInstance().evaluateSymbolicValue
        (readableState, otherStates, cfaEdge, e);
    List<SymbolicExpressionAndState> seAndStates = new ArrayList<>(results.size());
    for (SymbolicValueAndState result : results.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue value = result.getObject();
      ShapeState newState = result.getShapeState();
      ShapeExplicitValue expValue = getExplicitValue(newState, value);
      CType type = CoreShapeAdapter.getType(e);
      SymbolicExpression se;
      if (!expValue.isUnknown() && Types.isNumericalType(type)) {
        se = SEs.toConstant(expValue, e);
      } else {
        se = SEs.toConstant(value, e);
      }
      seAndStates.add(SymbolicExpressionAndState.of(newState, se));
    }
    return seAndStates;
  }

  private ShapeExplicitValue getExplicitValue(ShapeState pState, ShapeSymbolicValue pValue) {
    if (pValue.isUnknown()) {
      return UnknownValue.getInstance();
    } else if (pValue.equals(KnownSymbolicValue.ZERO)) {
      return KnownExplicitValue.ZERO;
    } else if (pValue.equals(KnownSymbolicValue.TRUE)) {
      return KnownExplicitValue.ONE;
    } else {
      return pState.getExplicit((KnownSymbolicValue) pValue);
    }
  }

  /* ***************** */
  /* binary expression */
  /* ***************** */

  private ShapeValueAndState handleBinaryOperation(
      ShapeState pState, ShapeValue pV1,
      ShapeValue pV2, BinaryOperator pOp,
      CBinaryExpression pOrigExp) {
    assert (!pV1.isUnknown());
    assert (!pV2.isUnknown());
    boolean isSymbolic1 = pV1 instanceof KnownSymbolicValue;
    boolean isSymbolic2 = pV2 instanceof KnownSymbolicValue;
    if (isSymbolic1 == isSymbolic2) {
      if (!isSymbolic1) {
        // two values are explicits
        Value v1 = new NumericValue(pV1.getValue());
        Value v2 = new NumericValue(pV2.getValue());
        Value result = AbstractExpressionValueVisitor.calculateBinaryOperation(v1, v2, pOrigExp,
            machineModel, logger);
        if (!result.isNumericValue()) {
          // this can be reached when, for example, divided-by-zero error occurs
          return ShapeValueAndState.of(pState);
        }
        if (pOp.isLogicalOperator()) {
          KnownExplicitValue expValue = CoreShapeAdapter.getInstance().convertFromValue(result
              .asNumericValue(), CNumericTypes.INT);
          return ShapeValueAndState.of(pState, expValue);
        }
        CType type = CoreShapeAdapter.getType(pOrigExp.getCalculationType());
        CSimpleType convertedType;
        if (type instanceof CSimpleType) {
          convertedType = (CSimpleType) type;
        } else {
          convertedType = CNumericTypes.INT;
        }
        KnownExplicitValue expValue;
        expValue = CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
            convertedType);
        return ShapeValueAndState.of(pState, expValue);
      } else {
        // two values are symbolic values
        ShapeValue result = evaluateBinaryOperation(pState, (KnownSymbolicValue) pV1,
            (KnownSymbolicValue) pV2, pOp, pOrigExp.getCalculationType());
        return ShapeValueAndState.of(pState, result);
      }
    } else {
      KnownSymbolicValue lv;
      KnownExplicitValue rv;
      boolean isReversed;
      if (isSymbolic1) {
        lv = (KnownSymbolicValue) pV1;
        rv = (KnownExplicitValue) pV2;
        isReversed = false;
      } else {
        lv = (KnownSymbolicValue) pV2;
        rv = (KnownExplicitValue) pV1;
        isReversed = true;
      }
      ShapeValue evaluation;
      if (pOp.isLogicalOperator()) {
        evaluation = Symbolizer.evaluateBinaryComparison(lv, rv, pOrigExp, isReversed, machineModel,
            logger);
      } else {
        evaluation = evaluateBinaryNonComparison(pState, lv, rv, pOrigExp, isReversed);
      }
      if (evaluation != null) {
        return ShapeValueAndState.of(pState, evaluation);
      } else {
        // pointer arithmetic
        AddressValueAndState result = Symbolizer.evaluatePointerArithmetic(pState, lv, rv, pOp,
            isReversed);
        ShapeAddressValue value = result.getObject();
        return ShapeValueAndState.of(result.getShapeState(), value.isUnknown() ? null : value);
      }
    }
  }

  @Nullable
  private ShapeValue evaluateBinaryOperation(
      ShapeState pState, KnownSymbolicValue v1,
      KnownSymbolicValue v2, BinaryOperator operator,
      CType targetType) {
    if (operator.isLogicalOperator()) {
      AssumeEvaluator evaluator = CoreShapeAdapter.getInstance().getAssumeEvaluator();
      return evaluator.evaluateAssumption(pState, v1, v2, operator);
    } else {
      boolean isZero;
      switch (operator) {
        case MINUS: {
          Value result = ExplicitValueVisitor.evaluatePointerDifference(pState, v1, v2);
          if (result.isNumericValue()) {
            return CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
                (CSimpleType) targetType);
          } else {
            return v1.equals(v2) ? KnownExplicitValue.ZERO : null;
          }
        }
        case LESS_EQUAL:
        case LESS_THAN:
        case GREATER_EQUAL:
        case GREATER_THAN:
        case EQUALS:
        case NOT_EQUALS: {
          AssumeEvaluator evaluator = CoreShapeAdapter.getInstance().getAssumeEvaluator();
          return evaluator.evaluateAssumption(pState, v1, v2, operator);
        }
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          isZero = v1.equals(KnownSymbolicValue.ZERO) && v2.equals(KnownSymbolicValue.ZERO);
          return isZero ? KnownSymbolicValue.ZERO : null;
        case MODULO:
          isZero = v1.equals(v2);
          return isZero ? KnownSymbolicValue.ZERO : null;
        case DIVIDE:
          if (v2.equals(KnownSymbolicValue.ZERO)) {
            // divided-by-zero error, but for now we just tolerate it in analysis
            return UnknownValue.getInstance();
          }
          isZero = v1.equals(KnownSymbolicValue.ZERO);
          return isZero ? KnownSymbolicValue.ZERO : null;
        case MULTIPLY:
        case BINARY_AND:
          isZero = v1.equals(KnownSymbolicValue.ZERO) || v2.equals(KnownSymbolicValue.ZERO);
          return isZero ? KnownSymbolicValue.ZERO : null;
        default:
          return null;
      }
    }
  }

  @Nullable
  private ShapeValue evaluateBinaryNonComparison(
      ShapeState pState, KnownSymbolicValue valueOne,
      KnownExplicitValue valueTwo,
      CBinaryExpression binExp,
      boolean isReversed) {
    // If the reverse flag is not set, then the expression should be valueOne * valueTwo where *
    // is the binary operator; otherwise the expression should be valueTwo * valueOne.
    KnownExplicitValue expOne = null;
    KnownSymbolicValue symTwo = null;
    if (valueOne.equals(KnownSymbolicValue.ZERO)) {
      expOne = KnownExplicitValue.ZERO;
    } else if (valueOne.equals(KnownSymbolicValue.TRUE)) {
      expOne = KnownExplicitValue.ONE;
    }
    if (expOne != null) {
      Value val1 = new NumericValue(expOne.getValue());
      Value val2 = new NumericValue(valueTwo.getValue());
      Value result;
      if (!isReversed) {
        result = AbstractExpressionValueVisitor.calculateBinaryOperation(val1, val2, binExp,
            machineModel, logger);
      } else {
        result = AbstractExpressionValueVisitor.calculateBinaryOperation(val2, val1, binExp,
            machineModel, logger);
      }
      if (!result.isNumericValue()) {
        return UnknownValue.getInstance();
      }
      if (binExp.getOperator().isLogicalOperator()) {
        return CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
            CNumericTypes.INT);
      }
      CType type = CoreShapeAdapter.getType(binExp.getCalculationType());
      assert (type instanceof CSimpleType);
      return CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
          (CSimpleType) type);
    }
    if (valueTwo.equals(KnownExplicitValue.ONE)) {
      // MULTIPLY: left or right
      // DIVIDE: right
      // MODULO: right
      BinaryOperator operator = binExp.getOperator();
      switch (operator) {
        case MULTIPLY:
          return valueOne;
        case DIVIDE:
          if (!isReversed) {
            return valueOne;
          }
        case MODULO:
          if (!isReversed) {
            return KnownExplicitValue.ZERO;
          }
      }
    } else if (valueTwo.equals(KnownExplicitValue.ZERO)) {
      symTwo = KnownSymbolicValue.ZERO;
    }
    if (symTwo != null) {
      if (isReversed) {
        return evaluateBinaryOperation(pState, symTwo, valueOne, binExp.getOperator(), binExp
            .getCalculationType());
      } else {
        return evaluateBinaryOperation(pState, valueOne, symTwo, binExp.getOperator(), binExp
            .getCalculationType());
      }
    }
    return null;
  }

  /* *************** */
  /* cast expression */
  /* *************** */

  /**
   * Simplify compound cast expression. Given a cast expression (T1)(T2)e where the type of e is
   * T and T2 holds all the values of T, then (T1)(T2)e can be simplified as (T1)e.
   *
   * @param pSe      a cast expression
   * @param castType the cast type
   * @param pOrigExp the original expression
   * @return a new cast expression that casts the given symbolic expression to the given cast type
   */
  public static SymbolicExpression simplifyCast(
      CastSE pSe, CType castType, CExpression pOrigExp,
      MachineModel pModel) {
    CType innerCast = pSe.getType();
    if (CoreShapeAdapter.isCompatible(innerCast, castType)) {
      // (T)(T)e is equivalent with (T)e
      return pSe;
    }
    SymbolicExpression innerOp = pSe.getOperand();
    CType innerType = innerOp.getType();
    if (Types.canHoldAllValues(innerCast, innerType, pModel)) {
      return new CastSE(innerOp, castType, pOrigExp);
    }
    return new CastSE(pSe, castType, pOrigExp);
  }

  private SymbolicExpression handleConstantCast(
      ConstantSE pSe, CType castType, CExpression
      pOrigExp) {
    ShapeValue value = pSe.getValue();
    CType cType = CoreShapeAdapter.getType(castType);
    assert (!value.isUnknown());
    if (value instanceof KnownExplicitValue) {
      NumericValue nv = new NumericValue(value.getValue());
      Value casted = AbstractExpressionValueVisitor.castCValue(nv, cType, machineModel,
          logger, pOrigExp.getFileLocation());
      if (casted.isNumericValue()) {
        if (cType instanceof CSimpleType) {
          KnownExplicitValue newValue = CoreShapeAdapter.getInstance().convertFromValue(casted
              .asNumericValue(), (CSimpleType) cType);
          return SEs.toConstant(newValue, pOrigExp);
        }
        return SEs.toConstant(KnownExplicitValue.of(casted.asNumericValue().getNumber()), pOrigExp);
      }
    }
    // for other cases, we directly append the casted type to the given symbolic expression
    return new CastSE(pSe, castType, pOrigExp);
  }

  /* **************** */
  /* unary expression */
  /* **************** */

  private SymbolicExpression handleUnaryOperation(
      SymbolicExpression pSe, UnaryOperator pOp,
      CExpression pOrigExp) {
    if (pSe instanceof ConstantSE) {
      ShapeValue value = pSe.getValue();
      assert (!value.isUnknown());
      if (value instanceof KnownSymbolicValue) {
        if (pOp == UnaryOperator.MINUS) {
          if (value.equals(KnownSymbolicValue.ZERO)) {
            return pSe;
          }
        }
      } else {
        KnownExplicitValue ev = (KnownExplicitValue) value;
        CType type = CoreShapeAdapter.getType(pOrigExp);
        CSimpleType convertedType;
        if (type instanceof CSimpleType) {
          convertedType = (CSimpleType) type;
        } else {
          convertedType = CNumericTypes.INT;
        }
        if (pOp == UnaryOperator.MINUS) {
          NumericValue nv = new NumericValue(ev.getValue());
          nv = nv.negate();
          KnownExplicitValue newEv = CoreShapeAdapter.getInstance().convertFromValue(nv,
              convertedType);
          return SEs.toConstant(newEv, pOrigExp);
        } else if (pOp == UnaryOperator.TILDE) {
          NumericValue nv = new NumericValue(~ev.getValue().longValue());
          KnownExplicitValue newEv = CoreShapeAdapter.getInstance().convertFromValue(nv,
              convertedType);
          return SEs.toConstant(newEv, pOrigExp);
        }
      }
    }
    return new UnarySE(pSe, pOp, CoreShapeAdapter.getType(pOrigExp), pOrigExp);
  }

}
