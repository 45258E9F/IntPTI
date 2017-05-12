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
package org.sosy_lab.cpachecker.cpa.taint;

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
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.AddressingSegment;
import org.sosy_lab.cpachecker.util.access.ArrayAccessSegment;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.access.PointerDereferenceSegment;

import java.util.List;
import java.util.Set;

public class AccessPathVisitorForTaint
    implements CLeftHandSideVisitor<AccessPath, UnrecognizedCCodeException> {

  private List<AbstractState> otherStates = null;

  /**
   * If the compose mode is ON, then we should compose the array index segment with the previous one
   */
  private boolean composeMode = false;
  /**
   * If this flag is set to TRUE, then no pointer dereference will be parsed in the following.
   * For example, consider the expression p[x][2] where the type of p is int**, the parsing
   * result should be p[] since p[x][2] is equivalent to *(p[x] + 2) while its actual memory
   * location cannot be decided statically.
   */
  private boolean refuseDereference = false;

  public AccessPathVisitorForTaint(List<AbstractState> pOtherStates) {
    otherStates = pOtherStates;
  }

  private AccessPath continueOnLHS(CLeftHandSide exp) throws UnrecognizedCCodeException {
    return exp.accept(this);
  }

  @Override
  public AccessPath visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCCodeException {
    CExpression array = pIastArraySubscriptExpression.getArrayExpression();
    AccessPath prePath;
    if (array instanceof CLeftHandSide) {
      prePath = continueOnLHS((CLeftHandSide) array);
    } else {
      // both continueOnLHS() and handleNonLHS() should return an ACTUAL path
      prePath = handleNonLHS(array);
    }
    if (prePath == null) {
      return null;
    }
    AccessPath arrayPath = null;
    CPointerType pointerType = Types.extractPointerType(array.getExpressionType());
    if (pointerType != null) {
      if (refuseDereference) {
        return null;
      }
      prePath = composeArrayWithIndex(prePath, 0);
      composeMode = true;
      if (otherStates != null) {
        SMGState smgState = AbstractStates.extractStateByType(otherStates, SMGState.class);
        if (smgState != null) {
          Set<AccessPath> targets = smgState.parsePointToFromMemoryLocationToAccessPath(prePath);
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
        // TODO: derive the concrete index value using value analysis
        arrayPath = composeArrayWithNoIndex(arrayPath);
        refuseDereference = true;
      }
    }
    composeMode = false;
    return arrayPath;
  }

  @Override
  public AccessPath visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
    CExpression owner = pIastFieldReference.getFieldOwner();
    AccessPath prePath;
    boolean containPtr = false;
    if (owner instanceof CLeftHandSide) {
      prePath = continueOnLHS((CLeftHandSide) owner);
      if (prePath == null) {
        return null;
      }
      if (pIastFieldReference.isPointerDereference()) {
        if (refuseDereference) {
          return null;
        } else {
          prePath.appendSegment(new PointerDereferenceSegment());
          containPtr = true;
        }
      }
    } else if (pIastFieldReference.isPointerDereference()) {
      prePath = handleNonLHS(owner);
      if (prePath == null) {
        return null;
      }
      if (prePath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        prePath.removeLastSegment();
      }
    } else {
      return null;
    }
    // use pointer analysis to derive the actual path
    AccessPath resultPath = null;
    if (containPtr) {
      if (otherStates != null) {
        SMGState smgState = AbstractStates.extractStateByType(otherStates, SMGState.class);
        if (smgState != null) {
          Set<AccessPath> targets = smgState.parsePointToFromMemoryLocationToAccessPath(prePath);
          if (!targets.isEmpty()) {
            resultPath = targets.iterator().next();
          }
        }
      }
    } else {
      resultPath = prePath;
    }
    if (resultPath != null) {
      resultPath.appendSegment(new FieldAccessSegment(pIastFieldReference.getFieldName()));
    }
    composeMode = false;
    return resultPath;
  }

  @Override
  public AccessPath visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
    composeMode = false;
    return new AccessPath(pIastIdExpression.getDeclaration());
  }

  @Override
  public AccessPath visit(CPointerExpression pointerExpression) throws UnrecognizedCCodeException {
    CExpression operand = pointerExpression.getOperand();
    AccessPath prePath;
    boolean containsPtr = false;
    if (operand instanceof CLeftHandSide) {
      prePath = continueOnLHS((CLeftHandSide) operand);
      if (prePath == null) {
        return null;
      }
      if (refuseDereference) {
        return null;
      }
      prePath.appendSegment(new PointerDereferenceSegment());
      containsPtr = true;
    } else {
      prePath = handleNonLHS(operand);
      if (prePath == null) {
        return null;
      }
      if (prePath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        prePath.removeLastSegment();
      }
    }
    composeMode = false;
    // use pointer analysis info.
    if (containsPtr) {
      if (otherStates != null) {
        SMGState smgState = AbstractStates.extractStateByType(otherStates, SMGState.class);
        if (smgState != null) {
          Set<AccessPath> targets = smgState.parsePointToFromMemoryLocationToAccessPath(prePath);
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

  @Override
  public AccessPath visit(CComplexCastExpression complexCastExpression)
      throws UnrecognizedCCodeException {
    composeMode = false;
    CExpression operand = complexCastExpression.getOperand();
    if (operand instanceof CLeftHandSide) {
      return continueOnLHS((CLeftHandSide) operand);
    } else {
      return null;
    }
  }

  private AccessPath handleNonLHS(CExpression exp) throws UnrecognizedCCodeException {
    if (exp instanceof CUnaryExpression) {
      UnaryOperator operator = ((CUnaryExpression) exp).getOperator();
      if (operator == UnaryOperator.AMPER) {
        CExpression operand = ((CUnaryExpression) exp).getOperand();
        // in principle, only l-value could be addressed
        assert (operand instanceof CLeftHandSide);
        AccessPath prePath = continueOnLHS((CLeftHandSide) operand);
        if (prePath != null) {
          composeMode = true;
          prePath.appendSegment(new AddressingSegment());
          return prePath;
        }
        return null;
      }
    } else if (exp instanceof CBinaryExpression) {
      CExpression op1 = ((CBinaryExpression) exp).getOperand1();
      CExpression op2 = ((CBinaryExpression) exp).getOperand2();
      BinaryOperator operator = ((CBinaryExpression) exp).getOperator();
      CType type1 = op1.getExpressionType();
      CType type2 = op2.getExpressionType();
      CPointerType pointerType1 = Types.extractPointerType(type1);
      CPointerType pointerType2 = Types.extractPointerType(type2);
      CArrayType arrayType1 = Types.extractArrayType(type1);
      CArrayType arrayType2 = Types.extractArrayType(type2);
      AccessPath possibleNonActualPath = null;
      if (pointerType1 != null && Types.isNumericalType(type2)) {
        possibleNonActualPath = handleBinaryPointerExpression(op1, pointerType1, operator, op2);
      } else if (arrayType1 != null && Types.isNumericalType(type2)) {
        possibleNonActualPath = handleBinaryArrayExpression(op1, operator, op2);
      } else if (pointerType2 != null && Types.isNumericalType(type1)) {
        if (operator == BinaryOperator.PLUS) {
          possibleNonActualPath = handleBinaryPointerExpression(op2, pointerType2, operator, op1);
        }
      } else if (arrayType2 != null && Types.isNumericalType(type1)) {
        if (operator == BinaryOperator.PLUS) {
          possibleNonActualPath = handleBinaryArrayExpression(op2, operator, op1);
        }
      }
      if (possibleNonActualPath != null) {
        if (possibleNonActualPath.isActualPath()) {
          return possibleNonActualPath;
        }
        if (otherStates != null) {
          SMGState smgState = AbstractStates.extractStateByType(otherStates, SMGState.class);
          if (smgState != null) {
            Set<AccessPath> targets =
                smgState.parsePointToFromMemoryLocationToAccessPath(possibleNonActualPath);
            if (!targets.isEmpty()) {
              return targets.iterator().next();
            }
          }
        }
      }
    }
    return null;
  }

  private AccessPath handleBinaryPointerExpression(
      CExpression pointer, CPointerType
      pointerType, BinaryOperator operator, CExpression index)
      throws UnrecognizedCCodeException {
    // first, we should add an dereference before pointer expression
    // NOTE: for array expression arr, *arr should be parsed as arr[0]
    CPointerExpression pointerExp = new CPointerExpression(FileLocation.DUMMY, pointerType
        .getType(), pointer);
    AccessPath prePath = continueOnLHS(pointerExp);
    composeMode = true;
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
            throw new AssertionError("Unsupported binary operator in pointer expression");
        }
        // TODO: an known issue, a[4][-5], which should be regularized as a[3][5] if the first
        // dimension has the length 10
        return composeArrayWithIndex(prePath, indexValue);
      } else {
        // TODO: add a branch for strengthening with value CPA to derive the concrete value
        refuseDereference = true;
        return composeArrayWithNoIndex(prePath);
      }
    }
    return null;
  }

  private AccessPath handleBinaryArrayExpression(
      CExpression array,
      BinaryOperator operator, CExpression index)
      throws UnrecognizedCCodeException {
    AccessPath prePath;
    if (array instanceof CLeftHandSide) {
      prePath = continueOnLHS((CLeftHandSide) array);
    } else {
      prePath = handleNonLHS(array);
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
        // TODO: regularize index value
        return composeArrayWithIndex(prePath, indexValue);
      } else {
        // TODO: derive concrete value with value CPA
        refuseDereference = true;
        return composeArrayWithNoIndex(prePath);
      }
    }
    return null;
  }

  private AccessPath composeArrayWithIndex(AccessPath path, long indexValue) {
    AccessPath newPath = AccessPath.copyOf(path);
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
      } else if (lastSegment instanceof ArrayAccessSegment) {
        // this index is absorbed
      } else {
        newPath.appendSegment(new ArrayConstIndexSegment(indexValue));
      }
    } else {
      newPath.appendSegment(new ArrayConstIndexSegment(indexValue));
    }
    return newPath;
  }

  private AccessPath composeArrayWithNoIndex(AccessPath path) {
    AccessPath newPath = AccessPath.copyOf(path);
    if (newPath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
      newPath.removeLastSegment();
    }
    if (composeMode) {
      PathSegment lastSegment = newPath.getLastSegment();
      if (lastSegment instanceof ArrayConstIndexSegment ||
          lastSegment instanceof ArrayAccessSegment) {
        newPath.removeLastSegment();
        newPath.appendSegment(new ArrayAccessSegment());
      } else {
        newPath.appendSegment(new ArrayAccessSegment());
      }
    } else {
      newPath.appendSegment(new ArrayAccessSegment());
    }
    return newPath;
  }

}
