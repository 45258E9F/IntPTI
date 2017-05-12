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
package org.sosy_lab.cpachecker.core.summary.instance.arith.ast;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.List;

import apron.Coeff;
import apron.Lincons0;
import apron.Linterm0;
import gmp.Mpq;

/**
 * Linear constraint in the form of
 *
 * C' V [predicate] constant
 *
 * e.g., x - y = 1
 *
 * @param <C> the type of coeffients and constant
 * @param <V> the type of variable
 */
@SuppressWarnings("javadoc")
public class LinearConstraint<C, V> {
  public static enum Predicate {
    EQ,
    GT,
    GE,
  }

  private List<V> variables = Lists.newArrayList();
  private List<C> coefficients = Lists.newArrayList();
  private C constant;
  private Predicate predicate;

  public LinearConstraint() {
    variables = Lists.newArrayList();
    coefficients = Lists.newArrayList();
    constant = null;
    predicate = Predicate.EQ;
  }

  public LinearConstraint(
      Predicate pPredicate,
      List<V> pVariables, List<C> pCoefficients, C pConstant) {
    predicate = pPredicate;
    variables = pVariables;
    coefficients = pCoefficients;
    constant = pConstant;
  }

  public int terms() {
    return coefficients.size();
  }

  public LinearConstraint<C, V> append(C c, V v) {
    coefficients.add(c);
    variables.add(v);
    return this;
  }

  public List<C> getCoefficients() {
    return coefficients;
  }

  public LinearConstraint<C, V> setCoefficients(List<C> pCoefficients) {
    coefficients = pCoefficients;
    return this;
  }

  public C getConstant() {
    return constant;
  }

  public LinearConstraint<C, V> setConstant(C pConstant) {
    constant = pConstant;
    return this;
  }

  public Predicate getPredicate() {
    return predicate;
  }

  public LinearConstraint<C, V> setPredicate(Predicate pComparator) {
    predicate = pComparator;
    return this;
  }

  public List<V> getVariables() {
    return variables;
  }


  public LinearConstraint<C, V> setVariables(List<V> pVariables) {
    variables = pVariables;
    return this;
  }


  public static LinearConstraint<Integer, LinearVariable> fromApronConstraint(
      ApronState apronState,
      Lincons0 lincons) {
//    1. get comparator
    LinearConstraint<Integer, LinearVariable> linearConstraint = new LinearConstraint<>();
    Predicate predicate = Predicate.EQ;
    switch (lincons.kind) {
      case Lincons0.EQ:
        predicate = Predicate.EQ;
        break;
      case Lincons0.SUPEQ:
        predicate = Predicate.GE;
        break;
      case Lincons0.SUP:
        predicate = Predicate.GT;
        break;
      default:
        break;
    }
    linearConstraint.setPredicate(predicate);

//    2. get constant
    Coeff constant = lincons.expr.getCst();
    Mpq cst = new Mpq();
    constant.inf().toMpq(cst, 0);
    int intConstant = cst.getNum().bigIntegerValue().intValue();
    intConstant = -intConstant;
    linearConstraint.setConstant(intConstant);


//    3. get variable and get coefficient
    Linterm0[] linterms = lincons.expr.getLinterms();

    for (Linterm0 linterm : linterms) {
      int dimension = linterm.getDimension();
      Coeff coeff = linterm.getCoefficient();
      if (coeff.isZero()) {
        continue;
      }
      MemoryLocation memoryLocation = apronState.getVariableMemoryLocation(dimension);
      LinearVariable linearVariable = LinearVariable.of(memoryLocation, apronState);
      Mpq mpq = new Mpq();
      coeff.inf().toMpq(mpq, 0);
      linearConstraint.addTerm(mpq.getNum().bigIntegerValue().intValue(), linearVariable);
    }
    return linearConstraint;
  }

  public void addTerm(C coeff, V variable) {
    this.coefficients.add(coeff);
    this.variables.add(variable);
  }

  public C getCoefficient(int i) {
    return coefficients.get(i);
  }

  public V getVariable(int i) {
    return variables.get(i);
  }

  /**
   * Substitute variable from V1 to V2 with a mapping
   * for instance, translate: x - y = 1 to SYM1 - SYM2 = 1
   */
  public static <C, V1, V2> LinearConstraint<C, V2> substitute(
      LinearConstraint<C, V1> cons,
      Function<V1, V2> map) {
    List<V2> newVariables = Lists.newArrayList(
        FluentIterable.from(cons.getVariables()).transform(map)
    );
    return new LinearConstraint<>(
        cons.getPredicate(),
        newVariables,
        Lists.newArrayList(cons.getCoefficients()),
        copyNumeric(cons.getConstant())
    );
  }

  /**
   * We assume the only numeric types that are used as coefficients are int, float, double
   */
  @SuppressWarnings("unchecked")
  private static <C> C copyNumeric(C c) {
    if (c instanceof Integer) {
      return (C) Integer.valueOf((Integer) c);
    } else if (c instanceof Float) {
      return (C) Float.valueOf((Float) c);
    } else if (c instanceof Double) {
      return (C) Double.valueOf((Double) c);
    }
    throw new RuntimeException("Unsupported coeffient type");
  }

  @Override
  public String toString() {
    String result = "";
    result += variables.toString();
    result += coefficients.toString();
    switch (predicate) {
      case EQ:
        result += "=";
        break;
      case GT:
        result += ">";
        break;
      case GE:
        result += ">=";
        break;
      default:
        break;
    }
    result += constant;
    return result;

  }
}
