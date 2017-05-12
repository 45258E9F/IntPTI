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
package org.sosy_lab.cpachecker.cpa.invariants.variableselection;

import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class AcceptAllVariableSelection<ConstantType> implements VariableSelection<ConstantType> {

  @Override
  public boolean contains(MemoryLocation pMemoryLocation) {
    return true;
  }

  @Override
  public VariableSelection<ConstantType> acceptAssumption(BooleanFormula<ConstantType> pAssumption) {
    return this;
  }

  @Override
  public VariableSelection<ConstantType> acceptAssignment(
      MemoryLocation pMemoryLocation,
      NumeralFormula<ConstantType> pAssumption) {
    return this;
  }

  @Override
  public VariableSelection<ConstantType> join(VariableSelection<ConstantType> pOther) {
    return this;
  }

  @Override
  public <T> T acceptVisitor(VariableSelectionVisitor<ConstantType, T> pVisitor) {
    return pVisitor.visit(this);
  }

}
