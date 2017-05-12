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
package org.sosy_lab.cpachecker.cpa.invariants.variableselection;

import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public interface VariableSelection<ConstantType> {

  boolean contains(MemoryLocation pMemoryLocation);

  VariableSelection<ConstantType> acceptAssumption(BooleanFormula<ConstantType> pAssumption);

  VariableSelection<ConstantType> acceptAssignment(
      MemoryLocation pMemoryLocation,
      NumeralFormula<ConstantType> pAssumption);

  VariableSelection<ConstantType> join(VariableSelection<ConstantType> pOther);

  <T> T acceptVisitor(VariableSelectionVisitor<ConstantType, T> pVisitor);

}
