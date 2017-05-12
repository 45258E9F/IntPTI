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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

/**
 * This class tracks conditions under which a memory-related error would occur in the program.
 */
public class ErrorConditions {

  private final BooleanFormulaManagerView bfmgr;

  private BooleanFormula invalidDeref;
  private BooleanFormula invalidFree;

  public ErrorConditions(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
    invalidDeref = bfmgr.makeBoolean(false);
    invalidFree = bfmgr.makeBoolean(false);
  }

  public boolean isEnabled() {
    return true;
  }

  public void addInvalidDerefCondition(BooleanFormula pCo) {
    invalidDeref = bfmgr.or(invalidDeref, pCo);
  }

  public void addInvalidFreeCondition(BooleanFormula pCo) {
    invalidFree = bfmgr.or(invalidFree, pCo);
  }

  public BooleanFormula getInvalidDerefCondition() {
    return invalidDeref;
  }

  public BooleanFormula getInvalidFreeCondition() {
    return invalidFree;
  }

  public static ErrorConditions dummyInstance(BooleanFormulaManagerView pBfmgr) {
    return new DummyErrorConditions(pBfmgr);
  }

  private static class DummyErrorConditions extends ErrorConditions {

    public DummyErrorConditions(BooleanFormulaManagerView pBfmgr) {
      super(pBfmgr);
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    public void addInvalidDerefCondition(BooleanFormula pCo) {
      // disabled
    }

    @Override
    public void addInvalidFreeCondition(BooleanFormula pCo) {
      // disabled
    }
  }
}