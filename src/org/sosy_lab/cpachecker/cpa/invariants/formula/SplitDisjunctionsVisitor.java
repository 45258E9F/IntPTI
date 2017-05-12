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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Instances of this class are invariants formula visitors used to split the
 * visited formulae on their outer conjunctions into lists of the conjunction
 * operands. This is done recursively so that nested conjunctions are split
 * as well.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class SplitDisjunctionsVisitor<T>
    implements BooleanFormulaVisitor<T, List<BooleanFormula<T>>> {

  @Override
  public List<BooleanFormula<T>> visit(Equal<T> pEqual) {
    return Collections.<BooleanFormula<T>>singletonList(pEqual);
  }

  @Override
  public List<BooleanFormula<T>> visit(LessThan<T> pLessThan) {
    return Collections.<BooleanFormula<T>>singletonList(pLessThan);
  }

  @Override
  public List<BooleanFormula<T>> visit(LogicalAnd<T> pAnd) {
    return Collections.<BooleanFormula<T>>singletonList(pAnd);
  }

  @Override
  public List<BooleanFormula<T>> visit(LogicalNot<T> pNot) {
    if (pNot.getNegated() instanceof LogicalAnd<?>) {
      List<BooleanFormula<T>> result = new ArrayList<>();
      LogicalAnd<T> formula = (LogicalAnd<T>) pNot.getNegated();
      BooleanFormula<T> term = formula.getOperand1();
      if (!(term instanceof LogicalNot<?>)) {
        term = LogicalNot.of(term);
      } else {
        term = ((LogicalNot<T>) term).getNegated();
      }
      result.addAll(term.accept(this));
      // If the left operand is true, return true
      if (result.contains(BooleanConstant.<T>getTrue())) {
        return visitTrue();
      }
      term = formula.getOperand2();
      if (!(term instanceof LogicalNot<?>)) {
        term = LogicalNot.of(term);
      } else {
        term = ((LogicalNot<T>) term).getNegated();
      }
      Collection<BooleanFormula<T>> toAdd = term.accept(this);
      // If the right operand is true, return true
      if (toAdd.contains(BooleanConstant.<T>getTrue())) {
        return visitTrue();
      }
      result.addAll(toAdd);
      return result;
    }
    return Collections.<BooleanFormula<T>>singletonList(pNot);
  }

  @Override
  public List<BooleanFormula<T>> visitFalse() {
    return Collections.<BooleanFormula<T>>emptyList();
  }

  @Override
  public List<BooleanFormula<T>> visitTrue() {
    return Collections.<BooleanFormula<T>>singletonList(BooleanConstant.<T>getTrue());
  }

}
