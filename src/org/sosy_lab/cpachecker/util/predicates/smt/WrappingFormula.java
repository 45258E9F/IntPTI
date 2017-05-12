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
package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;


abstract class WrappingFormula<TWrap extends Formula, TOut extends Formula> {

  private final TWrap wrapped;
  private final FormulaType<TOut> type;

  private WrappingFormula(FormulaType<TOut> pType, TWrap pWrapped) {
    wrapped = checkNotNull(pWrapped);
    type = checkNotNull(pType);
  }

  final TWrap getWrapped() {
    return wrapped;
  }

  final FormulaType<TOut> getType() {
    return type;
  }

  @Override
  public final String toString() {
    return wrapped.toString();
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    return (prime + type.hashCode()) * prime + wrapped.hashCode();
  }

  @Override
  public final boolean equals(Object pObj) {
    if ((pObj == null)
        || !getClass().equals(pObj.getClass())) {
      return false;
    }

    WrappingFormula<?, ?> other = (WrappingFormula<?, ?>) pObj;

    return wrapped.equals(other.wrapped)
        && type.equals(other.type);
  }

  static final class WrappingBitvectorFormula<TWrap extends Formula>
      extends WrappingFormula<TWrap, BitvectorFormula>
      implements BitvectorFormula {

    WrappingBitvectorFormula(FormulaType<BitvectorFormula> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }

  static final class WrappingFloatingPointFormula<TWrap extends Formula>
      extends WrappingFormula<TWrap, FloatingPointFormula>
      implements FloatingPointFormula {

    WrappingFloatingPointFormula(FormulaType<FloatingPointFormula> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }

  static final class WrappingArrayFormula<TWrap extends Formula, TI extends Formula, TE extends Formula>
      extends WrappingFormula<TWrap, ArrayFormula<TI, TE>>
      implements ArrayFormula<TI, TE> {

    WrappingArrayFormula(FormulaType<ArrayFormula<TI, TE>> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }
}
