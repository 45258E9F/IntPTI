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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Objects;


public class Variable<ConstantType> extends AbstractFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  private final MemoryLocation memoryLocation;

  private Variable(BitVectorInfo pInfo, MemoryLocation pMemoryLocation) {
    super(pInfo);
    this.memoryLocation = pMemoryLocation;
  }

  public MemoryLocation getMemoryLocation() {
    return this.memoryLocation;
  }

  @Override
  public String toString() {
    return getMemoryLocation().getAsSimpleString();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Variable) {
      Variable<?> other = (Variable<?>) pOther;
      return getBitVectorInfo().equals(other.getBitVectorInfo())
          && getMemoryLocation().equals(other.getMemoryLocation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBitVectorInfo(), getMemoryLocation());
  }

  @Override
  public <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing the variable with the given memory location.
   *
   * @param pInfo           the bit vector information.
   * @param pMemoryLocation the memory location of the variable.
   * @return an invariants formula representing the variable with the given memory location.
   */
  static <ConstantType> Variable<ConstantType> of(
      BitVectorInfo pInfo,
      MemoryLocation pMemoryLocation) {
    return new Variable<>(pInfo, pMemoryLocation);
  }
}
