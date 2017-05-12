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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.ArrayList;
import java.util.List;

/**
 * This class tracks constraints which are created during AST traversal but
 * cannot be applied at the time of creation.
 */
public class Constraints {

  private final BooleanFormulaManagerView bfmgr;

  private final List<BooleanFormula> constraints = new ArrayList<>();

  public Constraints(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }

  public void addConstraint(BooleanFormula pCo) {
    constraints.add(pCo);
  }

  public BooleanFormula get() {
    return bfmgr.and(constraints);
  }
}