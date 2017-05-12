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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Set;

/**
 * This Class contains functions,
 * that convert operators from C-source into CPAchecker-format.
 */
class ASTOperatorConverter {

  private final Function<String, String> niceFileNameFunction;

  ASTOperatorConverter(Function<String, String> pNiceFileNameFunction) {
    niceFileNameFunction = pNiceFileNameFunction;
  }

  /**
   * converts and returns the operator of an unaryExpression
   * (PLUS, MINUS, NOT, STAR,...)
   */
  UnaryOperator convertUnaryOperator(final IASTUnaryExpression e) {
    switch (e.getOperator()) {
      case IASTUnaryExpression.op_amper:
        return UnaryOperator.AMPER;
      case IASTUnaryExpression.op_minus:
        return UnaryOperator.MINUS;
      case IASTUnaryExpression.op_sizeof:
        return UnaryOperator.SIZEOF;
      case IASTUnaryExpression.op_star:
        throw new IllegalArgumentException(
            "For the star operator, CPointerExpression should be used instead of CUnaryExpression with a star operator.");
      case IASTUnaryExpression.op_tilde:
        return UnaryOperator.TILDE;
      case IASTUnaryExpression.op_alignOf:
        return UnaryOperator.ALIGNOF;
      default:
        throw new CFAGenerationRuntimeException("Unknown unary operator", e, niceFileNameFunction);
    }
  }

  /**
   * converts and returns the operator of an binaryExpression
   * (PLUS, MINUS, MULTIPLY,...) with an flag, if the operator causes an assignment.
   */
  Pair<BinaryOperator, Boolean> convertBinaryOperator(final IASTBinaryExpression e) {
    boolean isAssign = false;
    final BinaryOperator operator;

    switch (e.getOperator()) {
      case IASTBinaryExpression.op_multiply:
        operator = BinaryOperator.MULTIPLY;
        break;
      case IASTBinaryExpression.op_divide:
        operator = BinaryOperator.DIVIDE;
        break;
      case IASTBinaryExpression.op_modulo:
        operator = BinaryOperator.MODULO;
        break;
      case IASTBinaryExpression.op_plus:
        operator = BinaryOperator.PLUS;
        break;
      case IASTBinaryExpression.op_minus:
        operator = BinaryOperator.MINUS;
        break;
      case IASTBinaryExpression.op_shiftLeft:
        operator = BinaryOperator.SHIFT_LEFT;
        break;
      case IASTBinaryExpression.op_shiftRight:
        operator = BinaryOperator.SHIFT_RIGHT;
        break;
      case IASTBinaryExpression.op_lessThan:
        operator = BinaryOperator.LESS_THAN;
        break;
      case IASTBinaryExpression.op_greaterThan:
        operator = BinaryOperator.GREATER_THAN;
        break;
      case IASTBinaryExpression.op_lessEqual:
        operator = BinaryOperator.LESS_EQUAL;
        break;
      case IASTBinaryExpression.op_greaterEqual:
        operator = BinaryOperator.GREATER_EQUAL;
        break;
      case IASTBinaryExpression.op_binaryAnd:
        operator = BinaryOperator.BINARY_AND;
        break;
      case IASTBinaryExpression.op_binaryXor:
        operator = BinaryOperator.BINARY_XOR;
        break;
      case IASTBinaryExpression.op_binaryOr:
        operator = BinaryOperator.BINARY_OR;
        break;
      case IASTBinaryExpression.op_assign:
        operator = null;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_multiplyAssign:
        operator = BinaryOperator.MULTIPLY;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_divideAssign:
        operator = BinaryOperator.DIVIDE;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_moduloAssign:
        operator = BinaryOperator.MODULO;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_plusAssign:
        operator = BinaryOperator.PLUS;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_minusAssign:
        operator = BinaryOperator.MINUS;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_shiftLeftAssign:
        operator = BinaryOperator.SHIFT_LEFT;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_shiftRightAssign:
        operator = BinaryOperator.SHIFT_RIGHT;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_binaryAndAssign:
        operator = BinaryOperator.BINARY_AND;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_binaryXorAssign:
        operator = BinaryOperator.BINARY_XOR;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_binaryOrAssign:
        operator = BinaryOperator.BINARY_OR;
        isAssign = true;
        break;
      case IASTBinaryExpression.op_equals:
        operator = BinaryOperator.EQUALS;
        break;
      case IASTBinaryExpression.op_notequals:
        operator = BinaryOperator.NOT_EQUALS;
        break;
      default:
        throw new CFAGenerationRuntimeException("Unknown binary operator", e, niceFileNameFunction);
    }

    return Pair.of(operator, isAssign);
  }

  /**
   * converts and returns the operator of an idExpression
   * (alignOf, sizeOf,...)
   */
  TypeIdOperator convertTypeIdOperator(IASTTypeIdExpression e) {
    switch (e.getOperator()) {
      case IASTTypeIdExpression.op_alignof:
        return TypeIdOperator.ALIGNOF;
      case IASTTypeIdExpression.op_sizeof:
        return TypeIdOperator.SIZEOF;
      case IASTTypeIdExpression.op_typeid:
        return TypeIdOperator.TYPEID;
      case IASTTypeIdExpression.op_typeof:
        return TypeIdOperator.TYPEOF;
      default:
        throw new CFAGenerationRuntimeException("Unknown type id operator", e,
            niceFileNameFunction);
    }
  }

  private static final Set<BinaryOperator> BOOLEAN_BINARY_OPERATORS = ImmutableSet.of(
      BinaryOperator.EQUALS,
      BinaryOperator.NOT_EQUALS,
      BinaryOperator.GREATER_EQUAL,
      BinaryOperator.GREATER_THAN,
      BinaryOperator.LESS_EQUAL,
      BinaryOperator.LESS_THAN);

  static boolean isBooleanExpression(CExpression e) {
    if (e instanceof CBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((CBinaryExpression) e).getOperator());

    } else {
      return false;
    }
  }
}
