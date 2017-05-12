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
package org.sosy_lab.cpachecker.cpa.bind;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;

import java.util.List;
import java.util.Set;

/**
 * Recursively visit a LeftHandSide of a CExpression, and generate the following:
 *
 * (1) A over-approximated access path of the left hand side. i.e., (a) a[x] = y, omit 'x' and treat
 * it as any element of 'a' can be 'y' (b) *p = y or *(p + i) = y, omit the offset and treat any of
 * the memory pointed from 'p' can be 'y'
 *
 * Return value may be null
 */
public class AccessPathExtractorForLHS
    implements CLeftHandSideVisitor<AccessPath, UnrecognizedCCodeException> {

  private List<AbstractState> otherStates = null;

  /**
   * This is set if we encounter an undetermined array index.
   * For example, for a[x] we omit the subscript expression and the result is a.
   */
  private boolean shouldStop = false;

  private boolean composeMode = false;

  AccessPathExtractorForLHS(List<AbstractState> pOtherStates) {
    otherStates = pOtherStates;
  }

  public AccessPath visit(CLeftHandSide lhs) throws UnrecognizedCCodeException {
    if (lhs instanceof CArraySubscriptExpression) {
      return visit((CArraySubscriptExpression) lhs);
    } else if (lhs instanceof CFieldReference) {
      return visit((CFieldReference) lhs);
    } else if (lhs instanceof CIdExpression) {
      return visit((CIdExpression) lhs);
    } else if (lhs instanceof CPointerExpression) {
      return visit((CPointerExpression) lhs);
    } else if (lhs instanceof CComplexCastExpression) {
      return visit((CComplexCastExpression) lhs);
    } else {
      throw new UnrecognizedCCodeException("Unknown Left Hand Side", lhs);
    }
  }

  /**
   * Recursively work on CLeftHandSide
   */
  private AccessPath continueOnLhs(CLeftHandSide exp) throws UnrecognizedCCodeException {
    return exp.accept(this);
  }

  @Override
  public AccessPath visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCCodeException {
    CExpression array = pIastArraySubscriptExpression.getArrayExpression();
    AccessPath prePath;
    if (array instanceof CLeftHandSide) {
      prePath = continueOnLhs((CLeftHandSide) array);
    } else {
      // both continueOnLhs() and continueOnNonLhs() should return an ACTUAL path
      prePath = continueOnNonLhs(array);
    }
    if (prePath == null || shouldStop) {
      return prePath;
    }
    AccessPath arrayPath = null;
    CPointerType pointerType = Types.extractPointerType(array.getExpressionType());
    if (pointerType != null) {
      prePath = composeArrayWithIndex(prePath, 0);
      composeMode = true;
      if (otherStates != null) {
        ShapeState shapeState = AbstractStates.extractStateByType(otherStates, ShapeState.class);
        if (shapeState != null) {
          Set<AccessPath> targets = shapeState.getPointsToTargetForAccessPath(prePath);
          if (!targets.isEmpty()) {
            arrayPath = targets.iterator().next();
          }
        }
      }
    } else {
      arrayPath = prePath;
    }
    if (arrayPath != null) {
      CExpression index = pIastArraySubscriptExpression.getSubscriptExpression();
      if (index instanceof CIntegerLiteralExpression) {
        long indexValue = ((CIntegerLiteralExpression) index).getValue().longValue();
        arrayPath = composeArrayWithIndex(arrayPath, indexValue);
      } else {
        // composeArrayWithNoIndex() attempts to combine undetermined array index at the tail and
        // then remove it
        // for example: arr[3][x] is truncated as arr[3]; (p+3)[x] is truncated as malloc_p
        arrayPath = composeArrayWithNoIndex(arrayPath);
        shouldStop = true;
      }
    }
    composeMode = false;
    return arrayPath;
  }

  @Override
  public AccessPath visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
    CExpression owner = pIastFieldReference.getFieldOwner();
    AccessPath prePath = null;
    boolean containPtr = false;
    if (owner instanceof CLeftHandSide) {
      prePath = continueOnLhs((CLeftHandSide) owner);
      if (shouldStop || prePath == null) {
        // since composeMode works only when shouldStop is FALSE, therefore there is no need to
        // reset composeMode flag here
        return prePath;
      }
      if (pIastFieldReference.isPointerDereference()) {
        prePath.appendSegment(new PointerDereferenceSegment());
        containPtr = true;
      }
    } else if (pIastFieldReference.isPointerDereference()) {
      prePath = continueOnNonLhs(owner);
      if (prePath == null) {
        return null;
      }
      if (prePath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        prePath.removeLastSegment();
      }
    } else {
      return null;
    }
    AccessPath actualPath = null;
    if (containPtr) {
      if (otherStates != null) {
        ShapeState shapeState = AbstractStates.extractStateByType(otherStates, ShapeState.class);
        if (shapeState != null) {
          Set<AccessPath> targets = shapeState.getPointsToTargetForAccessPath(prePath);
          if (!targets.isEmpty()) {
            actualPath = targets.iterator().next();
          }
        }
      }
    } else {
      actualPath = prePath;
    }
    if (actualPath != null) {
      actualPath.appendSegment(new FieldAccessSegment(pIastFieldReference.getFieldName()));
    }
    composeMode = false;
    return actualPath;
  }

  @Override
  public AccessPath visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
    // create path segment
    composeMode = false;
    return new AccessPath(pIastIdExpression.getDeclaration());
  }

  @Override
  public AccessPath visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
    CExpression operand = pPointerExpression.getOperand();
    AccessPath prePath;
    boolean containsPtr = false;
    if (operand instanceof CLeftHandSide) {
      prePath = continueOnLhs((CLeftHandSide) operand);
      if (prePath == null) {
        return null;
      }
      if (shouldStop) {
        return null;
      }
      prePath.appendSegment(new PointerDereferenceSegment());
      containsPtr = true;
    } else {
      prePath = continueOnNonLhs(operand);
      if (prePath == null) {
        return null;
      }
      if (prePath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        prePath.removeLastSegment();
      }
    }
    composeMode = false;
    if (containsPtr) {
      if (otherStates != null) {
        ShapeState shapeState = AbstractStates.extractStateByType(otherStates, ShapeState.class);
        if (shapeState != null) {
          Set<AccessPath> targets = shapeState.getPointsToTargetForAccessPath(prePath);
          if (!targets.isEmpty()) {
            return targets.iterator().next();
          }
        }
      }
    } else {
      return prePath;
    }
    return null;
  }

  private AccessPath continueOnNonLhs(CExpression nonLhs)
      throws UnrecognizedCCodeException {
    if (nonLhs instanceof CUnaryExpression) {
      UnaryOperator operator = ((CUnaryExpression) nonLhs).getOperator();
      if (operator == UnaryOperator.AMPER) {
        CExpression operand = ((CUnaryExpression) nonLhs).getOperand();
        assert (operand instanceof CLeftHandSide);
        AccessPath prePath = continueOnLhs((CLeftHandSide) operand);
        if (prePath != null) {
          composeMode = true;
          prePath.appendSegment(new AddressingSegment());
          return prePath;
        }
        return null;
      }
    } else if (nonLhs instanceof CBinaryExpression) {
      CExpression op1 = ((CBinaryExpression) nonLhs).getOperand1();
      CExpression op2 = ((CBinaryExpression) nonLhs).getOperand2();
      BinaryOperator optr = ((CBinaryExpression) nonLhs).getOperator();
      CType type1 = op1.getExpressionType();
      CType type2 = op2.getExpressionType();
      CPointerType pointerType1 = Types.extractPointerType(type1);
      CPointerType pointerType2 = Types.extractPointerType(type2);
      CArrayType arrayType1 = Types.extractArrayType(type1);
      CArrayType arrayType2 = Types.extractArrayType(type2);
      AccessPath actualPath = null;
      if (pointerType1 != null && Types.isNumericalType(type2)) {
        // for example, p + 2 returns malloc_p.[2], p + x returns malloc_p
        // that said, we need to handle pointer expressions in this function
        actualPath = handleBinaryPointerExpression(op1, pointerType1, optr, op2);
      } else if (arrayType1 != null && Types.isNumericalType(type2)) {
        actualPath = handleBinaryArrayExpression(op1, optr, op2);
      } else if (pointerType2 != null && Types.isNumericalType(type1)) {
        if (optr == BinaryOperator.PLUS) {
          actualPath = handleBinaryPointerExpression(op2, pointerType2, optr, op1);
        }
      } else if (arrayType2 != null && Types.isNumericalType(type1)) {
        if (optr == BinaryOperator.PLUS) {
          actualPath = handleBinaryArrayExpression(op2, optr, op1);
        }
      }
      return actualPath;
    }
    return null;
  }

  private AccessPath handleBinaryPointerExpression(
      CExpression pointer,
      CPointerType pointerType,
      BinaryOperator operator,
      CExpression index)
      throws UnrecognizedCCodeException {
    CPointerExpression pointerExp = new CPointerExpression(FileLocation.DUMMY, pointerType
        .getType(), pointer);
    AccessPath prePath = continueOnLhs(pointerExp);
    if (shouldStop) {
      // consider *(p + k + 2), when we address *(l + 2) where l is (p + k), we directly return
      // malloc_p without composing [2] into access path
      return prePath;
    }
    composeMode = true;
    boolean isUndet = false;
    if (prePath != null) {
      if (index instanceof CIntegerLiteralExpression) {
        long indexValue = ((CIntegerLiteralExpression) index).getValue().longValue();
        switch (operator) {
          case PLUS:
            break;
          case MINUS:
            indexValue = -indexValue;
            break;
          default:
            throw new AssertionError("Unsupported binary operation in pointer expression");
        }
        // TODO: regularization of array expression
        prePath = composeArrayWithIndex(prePath, indexValue);
      } else {
        // TODO: derive concrete index value using value analysis information
        shouldStop = true;
        isUndet = true;
        // the array index is initialized as 0
        prePath = composeArrayWithIndex(prePath, 0);
      }
      // use pointer analysis info.
      AccessPath actualPath = null;
      if (prePath.isActualPath()) {
        actualPath = prePath;
      } else {
        if (otherStates != null) {
          ShapeState shapeState = AbstractStates.extractStateByType(otherStates, ShapeState.class);
          if (shapeState != null) {
            Set<AccessPath> targets = shapeState.getPointsToTargetForAccessPath(prePath);
            if (!targets.isEmpty()) {
              actualPath = targets.iterator().next();
            }
          }
        }
      }
      if (actualPath != null) {
        if (isUndet) {
          actualPath = composeArrayWithNoIndex(actualPath);
        }
      }
      return actualPath;
    }
    return null;
  }

  private AccessPath handleBinaryArrayExpression(
      CExpression array,
      BinaryOperator operator,
      CExpression index)
      throws UnrecognizedCCodeException {
    AccessPath prePath;
    if (array instanceof CLeftHandSide) {
      prePath = continueOnLhs((CLeftHandSide) array);
    } else {
      prePath = continueOnNonLhs(array);
    }
    if (shouldStop) {
      return prePath;
    }
    if (prePath != null) {
      prePath = composeArrayWithIndex(prePath, 0);
      composeMode = true;
      if (index instanceof CIntegerLiteralExpression) {
        long indexValue = ((CIntegerLiteralExpression) index).getValue().longValue();
        switch (operator) {
          case PLUS:
            break;
          case MINUS:
            indexValue = -indexValue;
            break;
          default:
            throw new AssertionError("Unsupported binary operator in array expression");
        }
        return composeArrayWithIndex(prePath, indexValue);
      } else {
        // TODO: derive concrete index value by value analysis
        shouldStop = true;
        return composeArrayWithNoIndex(prePath);
      }
    }
    return null;
  }

  private AccessPath composeArrayWithIndex(AccessPath pointerPath, long indexValue) {
    AccessPath newPath = AccessPath.copyOf(pointerPath);
    if (newPath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
      newPath.removeLastSegment();
    }
    if (composeMode) {
      PathSegment lastSegment = newPath.getLastSegment();
      if (lastSegment instanceof ArrayConstIndexSegment) {
        long existingIndex = ((ArrayConstIndexSegment) lastSegment).getIndex();
        existingIndex += indexValue;
        ArrayConstIndexSegment newIndexSeg = new ArrayConstIndexSegment(existingIndex);
        newPath.removeLastSegment();
        newPath.appendSegment(newIndexSeg);
      } else {
        newPath.appendSegment(new ArrayConstIndexSegment(indexValue));
      }
    } else {
      newPath.appendSegment(new ArrayConstIndexSegment(indexValue));
    }
    return newPath;
  }

  /**
   * Try to compose an undetermined segment at the tail and remove the ending undetermined segment.
   * For example: arr[3] --> arr[3] (compose mode = OFF)
   * p[3] --> p (compose mode = ON)
   */
  private AccessPath composeArrayWithNoIndex(AccessPath pointerPath) {
    AccessPath newPath = AccessPath.copyOf(pointerPath);
    if (newPath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
      newPath.removeLastSegment();
    }
    PathSegment lastSegment = newPath.getLastSegment();
    if (lastSegment instanceof ArrayConstIndexSegment) {
      if (composeMode) {
        newPath.removeLastSegment();
      }
    }
    return newPath;
  }

  @Override
  public AccessPath visit(CComplexCastExpression pComplexCastExpression)
      throws UnrecognizedCCodeException {
    CExpression operand = pComplexCastExpression.getOperand();
    if (operand instanceof CLeftHandSide) {
      return continueOnLhs((CLeftHandSide) operand);
    } else {
      return null;
    }
  }
}