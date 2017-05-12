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

import com.google.common.collect.Lists;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapeValueAdapter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ValueAndState;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor is used to evaluate a C expression into an explicit value.
 * Different from symbolic value, explicit value has numerical semantics.
 * The evaluation result is {@link Value}, an interface defined in Value CPA.
 */
public class ExplicitValueVisitor
    extends DefaultCExpressionVisitor<List<ValueAndState>, CPATransferException>
    implements CRightHandSideVisitor<List<ValueAndState>, CPATransferException> {

  /**
   * This state is probably updated during evaluation.
   */
  private ShapeState readableState;
  private final List<AbstractState> otherStates;
  private final CFAEdge edge;

  // the following fields are required in invoking methods in abstract value visitor
  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  /**
   * The constructor of explicit value visitor.
   *
   * @param pState  Shape state
   * @param pEdge   CFA edge
   * @param pModel  current machine model in use
   * @param pLogger error logging entity
   */
  public ExplicitValueVisitor(
      ShapeState pState, List<AbstractState> pOtherStates, CFAEdge pEdge,
      MachineModel pModel, LogManagerWithoutDuplicates pLogger) {
    readableState = pState;
    otherStates = pOtherStates;
    edge = pEdge;
    machineModel = pModel;
    logger = pLogger;
  }

  public ShapeState getState() {
    return readableState;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  /* ****************** */
  /* override functions */
  /* ****************** */

  @Override
  protected List<ValueAndState> visitDefault(CExpression exp) throws CPATransferException {
    return Lists.newArrayList(ValueAndState.of(readableState));
  }

  @Override
  public List<ValueAndState> visit(CArraySubscriptExpression e) throws CPATransferException {
    return evaluateLeftHandSide(e);
  }

  @Override
  public List<ValueAndState> visit(CBinaryExpression e) throws CPATransferException {
    // there are totally 3 cases in evaluating binary expression:
    // (1) ordinary binary expression, (2) pointer comparison, (3) pointer difference
    List<ValueAndState> results = new ArrayList<>();
    PointerOperation kind = getPointerOperationKind(e);
    CExpression lE = e.getOperand1();
    CExpression rE = e.getOperand2();
    switch (kind) {
      case UNKNOWN:
      case COMPARISON: {
        // ordinary binary expression
        List<ValueAndState> lResults = lE.accept(this);
        for (ValueAndState lvalueAndState : lResults) {
          Value lValue = lvalueAndState.getObject();
          ShapeState lState = lvalueAndState.getShapeState();
          if (lValue.isUnknown()) {
            results.add(ValueAndState.of(lState));
            continue;
          }
          readableState = lState;
          List<ValueAndState> rResults = rE.accept(this);
          for (ValueAndState rValueAndState : rResults) {
            Value rValue = rValueAndState.getObject();
            ShapeState rState = rValueAndState.getShapeState();
            if (rValue.isUnknown()) {
              results.add(ValueAndState.of(rState));
              continue;
            }
            Value tValue = AbstractExpressionValueVisitor.calculateBinaryOperation(lValue,
                rValue, e, machineModel, logger);
            results.add(ValueAndState.of(rState, tValue));
          }
        }
        return results;
      }
      default:
        // DIFFERENCE
        // it is possible to derive a concrete pointer difference value only when:
        // (1) two pointers point to the same memory object; (2) two pointers point to stack
        // objects which belong to the same stack frame and the same segment.
        SymbolicValueAndStateList lResults = CoreShapeAdapter.getInstance().evaluateSymbolicValue
            (readableState, otherStates, edge, lE);
        for (SymbolicValueAndState lResult : lResults.asSymbolicValueAndStateList()) {
          ShapeSymbolicValue lValue = lResult.getObject();
          ShapeState newState = lResult.getShapeState();
          SymbolicValueAndStateList rResults = CoreShapeAdapter.getInstance().evaluateSymbolicValue
              (newState, otherStates, edge, rE);
          for (SymbolicValueAndState rResult : rResults.asSymbolicValueAndStateList()) {
            ShapeSymbolicValue rValue = rResult.getObject();
            newState = rResult.getShapeState();
            Value difference = evaluatePointerDifference(newState, lValue, rValue);
            results.add(ValueAndState.of(newState, difference));
          }
        }
        return results;
    }
  }

  @Override
  public List<ValueAndState> visit(CCastExpression e) throws CPATransferException {
    CExpression operand = e.getOperand();
    CType type = CoreShapeAdapter.getType(e);
    List<ValueAndState> results = operand.accept(this);
    for (int pos = 0; pos < results.size(); pos++) {
      ValueAndState result = results.get(pos);
      Value value = result.getObject();
      ShapeState newState = result.getShapeState();
      value = AbstractExpressionValueVisitor.castCValue(value, type, machineModel, logger, e
          .getFileLocation());
      results.set(pos, ValueAndState.of(newState, value));
    }
    return results;
  }

  @Override
  public List<ValueAndState> visit(CFieldReference e) throws CPATransferException {
    return evaluateLeftHandSide(e);
  }

  @Override
  public List<ValueAndState> visit(CIdExpression e) throws CPATransferException {
    return evaluateLeftHandSide(e);
  }

  @Override
  public List<ValueAndState> visit(CCharLiteralExpression e) throws CPATransferException {
    return Lists.newArrayList(ValueAndState.of(readableState, new NumericValue((long) e
        .getCharacter())));
  }

  @Override
  public List<ValueAndState> visit(CFloatLiteralExpression e) throws CPATransferException {
    return Lists.newArrayList(ValueAndState.of(readableState, new NumericValue(e.getValue())));
  }

  @Override
  public List<ValueAndState> visit(CIntegerLiteralExpression e) throws CPATransferException {
    return Lists.newArrayList(ValueAndState.of(readableState, new NumericValue(e.getValue())));
  }

  @Override
  public List<ValueAndState> visit(CStringLiteralExpression e) throws CPATransferException {
    return Lists.newArrayList(ValueAndState.of(readableState));
  }

  @Override
  public List<ValueAndState> visit(CTypeIdExpression e) throws CPATransferException {
    final TypeIdOperator operator = e.getOperator();
    final CType type = e.getType();
    List<ValueAndState> results = new ArrayList<>();
    switch (operator) {
      case SIZEOF:
        int size = machineModel.getSizeof(type);
        results.add(ValueAndState.of(readableState, new NumericValue(size)));
        return results;
      case ALIGNOF:
        int align = machineModel.getAlignof(type);
        results.add(ValueAndState.of(readableState, new NumericValue(align)));
        return results;
      default:
        return Lists.newArrayList(ValueAndState.of(readableState));
    }
  }

  @Override
  public List<ValueAndState> visit(CUnaryExpression e) throws CPATransferException {
    final UnaryOperator operator = e.getOperator();
    final CExpression operand = e.getOperand();
    final CType innerType = CoreShapeAdapter.getType(operand);
    final CType type = CoreShapeAdapter.getType(e);
    switch (operator) {
      case AMPER:
        // address value should be symbolic
        return Lists.newArrayList(ValueAndState.of(readableState));
      case SIZEOF:
        return Lists.newArrayList(ValueAndState.of(readableState, new NumericValue(machineModel
            .getSizeof(innerType))));
      case ALIGNOF:
        return Lists.newArrayList(ValueAndState.of(readableState, new NumericValue(machineModel
            .getAlignof(innerType))));
    }
    // for other operators, we first evaluate the operand
    List<ValueAndState> results = new ArrayList<>();
    List<ValueAndState> oResults = operand.accept(this);
    for (ValueAndState oResult : oResults) {
      Value oValue = oResult.getObject();
      ShapeState oState = oResult.getShapeState();
      if (oValue.isUnknown()) {
        results.add(ValueAndState.of(oState));
        continue;
      }
      if (oValue instanceof SymbolicValue) {
        results.add(ValueAndState.of(oState, AbstractExpressionValueVisitor
            .createSymbolicExpression(oValue, innerType, operator, type)));
      } else if (oValue.isNumericValue()) {
        final NumericValue numericValue = (NumericValue) oValue;
        switch (operator) {
          case MINUS:
            results.add(ValueAndState.of(oState, numericValue.negate()));
            break;
          case TILDE:
            results.add(ValueAndState.of(oState, new NumericValue(~(numericValue).longValue())));
            break;
          default:
            throw new IllegalArgumentException("unknown unary operator: " + operator);
        }
      } else {
        results.add(ValueAndState.of(oState));
      }
    }
    return results;
  }

  @Override
  public List<ValueAndState> visit(CPointerExpression e) throws CPATransferException {
    return evaluateLeftHandSide(e);
  }

  @Override
  public List<ValueAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    return ShapeValueAdapter.instance().evaluateFunctionCallExpression(pIastFunctionCallExpression,
        readableState, otherStates, edge);
  }

  /* ******************* */
  /* auxiliary functions */
  /* ******************* */

  private ShapeExplicitValue getExplicitValue(ShapeSymbolicValue pValue) {
    if (pValue.isUnknown()) {
      return UnknownValue.getInstance();
    }
    if (pValue.equals(KnownSymbolicValue.ZERO)) {
      return KnownExplicitValue.ZERO;
    }
    if (pValue.equals(KnownSymbolicValue.TRUE)) {
      return KnownExplicitValue.ONE;
    }
    return readableState.getExplicit((KnownSymbolicValue) pValue);
  }

  private List<ValueAndState> evaluateLeftHandSide(CLeftHandSide pCLeftHandSide)
      throws CPATransferException {
    List<ValueAndState> results = new ArrayList<>();
    SymbolicValueAndStateList valueAndStates = CoreShapeAdapter.getInstance().evaluateSymbolicValue
        (readableState, otherStates, edge, pCLeftHandSide);
    for (SymbolicValueAndState valueAndState : valueAndStates.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue value = valueAndState.getObject();
      ShapeState newState = valueAndState.getShapeState();
      ShapeExplicitValue explicitValue = getExplicitValue(value);
      if (explicitValue.isUnknown()) {
        results.add(ValueAndState.of(newState));
      } else {
        results.add(ValueAndState.of(newState, new NumericValue(explicitValue.getAsLong())));
      }
    }
    return results;
  }

  private enum PointerOperation {
    COMPARISON,
    DIFFERENCE,
    UNKNOWN
  }

  private PointerOperation getPointerOperationKind(CBinaryExpression pE) {
    CExpression lE = pE.getOperand1();
    CExpression rE = pE.getOperand2();
    CType lType = lE.getExpressionType();
    CType rType = rE.getExpressionType();
    boolean leftPointer = lType instanceof CPointerType || lType instanceof CArrayType;
    boolean rightPointer = rType instanceof CPointerType || rType instanceof CArrayType;
    if (!leftPointer || !rightPointer) {
      return PointerOperation.UNKNOWN;
    }
    switch (pE.getOperator()) {
      case EQUALS:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case NOT_EQUALS:
        return PointerOperation.COMPARISON;
      case MINUS:
        return PointerOperation.DIFFERENCE;
      default:
        return PointerOperation.UNKNOWN;
    }
  }

  /**
   * Attempt to compute the difference between two pointers.
   *
   * @param pState a shape state
   * @param pV1    symbolic value 1, which is possibly a pointer
   * @param pV2    symbolic value 2
   * @return the pointer difference between these two values
   */
  public static Value evaluatePointerDifference(
      ShapeState pState,
      ShapeSymbolicValue pV1,
      ShapeSymbolicValue pV2) {
    if (pV1.isUnknown() || pV2.isUnknown()) {
      return Value.UnknownValue.getInstance();
    }
    long v1 = pV1.getAsLong();
    long v2 = pV2.getAsLong();
    SGPointToEdge pointer1 = pState.getPointer(v1);
    SGPointToEdge pointer2 = pState.getPointer(v2);
    if (pointer1 == null || pointer2 == null) {
      return Value.UnknownValue.getInstance();
    }
    // then, two symbolic values are pointers, we can compute their pointer difference
    SGObject object1 = pointer1.getObject();
    SGObject object2 = pointer2.getObject();
    if (object1 == object2) {
      // two pointers point to the same object
      return new NumericValue(pointer1.getOffset() - pointer2.getOffset());
    } else {
      return Value.UnknownValue.getInstance();
    }
  }

}
