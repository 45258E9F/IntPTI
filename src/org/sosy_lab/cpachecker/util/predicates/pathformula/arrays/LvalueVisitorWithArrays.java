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
package org.sosy_lab.cpachecker.util.predicates.pathformula.arrays;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.LvalueVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.solver.api.Formula;


public class LvalueVisitorWithArrays extends LvalueVisitor {

  LvalueVisitorWithArrays(
      CtoFormulaConverter pConv, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts, Constraints pConstraints, ErrorConditions pErrorConditions) {
    super(pConv, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions);
  }

  @Override
  public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
    return super.visit(pE);
  }
}
