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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.IOException;
import java.io.Serializable;

/**
 * Abstract state for the Collector CPA. Encapsulate a
 * symbolic formula
 */
public class AssumptionStorageState implements AbstractState, Serializable {

  private static final long serialVersionUID = -3738604180058424317L;

  // this formula provides the assumption generated from other sources than heuristics,
  // e.g. assumptions for arithmetic overflow
  private transient final BooleanFormula assumption;

  // if a heuristic told us to stop the analysis, this formula provides the reason
  // if it is TRUE, there is no reason -> don't stop
  private transient final BooleanFormula stopFormula;

  private transient final FormulaManagerView fmgr;

  // the assumption represented by this class is always the conjunction of "assumption" and "stopFormula"

  public AssumptionStorageState(
      FormulaManagerView pFmgr,
      BooleanFormula pAssumption,
      BooleanFormula pStopFormula) {
    assumption = Preconditions.checkNotNull(pAssumption);
    stopFormula = Preconditions.checkNotNull(pStopFormula);
    fmgr = pFmgr;

    assert !fmgr.getBooleanFormulaManager().isFalse(
        assumption); // FALSE would mean "stop the analysis", but this should be signaled by stopFormula
  }

  public FormulaManagerView getFormulaManager() {
    return fmgr;
  }

  public BooleanFormula getAssumption() {
    return assumption;
  }

  public Appender getAssumptionAsString() {
    return fmgr.dumpFormula(assumption);
  }

  public boolean isAssumptionTrue() {
    return fmgr.getBooleanFormulaManager().isTrue(assumption);
  }

  public boolean isAssumptionFalse() {
    return fmgr.getBooleanFormulaManager().isFalse(assumption);
  }

  public BooleanFormula getStopFormula() {
    return stopFormula;
  }

  public boolean isStopFormulaTrue() {
    return fmgr.getBooleanFormulaManager().isTrue(stopFormula);
  }

  public boolean isStopFormulaFalse() {
    return fmgr.getBooleanFormulaManager().isFalse(stopFormula);
  }

  @Override
  public String toString() {
    return (fmgr.getBooleanFormulaManager().isTrue(stopFormula) ? "" : "<STOP> ") + "assume: ("
        + assumption + " & " + stopFormula + ")";
  }

  public boolean isStop() {
    return !fmgr.getBooleanFormulaManager().isTrue(stopFormula);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof AssumptionStorageState) {
      AssumptionStorageState otherElement = (AssumptionStorageState) other;
      return assumption.equals(otherElement.assumption)
          && stopFormula.equals(otherElement.stopFormula);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return assumption.hashCode() + 17 * stopFormula.hashCode();
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    Preconditions.checkState(isAssumptionTrue() && isStopFormulaTrue(),
        "Assumption and stop formula must be true for serialization to be correctly restored");
    out.defaultWriteObject();
  }

  private Object readResolve() {
    FormulaManagerView fmgr = GlobalInfo.getInstance().getAssumptionStorageFormulaManager();
    return new AssumptionStorageState(fmgr, fmgr.getBooleanFormulaManager().makeBoolean(true),
        fmgr.getBooleanFormulaManager().makeBoolean(true));
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
