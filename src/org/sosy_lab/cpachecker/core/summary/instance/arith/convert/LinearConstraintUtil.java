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
package org.sosy_lab.cpachecker.core.summary.instance.arith.convert;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearConstraint;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearConstraint.Predicate;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearVariable;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SEs;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.util.KnownTypes;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;

import java.util.List;

/**
 * Utilities for LinearConstraint
 */
public class LinearConstraintUtil {

  private static CType longlongType = KnownTypes.createLongLongIntType();

  /**
   * Convert linear constraints to list of symbolic expression
   * Notice that the returned symbolic expression:
   * 1. does not have the original CExpression
   * 2. is liftted to longlong type
   */
  public static List<SymbolicExpression> convert(
      List<LinearConstraint<Integer, LinearVariable>> constraints,
      final SymbolicValueResolver resolver) {
    return Lists.transform(constraints,
        new Function<LinearConstraint<Integer, LinearVariable>, SymbolicExpression>() {
          @Override
          public SymbolicExpression apply(LinearConstraint<Integer, LinearVariable> lc) {
            return convert(lc, resolver);
          }
        });
  }

  /**
   * Convert a linear constraint to symbolic expression
   * Notice that the returned symbolic expression:
   * 1. does not have the original CExpression
   * 2. is liftted to longlong type
   */
  public static SymbolicExpression convert(
      LinearConstraint<Integer, LinearVariable> lc,
      final SymbolicValueResolver resolver) {
    // construct the linear combination
    SymbolicExpression lhs;
    if (lc.terms() == 0) {
      lhs = makeConstSE(0);
    } else {
      lhs = makeTermSE(lc.getCoefficient(0), lc.getVariable(0), resolver);
    }
    for (int i = 1; i < lc.terms(); i++) {
      SymbolicExpression addition = makeTermSE(lc.getCoefficient(i), lc.getVariable(i), resolver);
      lhs = SEs.makeBinary(lhs, addition, BinaryOperator.PLUS, longlongType);
    }
    // construct the rhs
    SymbolicExpression rhs = makeConstSE(lc.getConstant());
    // construct the whole formula
    BinaryOperator op = null;
    if (lc.getPredicate() == Predicate.EQ) {
      op = BinaryOperator.EQUALS;
    } else if (lc.getPredicate() == Predicate.GT) {
      op = BinaryOperator.GREATER_THAN;
    } else if (lc.getPredicate() == Predicate.GE) {
      op = BinaryOperator.GREATER_EQUAL;
    } else {
      throw new RuntimeException("unsupported predicate: " + lc.getPredicate());
    }
    return SEs.makeBinary(lhs, rhs, op, longlongType);
  }

  private static SymbolicExpression makeConstSE(Integer value) {
    return SEs.toConstant(KnownExplicitValue.valueOf(value), longlongType);
  }

  private static SymbolicExpression makeTermSE(
      Integer pCoefficient,
      LinearVariable pVariable,
      SymbolicValueResolver resolver) {
    if (pCoefficient == 1) {
      return SEs.toConstant(resolver.apply(pVariable), longlongType);
    } else {
      SymbolicExpression coe = SEs.toConstant(KnownExplicitValue.valueOf(pCoefficient),
          longlongType);
      SymbolicExpression var = SEs.toConstant(resolver.apply(pVariable), longlongType);
      return SEs.makeBinary(coe, var, BinaryOperator.MULTIPLY, longlongType);
    }
  }

}
