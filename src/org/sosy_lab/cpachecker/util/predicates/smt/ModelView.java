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

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.NumeralFormula.RationalFormula;

import java.math.BigInteger;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * Wrapping for models.
 */
public class ModelView implements Model {
  private final Model delegate;
  private final FormulaWrappingHandler wrappingHandler;

  public ModelView(Model pDelegate, FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Nullable
  private Object evaluateImpl(Formula f) {
    return delegate.evaluate(
        wrappingHandler.unwrap(f)
    );
  }

  @Nullable
  @Override
  public Object evaluate(Formula f) {
    return evaluateImpl(f);
  }

  @Nullable
  @Override
  public BigInteger evaluate(IntegerFormula f) {
    return (BigInteger) evaluateImpl(f);
  }

  @Nullable
  @Override
  public Rational evaluate(RationalFormula f) {
    return (Rational) evaluateImpl(f);
  }

  @Nullable
  @Override
  public Boolean evaluate(BooleanFormula f) {
    return (Boolean) evaluateImpl(f);
  }

  @Nullable
  @Override
  public BigInteger evaluate(BitvectorFormula f) {
    return (BigInteger) evaluateImpl(f);
  }

  @Override
  public Iterator<ValueAssignment> iterator() {
    return delegate.iterator();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
