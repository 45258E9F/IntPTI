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

import org.sosy_lab.solver.api.IntegerFormulaManager;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.NumeralFormulaManager;

public class IntegerFormulaManagerView
    extends NumeralFormulaManagerView<IntegerFormula, IntegerFormula>
    implements IntegerFormulaManager {
  IntegerFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<IntegerFormula, IntegerFormula> pManager) {
    super(pWrappingHandler, pManager);
  }
}
