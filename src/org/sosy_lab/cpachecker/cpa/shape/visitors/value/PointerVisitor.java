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

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.function.ShapePointerAdapter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeAddressValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndState;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.AddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.shape.visitors.results.ExplicitValueAndState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor evaluates expressions of pointer type as address values (symbolic value). After we
 * evaluate expression as symbolic value using a legacy symbolic value visitor, we look into
 * shape graph to determine if the symbolic value has address semantics.
 */
public class PointerVisitor extends SymbolicValueVisitor {

  public static final String LITERAL_PREFIX = "LITERAL:";
  public static final String LITERAL_TEMPLATE = LITERAL_PREFIX.concat("%s");

  public PointerVisitor(CFAEdge pEdge, ShapeState pState, List<AbstractState> pOtherStates) {
    super(pEdge, pState, pOtherStates);
  }

  /* ****************** */
  /* evaluation methods */
  /* ****************** */

  @Override
  public AddressValueAndStateList visit(CArraySubscriptExpression e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().getAddressFromSymbolicValues(super.visit(e));
  }

  @Override
  public AddressValueAndStateList visit(CFieldReference e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().getAddressFromSymbolicValues(super.visit(e));
  }

  @Override
  public AddressValueAndStateList visit(CIdExpression e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().getAddressFromSymbolicValues(super.visit(e));
  }

  /**
   * This method should be overridden because the base symbolic value visitor does not
   * specifically handle unary expression such as ampersand.
   */
  @Override
  public AddressValueAndStateList visit(CUnaryExpression e) throws CPATransferException {
    UnaryOperator operator = e.getOperator();
    CExpression operand = e.getOperand();
    switch (operator) {
      case AMPER:
        return handleAddressOf(operand);
      default:
        // these cases cannot have pointer type
        return AddressValueAndStateList.of(readableState);
    }
  }

  @Override
  public AddressValueAndStateList visit(CPointerExpression e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().getAddressFromSymbolicValues(super.visit(e));
  }

  /**
   * Since C supports pointer arithmetic such as (p + i), we should override this method. (The
   * base class simply treats all symbolic values as ones without numerical semantics)
   */
  @Override
  public AddressValueAndStateList visit(CBinaryExpression e) throws CPATransferException {
    BinaryOperator op = e.getOperator();
    CExpression lE = e.getOperand1();
    CExpression rE = e.getOperand2();
    CType lType = CoreShapeAdapter.getType(lE);
    CType rType = CoreShapeAdapter.getType(rE);
    // either one of operands is of pointer type
    boolean leftAddress = lType instanceof CPointerType || lType instanceof CArrayType;
    boolean rightAddress = rType instanceof CPointerType || rType instanceof CArrayType;
    CExpression address, offset;
    CPointerType addressType;
    if (leftAddress == rightAddress) {
      // both operands are numeric values --- impossible
      // both operands are pointers --- undefined
      return AddressValueAndStateList.of(readableState);
    } else if (leftAddress) {
      address = lE;
      offset = rE;
      if (lType instanceof CArrayType) {
        addressType = new CPointerType(lType.isConst(), lType.isVolatile(), ((CArrayType) lType)
            .getType());
      } else {
        addressType = (CPointerType) lType;
      }
      if (op != BinaryOperator.PLUS && op != BinaryOperator.MINUS) {
        // other operators are not supported in pointer arithmetic
        return AddressValueAndStateList.of(readableState);
      }
    } else {
      address = rE;
      offset = lE;
      if (rType instanceof CArrayType) {
        addressType = new CPointerType(rType.isConst(), rType.isVolatile(), ((CArrayType) rType)
            .getType());
      } else {
        addressType = (CPointerType) rType;
      }
      if (op != BinaryOperator.PLUS) {
        // we cannot negate a pointer value
        return AddressValueAndStateList.of(readableState);
      }
    }
    // handle pointer arithmetic
    return CoreShapeAdapter.getInstance().evaluatePointerArithmetic(readableState, otherStates,
        cfaEdge, address, offset, addressType, op);
  }

  @Override
  public AddressValueAndStateList visit(CCastExpression e) throws CPATransferException {
    return CoreShapeAdapter.getInstance().getAddressFromSymbolicValues(CoreShapeAdapter
        .getInstance().evaluateSymbolicValue(readableState, otherStates, cfaEdge, e.getOperand()));
  }

  @Override
  public AddressValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    return ShapePointerAdapter.instance()
        .evaluateFunctionCallExpression(pIastFunctionCallExpression, readableState, null,
            cfaEdge);
  }

  @Override
  public AddressValueAndStateList visit(CStringLiteralExpression e) throws CPATransferException {
    // The string literal is allocated in data segment. Generally speaking, we can regard a
    // string literal as a global array of `char`.
    String content = e.getContentString();
    String name = String.format(LITERAL_TEMPLATE, content);
    SGObject object = readableState.getObjectForVisibleVariable(name);
    if (object != null) {
      AddressValueAndState addressState = CoreShapeAdapter.getInstance().getAddress
          (readableState, object, KnownExplicitValue.ZERO);
      return AddressValueAndStateList.of(addressState);
    }
    int sizeofChar = CoreShapeAdapter.getInstance().getSizeofChar();
    int length = content.length() + 1;
    CArrayType type = new CArrayType(true, false, CNumericTypes.CHAR,
        CIntegerLiteralExpression.createDummyLiteral(length, CNumericTypes.UNSIGNED_INT));
    object = new SGRegion(name, type, KnownExplicitValue.valueOf(sizeofChar * length),
        SGRegion.STATIC);
    ShapeState newState = new ShapeState(readableState);
    newState.addGlobalObject((SGRegion) object);
    // fill in character values
    int offset = 0;
    for (int i = 0; i < content.length(); i++) {
      char ch = content.charAt(i);
      KnownSymbolicValue sValue = KnownSymbolicValue.valueOf(SymbolicValueFactory.getNewValue());
      sValue = newState.updateExplicitValue(sValue, KnownExplicitValue.valueOf(ch));
      newState = CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge, object,
          KnownExplicitValue.valueOf(offset), CNumericTypes.CHAR, sValue);
      offset += sizeofChar;
    }
    // append '\0' at the tail of string object
    newState = CoreShapeAdapter.getInstance().writeValue(newState, otherStates, cfaEdge, object,
        KnownExplicitValue.valueOf(offset), CNumericTypes.CHAR, KnownSymbolicValue.ZERO);
    // finally, create an address for this string literal
    AddressValueAndState stringValue = CoreShapeAdapter.getInstance().createAddress(newState,
        object, KnownExplicitValue.ZERO);
    return AddressValueAndStateList.of(stringValue);
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  private AddressValueAndStateList handleAddressOf(CExpression e) throws CPATransferException {
    // Note: e should be a l-value
    if (CoreShapeAdapter.getType(e) instanceof CFunctionType && e instanceof CIdExpression) {
      // function pointer
      return createAddressForFunction((CIdExpression) e);
    } else if (e instanceof CIdExpression) {
      // &x, then we should create a new address for this variable
      return createAddressForVariable((CIdExpression) e);
    } else if (e instanceof CPointerExpression) {
      // &(*x), then the value is the symbolic value of x
      // Note: x could be dereferenced, then x should be of T* or T[].
      return CoreShapeAdapter.getInstance().evaluateAddressValue(readableState, otherStates,
          cfaEdge, ((CPointerExpression) e).getOperand());
    } else if (e instanceof CFieldReference) {
      return createAddressForField((CFieldReference) e);
    } else if (e instanceof CArraySubscriptExpression) {
      return createAddressForArray((CArraySubscriptExpression) e);
    } else {
      // such case should not be reached
      return AddressValueAndStateList.of(readableState);
    }
  }

  private AddressValueAndStateList createAddressForFunction(CIdExpression e) {
    CFunctionDeclaration function = (CFunctionDeclaration) e.getDeclaration();
    SGObject object = readableState.getFunctionObject(function);
    if (object == null) {
      // no such function object, then we create a new one
      object = readableState.createFunctionObject(function);
    }
    return AddressValueAndStateList.of(CoreShapeAdapter.getInstance().createAddress(readableState,
        object, KnownExplicitValue.ZERO));
  }

  private AddressValueAndStateList createAddressForVariable(CIdExpression e) {
    SGObject object = readableState.getObjectForVisibleVariable(e.getName());
    if (object == null) {
      return AddressValueAndStateList.of(readableState);
    }
    return AddressValueAndStateList.of(CoreShapeAdapter.getInstance().createAddress
        (readableState, object, KnownExplicitValue.ZERO));
  }

  private AddressValueAndStateList createAddressForField(CFieldReference e)
      throws CPATransferException {
    List<AddressValueAndState> results = new ArrayList<>();
    List<AddressAndState> addresses = CoreShapeAdapter.getInstance().evaluateFieldAddress
        (readableState, otherStates, cfaEdge, e);
    for (AddressAndState addressAndState : addresses) {
      Address address = addressAndState.getObject();
      ShapeState newState = addressAndState.getShapeState();
      if (address.isUnknown()) {
        results.add(AddressValueAndState.of(newState));
        continue;
      }
      AddressValueAndState result = CoreShapeAdapter.getInstance().createAddress(newState,
          address.getObject(), address.getOffset());
      results.add(result);
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

  private AddressValueAndStateList createAddressForArray(CArraySubscriptExpression e)
      throws CPATransferException {
    // Note: the array expression should be l-value, since arr[x] is equivalent to *(arr + x),
    // then arr should have its address
    CExpression array = e.getArrayExpression();
    List<AddressValueAndState> results = new ArrayList<>();
    AddressValueAndStateList aResults = CoreShapeAdapter.getInstance().evaluateAddressValue
        (readableState, otherStates, cfaEdge, array);
    for (AddressValueAndState aResult : aResults.asAddressValueAndStateList()) {
      ShapeAddressValue address = aResult.getObject();
      ShapeState newState = aResult.getShapeState();
      if (address.isUnknown()) {
        results.add(AddressValueAndState.of(newState));
        continue;
      }
      CExpression index = e.getSubscriptExpression();
      List<ExplicitValueAndState> iResults = CoreShapeAdapter.getInstance().evaluateExplicitValue
          (readableState, otherStates, cfaEdge, index);
      for (ExplicitValueAndState iResult : iResults) {
        ShapeExplicitValue iValue = iResult.getObject();
        newState = iResult.getShapeState();
        if (iValue.isUnknown()) {
          results.add(AddressValueAndState.of(newState));
          continue;
        }
        ShapeExplicitValue arrayOffset = address.getOffset();
        int elementSize = CoreShapeAdapter.getInstance().evaluateSizeof(newState, otherStates,
            cfaEdge, CoreShapeAdapter.getType(e), e);
        ShapeExplicitValue explicitSize = KnownExplicitValue.valueOf(elementSize);
        ShapeExplicitValue newOffset = arrayOffset.add(iValue.multiply(explicitSize));
        AddressValueAndState newAddress = CoreShapeAdapter.getInstance().createAddress(newState,
            address.getObject(), newOffset);
        results.add(newAddress);
      }
    }
    return AddressValueAndStateList.copyOfAddressList(results);
  }

}
