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
package org.sosy_lab.cpachecker.core.summary.instance.arith;

import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.core.summary.apply.AbstractFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearConstraint;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearVariable;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;

import java.util.Collections;
import java.util.List;

import apron.Abstract0;
import apron.Lincons0;

/**
 * Arithmetic relation of variables
 */
public class ArithFunctionSummaryInstance
    extends AbstractFunctionSummaryInstance<List<LinearConstraint<Integer, LinearVariable>>> {

  private ApronState apronState;
  private List<LinearConstraint<Integer, LinearVariable>> linearConstraints = Lists.newArrayList();

  private ArithFunctionSummaryInstance(String function) {
    super(function);
  }

  /*
   * build an arithsummaryInstance from apronState, we can get it from abstract0
   */
  private ArithFunctionSummaryInstance(String function, ApronState state) {
    super(function);
    this.apronState = state;
    storeLinearConstraints();
  }

  private void storeLinearConstraints() {
    Abstract0 abstract0 = apronState.getApronNativeState();
    Lincons0[] linearCons = abstract0.toLincons(abstract0.getCreationManager());
    for (Lincons0 lincons : linearCons) {
      linearConstraints.add(LinearConstraint.fromApronConstraint(apronState, lincons));
    }
  }

  @Override
  public boolean isEqualTo(SummaryInstance pThat) {
    if (pThat instanceof ArithFunctionSummaryInstance) {
      return false;
    } else {
      ArithFunctionSummaryInstance that = (ArithFunctionSummaryInstance) pThat;
      if (this.apronState == that.apronState) {
        return true;
      } else if (this.apronState == null || that.apronState == null) {
        return false;
      } else {
        return this.apronState.getLessOrEquals(that.apronState) &&
            that.apronState.getLessOrEquals(this.apronState);
      }
    }
  }

  public static ArithFunctionSummaryInstance of(String function) {
    return new ArithFunctionSummaryInstance(function);
  }

  public static ArithFunctionSummaryInstance from(String function, ApronState apronState) {
    return new ArithFunctionSummaryInstance(function, apronState);
  }

  @Override
  public List<LinearConstraint<Integer, LinearVariable>> apply() {
    return Collections.unmodifiableList(linearConstraints);
  }

}
