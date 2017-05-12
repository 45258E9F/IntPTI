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


public class ContainsOnlyEnvInfoVisitor<T> extends DefaultNumeralFormulaVisitor<T, Boolean>
    implements BooleanFormulaVisitor<T, Boolean> {

  private final CollectVarsVisitor<T> collectVarsVisitor = new CollectVarsVisitor<>();

  @Override
  public Boolean visit(Equal<T> pEqual) {
    return pEqual.accept(collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan) {
    return pLessThan.accept(collectVarsVisitor).size() == 1;
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd) {
    return pAnd.getOperand1().accept(this) && pAnd.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Boolean visitFalse() {
    return true;
  }

  @Override
  public Boolean visitTrue() {
    return true;
  }

  @Override
  public Boolean visit(Union<T> pUnion) {
    return pUnion.getOperand1().accept(this)
        && pUnion.getOperand2().accept(this);
  }

  @Override
  protected Boolean visitDefault(NumeralFormula<T> pFormula) {
    return false;
  }

}
