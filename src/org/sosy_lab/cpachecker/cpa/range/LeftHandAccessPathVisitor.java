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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.Pointer2State;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * This visitor computes access path for left-hand-side expressions.
 * In particular, if an array expression has uncertain index, no valid access path will be generated
 *
 * NOTE: since path visitor has state, it should be used for only once and then be recycled.
 */
public final class LeftHandAccessPathVisitor implements CLeftHandSideVisitor<Optional<AccessPath>,
    UnrecognizedCCodeException> {

  private final ExpressionRangeVisitor rangeVisitor;

  /**
   * Other components of abstract states, used for strengthen
   */
  private List<AbstractState> otherStates = null;

  /**
   * If this flag is ON, then we compose two array indexes as one.
   * For example, a[3] and [4..6] is composed into a[7..9]
   *
   * NOTE: this flag can be set to TRUE or FALSE without limitation.
   */
  private boolean composeMode = false;

  /**
   * If this flag is ON, then we stop generation of access path because we do not know what the
   * exact memory location that this pointer points to.
   *
   * NOTE: once this flag becomes TRUE, it cannot be set to FALSE again!
   */
  private boolean refuseDereference = false;

  public LeftHandAccessPathVisitor(ExpressionRangeVisitor pVisitor) {
    rangeVisitor = pVisitor;
    otherStates = pVisitor.getOtherStates();
  }

  @Override
  public Optional<AccessPath> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCCodeException {
    CExpression array = pIastArraySubscriptExpression.getArrayExpression();
    AccessPath arrayPath;
    if (array instanceof CLeftHandSide) {
      arrayPath = ((CLeftHandSide) array).accept(this).orNull();
    } else {
      arrayPath = handleNonLeftValue(array).orNull();
    }
    if (arrayPath == null) {
      return Optional.absent();
    }
    AccessPath resultPath = null;
    CPointerType pointerType = Types.extractPointerType(array.getExpressionType());
    if (pointerType != null) {
      // then, the array subscript is equivalent to a pointer dereference operation
      if (refuseDereference) {
        return Optional.absent();
      }
      // examine if the last segment is address-of
      if (arrayPath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        arrayPath.removeLastSegment();
        resultPath = arrayPath;
      } else {
        composeArrayIndexSegments(arrayPath, new ArrayConstIndexSegment(0));
        if (otherStates != null) {
          // strengthen with other pointer/alias/shape analyses
          AbstractState pointerState = AbstractStates.extractStateByTypes(otherStates, ShapeState
              .class, Pointer2State.class);
          if (pointerState instanceof ShapeState) {
            // The remaining segments after uncertain array segment could not contain dereference
            // segment, since `refuseDereference` flag works.
            List<PathSegment> remains = splitAccessPath(arrayPath);
            Set<AccessPath> targets = ((ShapeState) pointerState).getPointsToTargetForAccessPath
                (arrayPath);
            if (!targets.isEmpty()) {
              // we only pick one of the resultant access path, thus the analysis precision may lose
              resultPath = targets.iterator().next();
              for (PathSegment r : remains) {
                // thus, we can directly append remaining segments
                resultPath.appendSegment(r);
              }
            }
          } else if (pointerState instanceof Pointer2State) {
            List<PathSegment> remains = splitAccessPath(arrayPath);
            Set<AccessPath> targets = ((Pointer2State) pointerState)
                .getPointsToTargetForAccessPath(arrayPath, rangeVisitor.getMachineModel());
            if (!targets.isEmpty()) {
              resultPath = targets.iterator().next();
              for (PathSegment r : remains) {
                resultPath.appendSegment(r);
              }
            }
          }
        }
      }
      composeMode = true;
    } else {
      resultPath = arrayPath;
    }
    if (resultPath != null) {
      CExpression index = pIastArraySubscriptExpression.getSubscriptExpression();
      Range indexRange = index.accept(rangeVisitor).compress();
      CompInteger integerNum = indexRange.numOfIntegers();
      if (integerNum.equals(CompInteger.ZERO)) {
        return Optional.absent();
      } else if (!integerNum.equals(CompInteger.ONE)) {
        refuseDereference = true;
        composeArrayIndexSegments(resultPath, new ArrayUncertainIndexSegment(indexRange));
      } else {
        Long concreteIndex = indexRange.getLow().longValue();
        if (concreteIndex == null) {
          return Optional.absent();
        }
        composeArrayIndexSegments(resultPath, new ArrayConstIndexSegment(concreteIndex));
      }
      composeMode = false;
      return Optional.of(resultPath);
    }
    return Optional.absent();
  }

  @Override
  public Optional<AccessPath> visit(CFieldReference pIastFieldReference)
      throws UnrecognizedCCodeException {
    CExpression fieldOwner = pIastFieldReference.getFieldOwner();
    AccessPath ownerPath;
    boolean containsPtr = false;
    if (fieldOwner instanceof CLeftHandSide) {
      ownerPath = ((CLeftHandSide) fieldOwner).accept(this).orNull();
      if (ownerPath == null) {
        return Optional.absent();
      }
      if (pIastFieldReference.isPointerDereference()) {
        if (refuseDereference) {
          return Optional.absent();
        }
        ownerPath.appendSegment(new PointerDereferenceSegment());
        containsPtr = true;
      }
    } else if (pIastFieldReference.isPointerDereference()) {
      // the star operation is reduced if the owner is a non-LHS
      ownerPath = handleNonLeftValue(fieldOwner).orNull();
      if (ownerPath == null) {
        return Optional.absent();
      }
      if (ownerPath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        ownerPath.removeLastSegment();
      }
    } else {
      // that is impossible
      return Optional.absent();
    }
    AccessPath resultPath = null;
    if (containsPtr) {
      if (otherStates != null) {
        AbstractState pointerState = AbstractStates.extractStateByTypes(otherStates, ShapeState
            .class, Pointer2State.class);
        if (pointerState instanceof ShapeState) {
          List<PathSegment> remains = splitAccessPath(ownerPath);
          Set<AccessPath> targets = ((ShapeState) pointerState).getPointsToTargetForAccessPath
              (ownerPath);
          if (!targets.isEmpty()) {
            resultPath = targets.iterator().next();
            for (PathSegment r : remains) {
              resultPath.appendSegment(r);
            }
          }
        } else if (pointerState instanceof Pointer2State) {
          List<PathSegment> remains = splitAccessPath(ownerPath);
          Set<AccessPath> targets = ((Pointer2State) pointerState)
              .getPointsToTargetForAccessPath(ownerPath, rangeVisitor.getMachineModel());
          if (!targets.isEmpty()) {
            resultPath = targets.iterator().next();
            for (PathSegment r : remains) {
              resultPath.appendSegment(r);
            }
          }
        }
      }
    } else {
      resultPath = ownerPath;
    }
    composeMode = false;
    if (resultPath != null) {
      resultPath.appendSegment(new FieldAccessSegment(pIastFieldReference.getFieldName()));
      return Optional.of(resultPath);
    }
    return Optional.absent();
  }

  @Override
  public Optional<AccessPath> visit(CIdExpression pIastIdExpression)
      throws UnrecognizedCCodeException {
    composeMode = false;
    AccessPath ap = new AccessPath(pIastIdExpression.getDeclaration());
    return Optional.of(ap);
  }

  @Override
  public Optional<AccessPath> visit(CPointerExpression pointerExpression)
      throws UnrecognizedCCodeException {
    CExpression refExpression = pointerExpression.getOperand();
    AccessPath refPath;
    boolean containsPtr = false;
    if (refExpression instanceof CLeftHandSide) {
      refPath = ((CLeftHandSide) refExpression).accept(this).orNull();
      if (refPath == null) {
        return Optional.absent();
      }
      if (refuseDereference) {
        return Optional.absent();
      }
      refPath.appendSegment(new PointerDereferenceSegment());
      containsPtr = true;
    } else {
      refPath = handleNonLeftValue(refExpression).orNull();
      if (refPath == null) {
        return Optional.absent();
      }
      if (refPath.getLastSegment().equals(AddressingSegment.INSTANCE)) {
        refPath.removeLastSegment();
      }
    }
    composeMode = false;
    // use pointer analysis info.
    if (containsPtr) {
      AccessPath result = null;
      if (otherStates != null) {
        AbstractState pointerState = AbstractStates.extractStateByTypes(otherStates, ShapeState
            .class, Pointer2State.class);
        if (pointerState instanceof ShapeState) {
          List<PathSegment> remains = splitAccessPath(refPath);
          Set<AccessPath> targets = ((ShapeState) pointerState).getPointsToTargetForAccessPath
              (refPath);
          if (!targets.isEmpty()) {
            result = targets.iterator().next();
            for (PathSegment r : remains) {
              result.appendSegment(r);
            }
          }
        } else if (pointerState instanceof Pointer2State) {
          List<PathSegment> remains = splitAccessPath(refPath);
          Set<AccessPath> targets = ((Pointer2State) pointerState)
              .getPointsToTargetForAccessPath(refPath, rangeVisitor.getMachineModel());
          if (!targets.isEmpty()) {
            result = targets.iterator().next();
            for (PathSegment r : remains) {
              result.appendSegment(r);
            }
          }
        }
      }
      return (result == null) ? Optional.<AccessPath>absent() : Optional.of(result);
    } else {
      return Optional.of(refPath);
    }
  }

  /**
   * Compute actual path or actual path plus a tail & (address-of) for non-LHS.
   * Note: this method is used for handling non-LHS inside pointer reference.
   */
  private Optional<AccessPath> handleNonLeftValue(CExpression expression)
      throws UnrecognizedCCodeException {
    if (expression instanceof CUnaryExpression) {
      UnaryOperator operator = ((CUnaryExpression) expression).getOperator();
      if (operator == UnaryOperator.AMPER) {
        CExpression operand = ((CUnaryExpression) expression).getOperand();
        assert (operand instanceof CLeftHandSide);
        AccessPath prePath = ((CLeftHandSide) operand).accept(this).orNull();
        if (prePath != null) {
          composeMode = true;
          prePath.appendSegment(new AddressingSegment());
          // actual path + address-of operation (&)
          return Optional.of(prePath);
        }
        return Optional.absent();
      }
    } else if (expression instanceof CBinaryExpression) {
      CBinaryExpression binExp = (CBinaryExpression) expression;
      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();
      BinaryOperator operator = binExp.getOperator();
      CType type1 = op1.getExpressionType();
      CType type2 = op2.getExpressionType();
      CPointerType pointerType1 = Types.extractPointerType(type1);
      CPointerType pointerType2 = Types.extractPointerType(type2);
      CArrayType arrayType1 = Types.extractArrayType(type1);
      CArrayType arrayType2 = Types.extractArrayType(type2);
      AccessPath actualPath = null;
      if (pointerType1 != null && Types.isNumericalType(type2)) {
        actualPath = handleBinaryPointerExpression(op1, pointerType1, operator, op2);
      } else if (arrayType1 != null && Types.isNumericalType(type2)) {
        actualPath = handleBinaryArrayExpression(op1, operator, op2);
      } else if (pointerType2 != null && Types.isNumericalType(type1)) {
        if (operator == BinaryOperator.PLUS) {
          actualPath = handleBinaryPointerExpression(op2, pointerType2, operator, op1);
        }
      } else if (arrayType2 != null && Types.isNumericalType(type1)) {
        if (operator == BinaryOperator.PLUS) {
          actualPath = handleBinaryArrayExpression(op2, operator, op1);
        }
      }
      if (actualPath != null) {
        return Optional.of(actualPath);
      }
    }
    // other cases
    return Optional.absent();
  }

  private AccessPath handleBinaryPointerExpression(
      CExpression pointer,
      CPointerType pointerType,
      BinaryOperator operator,
      CExpression index)
      throws UnrecognizedCCodeException {
    // if pointer contains undetermined array index, it would be refused in handling pointer
    // expression
    CPointerExpression pointerExp = new CPointerExpression(FileLocation.DUMMY, pointerType
        .getType(), pointer);
    // prePath is an actual path
    AccessPath prePath = pointerExp.accept(this).orNull();
    composeMode = true;
    if (prePath == null) {
      return null;
    }
    Range indexRange = index.accept(rangeVisitor).compress();
    switch (operator) {
      case PLUS:
        break;
      case MINUS:
        indexRange = indexRange.negate();
        break;
      default:
        throw new AssertionError("Unsupported binary operator in pointer expression");
    }
    CompInteger integerNum = indexRange.numOfIntegers();
    if (integerNum.equals(CompInteger.ZERO)) {
      return null;
    } else if (!integerNum.equals(CompInteger.ONE)) {
      refuseDereference = true;
      composeArrayIndexSegments(prePath, new ArrayUncertainIndexSegment(indexRange));
    } else {
      Long concreteIndex = indexRange.getLow().longValue();
      if (concreteIndex == null) {
        return null;
      }
      composeArrayIndexSegments(prePath, new ArrayConstIndexSegment(concreteIndex));
    }
    // After composing index to an actual path, the result is still an actual path.
    // Therefore we directly return this path.
    return prePath;
  }

  private AccessPath handleBinaryArrayExpression(
      CExpression array,
      BinaryOperator operator,
      CExpression index)
      throws UnrecognizedCCodeException {
    AccessPath prePath;
    if (array instanceof CLeftHandSide) {
      prePath = ((CLeftHandSide) array).accept(this).orNull();
    } else {
      prePath = handleNonLeftValue(array).orNull();
    }
    if (prePath == null) {
      return null;
    }
    composeArrayIndexSegments(prePath, new ArrayConstIndexSegment(0));
    composeMode = true;
    Range indexRange = index.accept(rangeVisitor).compress();
    switch (operator) {
      case PLUS:
        break;
      case MINUS:
        indexRange = indexRange.negate();
        break;
      default:
        throw new AssertionError("Unsupported binary operator in array expression");
    }
    CompInteger integerNum = indexRange.numOfIntegers();
    if (integerNum.equals(CompInteger.ZERO)) {
      return null;
    } else if (!integerNum.equals(CompInteger.ONE)) {
      refuseDereference = false;
      composeArrayIndexSegments(prePath, new ArrayUncertainIndexSegment(indexRange));
    } else {
      Long concreteIndex = indexRange.getLow().longValue();
      if (concreteIndex == null) {
        return null;
      }
      composeArrayIndexSegments(prePath, new ArrayConstIndexSegment(concreteIndex));
    }
    return prePath;
  }

  private void composeArrayIndexSegments(AccessPath path, ArrayConstIndexSegment indexSegment) {
    PathSegment lastSegment = path.getLastSegment();
    if (composeMode) {
      long topIndex = indexSegment.getIndex();
      if (lastSegment instanceof ArrayConstIndexSegment) {
        long lastIndex = ((ArrayConstIndexSegment) lastSegment).getIndex();
        ArrayConstIndexSegment newLastSegment = new ArrayConstIndexSegment(lastIndex + topIndex);
        path.removeLastSegment();
        path.appendSegment(newLastSegment);
      } else if (lastSegment instanceof ArrayUncertainIndexSegment) {
        Range lastIndex = ((ArrayUncertainIndexSegment) lastSegment).getIndexRange();
        lastIndex = lastIndex.plus(topIndex);
        ArrayUncertainIndexSegment newLastSegment = new ArrayUncertainIndexSegment(lastIndex);
        path.removeLastSegment();
        path.appendSegment(newLastSegment);
      } else {
        path.appendSegment(indexSegment);
      }
    } else {
      path.appendSegment(indexSegment);
    }
  }

  private void composeArrayIndexSegments(AccessPath path, ArrayUncertainIndexSegment indexSegment) {
    PathSegment lastSegment = path.getLastSegment();
    if (composeMode) {
      Range topIndex = indexSegment.getIndexRange();
      if (lastSegment instanceof ArrayUncertainIndexSegment) {
        Range lastIndex = ((ArrayUncertainIndexSegment) lastSegment).getIndexRange();
        lastIndex = lastIndex.plus(topIndex);
        ArrayUncertainIndexSegment newSegment = new ArrayUncertainIndexSegment(lastIndex);
        path.removeLastSegment();
        path.appendSegment(newSegment);
      } else if (lastSegment instanceof ArrayConstIndexSegment) {
        Long lastIndex = ((ArrayConstIndexSegment) lastSegment).getIndex();
        Range newIndex = topIndex.plus(lastIndex);
        ArrayUncertainIndexSegment newSegment = new ArrayUncertainIndexSegment(newIndex);
        path.removeLastSegment();
        path.appendSegment(newSegment);
      } else {
        path.appendSegment(indexSegment);
      }
    } else {
      path.appendSegment(indexSegment);
    }
  }

  @Override
  public Optional<AccessPath> visit(CComplexCastExpression complexCastExpression)
      throws UnrecognizedCCodeException {
    return Optional.absent();
  }

  Range computePointerDiff(AccessPath path1, AccessPath path2) {
    return Range.UNBOUND;
  }

  /**
   * For strengthen with shape analysis, we should process access path by pruning undetermined
   * array index segments.
   *
   * @param path an access path
   * @return a list of path segments which is the truncated parts of the original access path
   */
  private List<PathSegment> splitAccessPath(AccessPath path) {
    List<PathSegment> segments = path.path();
    Stack<PathSegment> remains = new Stack<>();
    int r = segments.size();
    for (PathSegment segment : segments) {
      if (segment instanceof ArrayUncertainIndexSegment) {
        break;
      }
      r--;
    }
    while (r > 0) {
      remains.push(path.getLastSegment());
      path.removeLastSegment();
      r--;
    }
    List<PathSegment> results = new ArrayList<>();
    while (!remains.isEmpty()) {
      results.add(remains.pop());
    }
    return results;
  }
}
