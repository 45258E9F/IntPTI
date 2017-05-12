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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.AssumeEvaluator;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.ExplicitValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Types;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Converter from C expression to symbolic expression.
 */
public class Symbolizer
    extends DefaultCExpressionVisitor<SymbolicAssumeInfo, CPATransferException>
    implements CExpressionVisitor<SymbolicAssumeInfo, CPATransferException> {

  private ShapeState readableState;
  private final List<AbstractState> otherStates;
  private final CFAEdge cfaEdge;
  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  /**
   * A status indicator, determines that which mode the visitor is working on.
   */
  private final boolean isEager;

  /**
   * An indicator that represent the level of current analysis.
   * When handling topmost level of assumption, we should store some derived information for
   * improving the precision.
   */
  private final boolean isTop;

  private AssumeEvaluator evaluator;

  public Symbolizer(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      MachineModel pModel, LogManagerWithoutDuplicates pLogger, boolean pTop,
      boolean pEager) {
    readableState = pState;
    otherStates = pOtherStates;
    cfaEdge = pEdge;
    machineModel = pModel;
    logger = pLogger;
    isEager = pEager;
    isTop = pTop;
  }

  /* *************** */
  /* override method */
  /* *************** */

  @Override
  protected SymbolicAssumeInfo visitDefault(CExpression exp) throws CPATransferException {
    ConstantSE se = new ConstantSE(UnknownValue.getInstance(), exp.getExpressionType(), exp);
    return SymbolicAssumeInfo.of(se, readableState);
  }

  @Override
  public SymbolicAssumeInfo visit(CArraySubscriptExpression e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public SymbolicAssumeInfo visit(CBinaryExpression e) throws CPATransferException {
    CExpression operand1 = e.getOperand1();
    CExpression operand2 = e.getOperand2();
    BinaryOperator operator = e.getOperator();
    SymbolicAssumeInfo result = SymbolicAssumeInfo.of();
    SymbolicAssumeInfo info1 = CoreShapeAdapter.getInstance().symbolizeAssumption(readableState,
        otherStates, cfaEdge, operand1, false, isEager);
    for (int i = 0; i < info1.size(); i++) {
      ShapeState newState = info1.getState(i);
      SymbolicExpression sop1 = info1.getSymbolicExpression(i);
      SymbolicAssumeInfo info2 = CoreShapeAdapter.getInstance().symbolizeAssumption(newState,
          otherStates, cfaEdge, operand2, false, isEager);
      for (int j = 0; j < info2.size(); j++) {
        newState = info2.getState(j);
        SymbolicExpression sop2 = info2.getSymbolicExpression(j);
        // generate the new symbolic expression
        SymbolicExpression se;
        if (sop1 instanceof ConstantSE && sop2 instanceof ConstantSE) {
          ShapeValue mergedValue = handleBinaryOperation(sop1.getValue(), sop2.getValue(),
              operator, e);
          if (mergedValue != null) {
            se = new ConstantSE(mergedValue, e.getCalculationType(), e);
            result.add(se, evaluator, newState);
            continue;
          }
        }
        se = new BinarySE(sop1, sop2, operator, e.getCalculationType(), e);
        result.add(se, evaluator, newState);
      }
    }
    return result;
  }

  @Override
  public SymbolicAssumeInfo visit(CCastExpression e) throws CPATransferException {
    CExpression operand = e.getOperand();
    CType castType = e.getCastType();
    SymbolicAssumeInfo result = SymbolicAssumeInfo.of();
    SymbolicAssumeInfo info = CoreShapeAdapter.getInstance().symbolizeAssumption(readableState,
        otherStates, cfaEdge, operand, isTop, isEager);
    for (int i = 0; i < info.size(); i++) {
      ShapeState newState = info.getState(i);
      SymbolicExpression sop = info.getSymbolicExpression(i);
      SymbolicExpression se;
      if (sop instanceof ConstantSE) {
        ShapeValue mergedValue = handleCastOperation(sop.getValue(), castType, e);
        if (mergedValue != null) {
          se = new ConstantSE(mergedValue, castType, e);
          result.add(se, newState);
          continue;
        }
      }
      se = new CastSE(sop, castType, e);
      result.add(se, newState);
    }
    return result;
  }

  @Override
  public SymbolicAssumeInfo visit(CFieldReference e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public SymbolicAssumeInfo visit(CIdExpression e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  @Override
  public SymbolicAssumeInfo visit(CCharLiteralExpression e) throws CPATransferException {
    char ch = e.getCharacter();
    ConstantSE se = new ConstantSE(KnownExplicitValue.valueOf(ch), CNumericTypes.CHAR, e);
    return SymbolicAssumeInfo.of(se, readableState);
  }

  @Override
  public SymbolicAssumeInfo visit(CFloatLiteralExpression e) throws CPATransferException {
    Value value = new NumericValue(e.getValue());
    CType type = CoreShapeAdapter.getType(e);
    value = AbstractExpressionValueVisitor.castCValue(value, type, machineModel, logger, e
        .getFileLocation());
    SymbolicExpression se;
    if (!value.isNumericValue()) {
      se = new ConstantSE(UnknownValue.getInstance(), type, e);
    } else {
      se = new ConstantSE(KnownExplicitValue.valueOf(((NumericValue) value).doubleValue()), type,
          e);
    }
    return SymbolicAssumeInfo.of(se, readableState);
  }

  @Override
  public SymbolicAssumeInfo visit(CIntegerLiteralExpression e) throws CPATransferException {
    Value value = new NumericValue(e.getValue());
    CType type = CoreShapeAdapter.getType(e);
    value = AbstractExpressionValueVisitor.castCValue(value, type, machineModel, logger, e
        .getFileLocation());
    SymbolicExpression se;
    if (!value.isNumericValue()) {
      se = new ConstantSE(UnknownValue.getInstance(), type, e);
    } else {
      se = new ConstantSE(KnownExplicitValue.valueOf(((NumericValue) value).longValue()), type, e);
    }
    return SymbolicAssumeInfo.of(se, readableState);
  }

  @Override
  public SymbolicAssumeInfo visit(CStringLiteralExpression e) throws CPATransferException {
    SymbolicAssumeInfo result = SymbolicAssumeInfo.of();
    SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().evaluateSymbolicValue
        (readableState, otherStates, cfaEdge, e);
    for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
      ConstantSE se = new ConstantSE(valueAndState.getObject(), e.getExpressionType(), e);
      result.add(se, valueAndState.getShapeState());
    }
    return result;
  }

  @Override
  public SymbolicAssumeInfo visit(CTypeIdExpression e) throws CPATransferException {
    CType type = e.getType();
    TypeIdOperator operator = e.getOperator();
    SymbolicExpression se;
    switch (operator) {
      case SIZEOF:
        int length = machineModel.getSizeof(type);
        se = new ConstantSE(KnownExplicitValue.valueOf(length), CoreShapeAdapter.getType(e), e);
        break;
      case ALIGNOF:
        int alignof = machineModel.getAlignof(type);
        se = new ConstantSE(KnownExplicitValue.valueOf(alignof), CoreShapeAdapter.getType(e), e);
        break;
      default:
        throw new IllegalArgumentException("unsupported type-id operator" + operator);
    }
    return SymbolicAssumeInfo.of(se, readableState);
  }

  @Override
  public SymbolicAssumeInfo visit(CUnaryExpression e) throws CPATransferException {
    CExpression operand = e.getOperand();
    UnaryOperator operator = e.getOperator();
    boolean eager = isEager;
    if (operator == UnaryOperator.AMPER) {
      if (CoreShapeAdapter.getType(operand) instanceof CFunctionType && operand instanceof
          CIdExpression) {
        CIdExpression functionId = (CIdExpression) operand;
        CFunctionDeclaration function = (CFunctionDeclaration) functionId.getDeclaration();
        SGObject object = readableState.getFunctionObject(function);
        if (object == null) {
          object = readableState.createFunctionObject(function);
        }
        AddressValueAndState functionAddress = CoreShapeAdapter.getInstance().createAddress
            (readableState, object, KnownExplicitValue.ZERO);
        readableState = functionAddress.getShapeState();
        ShapeAddressValue addressValue = functionAddress.getObject();
        ConstantSE constSe = new ConstantSE(addressValue, CoreShapeAdapter.getType(e), e);
        return SymbolicAssumeInfo.of(constSe, readableState);
      }
      eager = false;
    }
    SymbolicAssumeInfo result = SymbolicAssumeInfo.of();
    SymbolicAssumeInfo info = CoreShapeAdapter.getInstance().symbolizeAssumption(readableState,
        otherStates, cfaEdge, operand, isTop, eager);
    for (int i = 0; i < info.size(); i++) {
      ShapeState newState = info.getState(i);
      SymbolicExpression sop = info.getSymbolicExpression(i);
      SymbolicExpression se;
      if (sop instanceof ConstantSE) {
        ShapeValue mergedValue = handleUnaryOperation(sop.getValue(), operator, e);
        if (mergedValue != null) {
          se = new ConstantSE(mergedValue, CoreShapeAdapter.getType(e), e);
          result.add(se, newState);
          continue;
        }
      }
      se = new UnarySE(sop, operator, CoreShapeAdapter.getType(e), e);
      result.add(se, newState);
    }
    return result;
  }

  @Override
  public SymbolicAssumeInfo visit(CPointerExpression e) throws CPATransferException {
    return handleLeftHandSide(e);
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private SymbolicAssumeInfo handleLeftHandSide(CLeftHandSide e) throws CPATransferException {
    SymbolicValueAndStateList results = CoreShapeAdapter.getInstance().evaluateSymbolicValue
        (readableState, otherStates, cfaEdge, e);
    SymbolicAssumeInfo result = SymbolicAssumeInfo.of();
    for (SymbolicValueAndState valueAndState : results.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue value = valueAndState.getObject();
      ShapeState state = valueAndState.getShapeState();
      ConstantSE se;
      if (!value.isUnknown() && isEager) {
        ShapeExplicitValue expValue = state.getExplicit((KnownSymbolicValue) value);
        CType type = CoreShapeAdapter.getType(e);
        if (!expValue.isUnknown() && Types.isNumericalType(type)) {
          se = new ConstantSE(expValue, CoreShapeAdapter.getType(e), e);
          result.add(se, state);
          continue;
        }
      }
      se = new ConstantSE(value, CoreShapeAdapter.getType(e), e);
      result.add(se, state);
    }
    return result;
  }

  /**
   * Handle arithmetic of two shape values.
   * There are totally 3 cases to be handled:
   * (1) both operands are explicit;
   * (2) one operand is explicit while the other is symbolic
   * (3) either of operands is unknown
   */
  @Nullable
  private ShapeValue handleBinaryOperation(
      ShapeValue valueOne, ShapeValue valueTwo,
      BinaryOperator operator, CBinaryExpression e)
      throws CPATransferException {
    if (valueOne.isUnknown() || valueTwo.isUnknown()) {
      return UnknownValue.getInstance();
    }
    if (valueOne instanceof KnownExplicitValue && valueTwo instanceof KnownExplicitValue) {
      Value val1 = new NumericValue(valueOne.getValue());
      Value val2 = new NumericValue(valueTwo.getValue());
      Value result = AbstractExpressionValueVisitor.calculateBinaryOperation(val1, val2, e,
          machineModel, logger);
      if (!result.isNumericValue()) {
        // possibly reached when divided-by-zero bug occurs
        return UnknownValue.getInstance();
      }
      if (operator.isLogicalOperator()) {
        return CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
            CNumericTypes.INT);
      }
      CType type = CoreShapeAdapter.getType(e.getCalculationType());
      CSimpleType convertedType;
      if (type instanceof CSimpleType) {
        convertedType = (CSimpleType) type;
      } else {
        convertedType = CNumericTypes.INT;
      }
      return CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
          convertedType);
    }
    boolean isSymbolicOne = valueOne instanceof KnownSymbolicValue;
    boolean isSymbolicTwo = valueTwo instanceof KnownSymbolicValue;
    if (isSymbolicOne == isSymbolicTwo) {
      // two symbolic values, they can be compared
      Preconditions.checkArgument(isSymbolicOne);
      return evaluateSymbolicArithmetic((KnownSymbolicValue) valueOne, (KnownSymbolicValue)
          valueTwo, operator, e.getCalculationType());
    } else {
      KnownSymbolicValue lValue;
      KnownExplicitValue rValue;
      boolean isReversed;
      if (isSymbolicOne) {
        lValue = (KnownSymbolicValue) valueOne;
        rValue = (KnownExplicitValue) valueTwo;
        isReversed = false;
      } else {
        // the second is symbolic while the first is explicit
        lValue = (KnownSymbolicValue) valueTwo;
        assert (valueOne instanceof KnownExplicitValue);
        rValue = (KnownExplicitValue) valueOne;
        isReversed = true;
      }
      ShapeValue result;
      if (operator.isLogicalOperator()) {
        // handle some comparison cases
        result = evaluateBinaryComparison(lValue, rValue, e, isReversed, machineModel, logger);
      } else {
        result = evaluateBinaryNonComparison(lValue, rValue, e, isReversed);
      }
      if (result != null) {
        return result;
      } else {
        AddressValueAndState valueAndState = evaluatePointerArithmetic(readableState, lValue,
            rValue, operator, isReversed);
        readableState = valueAndState.getShapeState();
        ShapeAddressValue value = valueAndState.getObject();
        return value.isUnknown() ? null : value;
      }
    }
  }

  @Nullable
  private ShapeValue evaluateSymbolicArithmetic(
      KnownSymbolicValue valueOne,
      KnownSymbolicValue valueTwo,
      BinaryOperator operator, CType targetType) {
    switch (operator) {
      case MINUS: {
        Value result = ExplicitValueVisitor.evaluatePointerDifference(readableState, valueOne,
            valueTwo);
        if (result.isNumericValue()) {
          Preconditions.checkArgument(targetType instanceof CSimpleType);
          return CoreShapeAdapter.getInstance().convertFromValue(result.asNumericValue(),
              (CSimpleType) targetType);
        } else {
          boolean isZero = valueOne.equals(valueTwo);
          return isZero ? KnownExplicitValue.ZERO : null;
        }
      }
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case EQUALS:
      case NOT_EQUALS: {
        // logical operations
        AssumeEvaluator evaluator = CoreShapeAdapter.getInstance().getAssumeEvaluator();
        ShapeSymbolicValue result = evaluator.evaluateAssumption(readableState, valueOne,
            valueTwo, operator);
        if (isTop) {
          this.evaluator = evaluator;
        }
        return result;
      }
      case PLUS:
      case SHIFT_LEFT:
      case BINARY_OR:
      case BINARY_XOR:
      case SHIFT_RIGHT: {
        boolean isZero = valueOne.equals(KnownSymbolicValue.ZERO) && valueTwo.equals
            (KnownSymbolicValue.ZERO);
        return isZero ? KnownSymbolicValue.ZERO : null;
      }
      case MODULO: {
        boolean isZero = valueOne.equals(valueTwo);
        return isZero ? KnownSymbolicValue.ZERO : null;
      }
      case DIVIDE: {
        if (valueTwo.equals(KnownSymbolicValue.ZERO)) {
          // divided-by-zero error, should we mark it as error here?
          return UnknownValue.getInstance();
        }
        boolean isZero = valueOne.equals(KnownSymbolicValue.ZERO);
        return isZero ? KnownSymbolicValue.ZERO : null;
      }
      case MULTIPLY:
      case BINARY_AND: {
        boolean isZero = valueOne.equals(KnownSymbolicValue.ZERO) || valueTwo.equals
            (KnownSymbolicValue.ZERO);
        return isZero ? KnownSymbolicValue.ZERO : null;
      }
      default:
        // we could not reach here
        return null;
    }
  }

  /**
   * Perform comparison between a symbolic value and an explicit value.
   */
  @Nullable
  public static KnownSymbolicValue evaluateBinaryComparison(
      KnownSymbolicValue valueOne,
      KnownExplicitValue valueTwo,
      CBinaryExpression binExp,
      boolean isReversed,
      MachineModel pModel,
      LogManagerWithoutDuplicates pLogger) {
    BinaryOperator operator = binExp.getOperator();
    if (!operator.isLogicalOperator()) {
      // this method handles comparison only
      return null;
    }
    if (isReversed) {
      operator = operator.getOppositeLogicalOperator();
    }
    if (valueTwo.equals(KnownExplicitValue.ZERO)) {
      KnownSymbolicValue symTwo = KnownSymbolicValue.ZERO;
      ShapeSymbolicValue result = SymbolicMerger.handleBinaryOperation(valueOne, symTwo, operator);
      return result.isUnknown() ? null : (KnownSymbolicValue) result;
    }
    // if we reach here, the second operand should be a non-zero explicit value
    KnownExplicitValue expOne = null;
    if (valueOne.equals(KnownSymbolicValue.TRUE)) {
      expOne = KnownExplicitValue.ONE;
    } else if (valueOne.equals(KnownSymbolicValue.FALSE)) {
      // in C language, FALSE is equivalent with integer 0
      expOne = KnownExplicitValue.ZERO;
    }
    if (expOne != null) {
      Value val1 = new NumericValue(expOne.getValue());
      Value val2 = new NumericValue(valueTwo.getValue());
      Value result = AbstractExpressionValueVisitor.calculateBinaryOperation(val1, val2, binExp,
          pModel, pLogger);
      if (result.isNumericValue()) {
        return result.asNumericValue().isNull() ? KnownSymbolicValue.FALSE : KnownSymbolicValue
            .TRUE;
      } else {
        throw new IllegalStateException("comparison of two explicit values should have explicit "
            + "result");
      }
    }
    // we cannot derive a known comparison result
    return null;
  }

  /**
   * Perform non-comparative operation on mixed symbolic/explicit value.
   */
  @Nullable
  private ShapeValue evaluateBinaryNonComparison(
      KnownSymbolicValue valueOne,
      KnownExplicitValue valueTwo,
      CBinaryExpression binExp,
      boolean isReversed)
      throws CPATransferException {
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
      if (isReversed) {
        return handleBinaryOperation(valueTwo, expOne, binExp.getOperator(), binExp);
      } else {
        return handleBinaryOperation(expOne, valueTwo, binExp.getOperator(), binExp);
      }
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
        return evaluateSymbolicArithmetic(symTwo, valueOne, binExp.getOperator(), binExp
            .getCalculationType());
      } else {
        return evaluateSymbolicArithmetic(valueOne, symTwo, binExp.getOperator(), binExp
            .getCalculationType());
      }
    }
    return null;
  }

  /**
   * Pointer arithmetic between a pointer value and a scalar value.
   * The result is still a pointer value.
   */
  public static AddressValueAndState evaluatePointerArithmetic(
      ShapeState pState,
      KnownSymbolicValue valueOne,
      KnownExplicitValue valueTwo,
      BinaryOperator operator,
      boolean isReversed) {
    // STEP 1: derive address value from symbolic value
    ShapeAddressValue address;
    if (valueOne instanceof KnownAddressValue) {
      address = (KnownAddressValue) valueOne;
    } else {
      if (pState.isAddress(valueOne.getAsLong())) {
        address = pState.getPointToForAddressValue(valueOne.getAsLong());
      } else {
        address = UnknownValue.getInstance();
      }
    }
    if (address.isUnknown()) {
      return AddressValueAndState.of(pState);
    }
    SGObject targetObject = address.getObject();
    ShapeExplicitValue innerOffset = address.getOffset();
    switch (operator) {
      case PLUS: {
        return CoreShapeAdapter.getInstance().createAddress(pState, targetObject,
            innerOffset.add(valueTwo));
      }
      case MINUS: {
        if (!isReversed) {
          return CoreShapeAdapter.getInstance().createAddress(pState, targetObject,
              innerOffset.subtract(valueTwo));
        } else {
          // -address + offset: undefined
          return AddressValueAndState.of(pState);
        }
      }
      default:
        // address * offset: undefined
        return AddressValueAndState.of(pState);
    }
  }

  /**
   * Handle cast operation
   */
  private ShapeValue handleCastOperation(ShapeValue pValue, CType castType, CCastExpression e)
      throws CPATransferException {
    if (pValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    if (pValue instanceof KnownSymbolicValue) {
      // we should model casting in the SMT formula
      return null;
    } else {
      assert (pValue instanceof KnownExplicitValue);
      NumericValue value = new NumericValue(pValue.getValue());
      Value casted = AbstractExpressionValueVisitor.castCValue(value, castType, machineModel,
          logger, e.getFileLocation());
      if (casted.isNumericValue()) {
        CType type = CoreShapeAdapter.getType(castType);
        if (type instanceof CSimpleType) {
          return CoreShapeAdapter.getInstance().convertFromValue(casted.asNumericValue(),
              (CSimpleType) type);
        }
        // for pointer values, we do not change its value during cast
        return KnownExplicitValue.of(casted.asNumericValue().getNumber());
      } else {
        return UnknownValue.getInstance();
      }
    }
  }

  /**
   * Handle arithmetic of one operand.
   */
  @Nullable
  private ShapeValue handleUnaryOperation(
      ShapeValue pValue, UnaryOperator operator,
      CUnaryExpression e) throws CPATransferException {
    if (pValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    if (pValue instanceof KnownSymbolicValue) {
      KnownSymbolicValue sValue = (KnownSymbolicValue) pValue;
      switch (operator) {
        case AMPER: {
          // (1) the operand of &e (i.e. e) should be a l-value
          // (2) we should not write address-of in the SMT formula
          SGHasValueEdgeFilter filter = new SGHasValueEdgeFilter();
          filter = filter.filterHavingValue(pValue.getAsLong());
          Set<SGHasValueEdge> edges = readableState.getHasValueEdgesFor(filter);
          if (edges.isEmpty()) {
            return UnknownValue.getInstance();
          }
          SGHasValueEdge edge = edges.iterator().next();
          SGObject object = edge.getObject();
          int offset = edge.getOffset();
          AddressValueAndState result = CoreShapeAdapter.getInstance().createAddress
              (readableState, object, KnownExplicitValue.valueOf(offset));
          readableState = result.getShapeState();
          return result.getObject();
        }
        case MINUS: {
          return sValue.equals(KnownSymbolicValue.ZERO) ? sValue : null;
        }
        case SIZEOF: {
          CType type = CoreShapeAdapter.getType(e.getOperand());
          return KnownExplicitValue.valueOf(machineModel.getSizeof(type));
        }
        case TILDE: {
          return null;
        }
        default:
          return null;
      }
    } else {
      assert (pValue instanceof KnownExplicitValue);
      KnownExplicitValue expValue = (KnownExplicitValue) pValue;
      CType type = CoreShapeAdapter.getType(e);
      CSimpleType convertedType;
      if (type instanceof CSimpleType) {
        convertedType = (CSimpleType) type;
      } else {
        // this case applies when the target type is, for example, an enum type
        convertedType = CNumericTypes.INT;
      }
      switch (operator) {
        case AMPER:
          return UnknownValue.getInstance();
        case SIZEOF:
          return KnownExplicitValue.valueOf(machineModel.getSizeof(type));
        case MINUS: {
          NumericValue nValue = new NumericValue(expValue.getValue());
          nValue = nValue.negate();
          return CoreShapeAdapter.getInstance().convertFromValue(nValue, convertedType);
        }
        case TILDE: {
          NumericValue nValue = new NumericValue(~expValue.getValue().longValue());
          return CoreShapeAdapter.getInstance().convertFromValue(nValue, convertedType);
        }
        default:
          return UnknownValue.getInstance();
      }
    }
  }

}
