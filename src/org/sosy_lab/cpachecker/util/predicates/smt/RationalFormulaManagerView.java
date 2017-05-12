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

import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.api.NumeralFormula.RationalFormula;
import org.sosy_lab.solver.api.NumeralFormulaManager;
import org.sosy_lab.solver.api.RationalFormulaManager;

public class RationalFormulaManagerView
    extends NumeralFormulaManagerView<NumeralFormula, RationalFormula>
    implements RationalFormulaManager {
  RationalFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<NumeralFormula, RationalFormula> pManager) {
    super(pWrappingHandler, pManager);
  }
}
