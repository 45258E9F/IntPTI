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

import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

import java.util.List;

/**
 * Abstract helper class that does nothing more than providing access
 * to the methods from {@link FormulaWrappingHandler} with less typing.
 */
abstract class BaseManagerView {

  private final FormulaWrappingHandler wrappingHandler;

  BaseManagerView(FormulaWrappingHandler pWrappingHandler) {
    wrappingHandler = pWrappingHandler;
  }

  final <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    return wrappingHandler.getFormulaType(pFormula);
  }

  final <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> targetType, T2 toWrap) {
    return wrappingHandler.wrap(targetType, toWrap);
  }

  public final Formula unwrap(Formula f) {
    return wrappingHandler.unwrap(f);
  }

  final List<Formula> unwrap(List<? extends Formula> f) {
    return wrappingHandler.unwrap(f);
  }

  final FormulaType<?> unwrapType(FormulaType<?> pType) {
    return wrappingHandler.unwrapType(pType);
  }

  final List<FormulaType<?>> unwrapType(List<? extends FormulaType<?>> pTypes) {
    return wrappingHandler.unwrapType(pTypes);
  }
}
