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
package org.sosy_lab.cpachecker.cpa.shape.visitors.value;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapePointerAdapter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.AssumeEvaluator;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.SymbolicValueAndStateList;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * This visitor is used to evaluate a C expression into a symbolic value. This is quite useful
 * when addressing pointer arithmetic.
 */
public class SymbolicValueVisitor
    extends DefaultCExpressionVisitor<SymbolicValueAndStateList, CPATransferException>
    implements CRightHandSideVisitor<SymbolicValueAndStateList, CPATransferException> {

  protected final CFAEdge cfaEdge;
  protected final ShapeState readableState;
  protected final List<AbstractState> otherStates;

  public SymbolicValueVisitor(CFAEdge pEdge, ShapeState pState, List<AbstractState> pOtherStates) {
    cfaEdge = pEdge;
    readableState = pState;
    otherStates = pOtherStates;
  }

  /* ************* */
  /* visit methods */
  /* ************* */

  @Override
  protected SymbolicValueAndStateList visitDefault(CExpression exp) throws CPATransferException {
    return SymbolicValueAndStateList.of(readableState);
  }

  @Override
  public SymbolicValueAndStateList visit(CArraySubscriptExpression e) throws CPATransferException {
    List<AddressAndState> addressAndStates = CoreShapeAdapter.getInstance()
        .evaluateArraySubscriptAddress(readableState, otherStates, cfaEdge, e);
    List<SymbolicValueAndState> results = new ArrayList<>(addressAndStates.size());
    for (AddressAndState addressAndState : addressAndStates) {
      Address address = addressAndState.getObject();
      ShapeState state = addressAndState.getShapeState();
      if (address.isUnknown()) {
        // then the address value is unknown
        results.add(SymbolicValueAndState.of(state));
        continue;
      }
      CType eType = CoreShapeAdapter.getType(e);
      SymbolicValueAndStateList result = CoreShapeAdapter.getInstance().readValue(state,
          otherStates, cfaEdge, address.getObject(), address.getOffset(), eType, e);
      results.addAll(result.asSymbolicValueAndStateList());
    }
    return SymbolicValueAndStateList.copyOfValueList(results);
  }

  @Override
  public SymbolicValueAndStateList visit(CBinaryExpression e) throws CPATransferException {
    CExpression lE = e.getOperand1();
    CExpression rE = e.getOperand2();
    BinaryOperator operator = e.getOperator();
    List<SymbolicValueAndState> results = new ArrayList<>();

    SymbolicValueAndStateList lResults = CoreShapeAdapter.getInstance().evaluateSymbolicValue
        (readableState, otherStates, cfaEdge, lE);
    for (SymbolicValueAndState lResult : lResults.asSymbolicValueAndStateList()) {
      ShapeSymbolicValue lValue = lResult.getObject();
      ShapeState lState = lResult.getShapeState();
      if (lValue.isUnknown()) {
        results.add(SymbolicValueAndState.of(lState));
        continue;
      }
      // otherwise, we continue to evaluate the second operand
      SymbolicValueAndStateList rResults = CoreShapeAdapter.getInstance().evaluateSymbolicValue
          (lState, otherStates, cfaEdge, rE);
      for (SymbolicValueAndState rResult : rResults.asSymbolicValueAndStateList()) {
        ShapeSymbolicValue rValue = rResult.getObject();
        ShapeState rState = rResult.getShapeState();
        if (rValue.isUnknown()) {
          results.add(SymbolicValueAndState.of(rState));
          continue;
        }
        SymbolicValueAndState tResult = evaluateBinaryExpression(lValue, rValue, operator, rState);
        results.add(tResult);
      }
    }
    return SymbolicValueAndStateList.copyOfValueList(results);
  }

  @Override
  public SymbolicValueAndStateList visit(CCastExpression e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().evaluateSymbolicValue(readableState, otherStates,
        cfaEdge, e.getOperand());
  }

  @Override
  public SymbolicValueAndStateList visit(CFieldReference e) throws CPATransferException {
    List<SymbolicValueAndState> results = new ArrayList<>();
    List<AddressAndState> addressAndStates = CoreShapeAdapter.getInstance().evaluateFieldAddress
        (readableState, otherStates, cfaEdge, e);
    for (AddressAndState addressAndState : addressAndStates) {
      Address address = addressAndState.getObject();
      ShapeState state = addressAndState.getShapeState();
      if (address.isUnknown()) {
        results.add(SymbolicValueAndState.of(state));
        continue;
      }
      CType type = CoreShapeAdapter.getType(e);
      SymbolicValueAndStateList result = CoreShapeAdapter.getInstance().readValue(state,
          otherStates, cfaEdge, address.getObject(), address.getOffset(), type, e);
      results.addAll(result.asSymbolicValueAndStateList());
    }
    return SymbolicValueAndStateList.copyOfValueList(results);
  }

  @Override
  public SymbolicValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    // there is no need to model library functions here, because (1) memory manipulations are
    // handled in pointer visitor; (2) there is no need to implement library functions for
    // non-address symbolic values because the result is almost unknown value.
    if (ShapePointerAdapter.instance().isRegistered(pIastFunctionCallExpression)) {
      // address value is a special kind of symbolic value
      return ShapePointerAdapter.instance().evaluateFunctionCallExpression
          (pIastFunctionCallExpression, readableState, null, cfaEdge);
    }
    return SymbolicValueAndStateList.of(readableState);
  }

  @Override
  public SymbolicValueAndStateList visit(CCharLiteralExpression e) throws CPATransferException {
    char value = e.getCharacter();
    ShapeSymbolicValue result = (value == 0) ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
    return SymbolicValueAndStateList.of(readableState, result);
  }

  @Override
  public SymbolicValueAndStateList visit(CFloatLiteralExpression e) throws CPATransferException {
    // if the floating point values in a specific range, its value is regarded as zero
    BigDecimal value = e.getValue();
    if (CompIntegers.isAlmostZero(value)) {
      return SymbolicValueAndStateList.of(readableState, KnownSymbolicValue.ZERO);
    } else {
      return SymbolicValueAndStateList.of(readableState);
    }
  }

  @Override
  public SymbolicValueAndStateList visit(CIdExpression e) throws CPATransferException {
    CSimpleDeclaration declaration = e.getDeclaration();
    if (declaration instanceof CEnumerator) {
      long enumValue = ((CEnumerator) declaration).getValue();
      ShapeSymbolicValue value = (enumValue == 0) ? KnownSymbolicValue.ZERO : UnknownValue
          .getInstance();
      return SymbolicValueAndStateList.of(readableState, value);
    } else if (declaration instanceof CVariableDeclaration || declaration instanceof
        CParameterDeclaration) {
      // use the name without qualifier
      SGObject object = readableState.getObjectForVisibleVariable(e.getName());
      CType type = CoreShapeAdapter.getType(e);
      return CoreShapeAdapter.getInstance().readValue(readableState, otherStates, cfaEdge, object,
          KnownExplicitValue.ZERO, type, e);
    }
    // we do not handle function or type declaration
    return SymbolicValueAndStateList.of(readableState);
  }

  @Override
  public SymbolicValueAndStateList visit(CIntegerLiteralExpression e) throws CPATransferException {
    BigInteger value = e.getValue();
    ShapeSymbolicValue result = value.equals(BigInteger.ZERO) ? KnownSymbolicValue.ZERO :
                                UnknownValue.getInstance();
    return SymbolicValueAndStateList.of(readableState, result);
  }

  @Override
  public SymbolicValueAndStateList visit(CUnaryExpression e) throws CPATransferException {
    UnaryOperator operator = e.getOperator();
    CExpression operand = e.getOperand();
    switch (operator) {
      case AMPER:
        // this case should be handled by pointer visitor
        return SymbolicValueAndStateList.of(readableState);
      case MINUS:
        List<SymbolicValueAndState> results = new ArrayList<>();
        SymbolicValueAndStateList oResults = operand.accept(this);
        for (SymbolicValueAndState oResult : oResults.asSymbolicValueAndStateList()) {
          ShapeSymbolicValue value = oResult.getObject();
          ShapeState state = oResult.getShapeState();
          ShapeSymbolicValue result = value.equals(KnownSymbolicValue.ZERO) ? value :
                                      UnknownValue.getInstance();
          results.add(SymbolicValueAndState.of(state, result));
        }
        return SymbolicValueAndStateList.copyOfValueList(results);
      case SIZEOF:
        CType type = CoreShapeAdapter.getType(operand);
        int size = CoreShapeAdapter.getInstance().evaluateSizeof(readableState, otherStates,
            cfaEdge, type, operand);
        ShapeSymbolicValue sizeValue = (size == 0) ? KnownSymbolicValue.ZERO : UnknownValue
            .getInstance();
        return SymbolicValueAndStateList.of(readableState, sizeValue);
      case TILDE:
        // the result is symbolically unknown in general
      default:
        return SymbolicValueAndStateList.of(readableState);
    }
  }

  @Override
  public SymbolicValueAndStateList visit(CTypeIdExpression e) throws CPATransferException {
    TypeIdOperator operator = e.getOperator();
    CType type = e.getType();
    switch (operator) {
      case SIZEOF:
        ShapeSymbolicValue value = CoreShapeAdapter.getInstance().evaluateSizeof(readableState,
            otherStates, cfaEdge, type) == 0 ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
        return SymbolicValueAndStateList.of(readableState, value);
      case ALIGNOF:
        // a general data type have non-zero align value
      default:
        return SymbolicValueAndStateList.of(readableState);
    }
  }

  @Override
  public SymbolicValueAndStateList visit(CPointerExpression e) throws CPATransferException {
    List<SymbolicValueAndState> results = new ArrayList<>();
    CExpression operand = e.getOperand();
    CType eType = CoreShapeAdapter.getType(e);
    AddressValueAndStateList addressValueAndStates =
        CoreShapeAdapter.getInstance().evaluateAddressValue
            (readableState, otherStates, cfaEdge, operand);
    for (AddressValueAndState addressAndState : addressValueAndStates
        .asAddressValueAndStateList()) {
      ShapeAddressValue address = addressAndState.getObject();
      ShapeState state = addressAndState.getShapeState();
      if (address.isUnknown()) {
        // ROB-1: A possible dereference error
        results.add(SymbolicValueAndState.of(state));
        continue;
      }
      // In general, for pointer expression *e, we evaluate the address value of e and then
      // read the symbolic value by this address from shape graph.
      // However, if pointer expression has array type (e.g. *(&arr)), then the evaluation result
      // is the value of pointer that points to arr
      if (eType instanceof CArrayType) {
        results.add(CoreShapeAdapter.getInstance().createAddress(state, address.getObject(), address
            .getOffset()));
      } else {
        results.addAll(
            CoreShapeAdapter.getInstance().readValue(state, otherStates, cfaEdge,
                address.getObject(), address.getOffset(), eType, e).asSymbolicValueAndStateList());
      }
    }
    return SymbolicValueAndStateList.copyOfValueList(results);
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private SymbolicValueAndState evaluateBinaryExpression(
      ShapeSymbolicValue lValue,
      ShapeSymbolicValue rValue,
      BinaryOperator operator,
      ShapeState pState) {
    if (lValue.isUnknown() || rValue.isUnknown()) {
      return SymbolicValueAndState.of(pState);
    }
    // Since symbolic values have no numerical semantics, for most cases the result is the
    // unknown value!
    if (operator.isLogicalOperator()) {
      AssumeEvaluator evaluator = CoreShapeAdapter.getInstance().getAssumeEvaluator();
      ShapeSymbolicValue value = evaluator.evaluateAssumption(pState, lValue, rValue, operator);
      return SymbolicValueAndState.of(pState, value);
    } else {
      boolean isZero;
      switch (operator) {
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          // additive operators
          isZero = lValue.equals(KnownSymbolicValue.ZERO) && rValue.equals(KnownSymbolicValue.ZERO);
          ShapeSymbolicValue result = isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
          return SymbolicValueAndState.of(pState, result);
        case MINUS:
        case MODULO:
          isZero = lValue.equals(rValue);
          result = isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
          return SymbolicValueAndState.of(pState, result);
        case DIVIDE:
          if (rValue.equals(KnownSymbolicValue.ZERO)) {
            return SymbolicValueAndState.of(pState);
          }
          isZero = lValue.equals(KnownSymbolicValue.ZERO);
          result = isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
          return SymbolicValueAndState.of(pState, result);
        case MULTIPLY:
        case BINARY_AND:
          // multiplicative operators
          isZero = lValue.equals(KnownSymbolicValue.ZERO) || rValue.equals(KnownSymbolicValue.ZERO);
          result = isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
          return SymbolicValueAndState.of(pState, result);
        default:
          // we should not reach here
          return SymbolicValueAndState.of(pState);
      }
    }
  }
}
