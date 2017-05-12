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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Objects;

/**
 * An expression containing {@link SymbolicValue}s.
 */
public abstract class SymbolicExpression implements SymbolicValue {

  private static final long serialVersionUID = 2228733300503173691L;

  private final Optional<MemoryLocation> representedLocation;

  protected SymbolicExpression(final MemoryLocation pRepresentedLocation) {
    representedLocation = Optional.of(pRepresentedLocation);
  }

  protected SymbolicExpression() {
    representedLocation = Optional.absent();
  }

  @Override
  public abstract SymbolicExpression copyForLocation(MemoryLocation pRepresentedLocation);

  @Override
  public Optional<MemoryLocation> getRepresentedLocation() {
    return representedLocation;
  }

  /**
   * Accepts the given {@link SymbolicValueVisitor}.
   *
   * @param pVisitor         the visitor to accept
   * @param <VisitorReturnT> the return type of the visitor's specific <code>visit</code> method
   * @return the value returned by the visitor's <code>visit</code> method
   */
  @Override
  public abstract <VisitorReturnT> VisitorReturnT accept(
      SymbolicValueVisitor<VisitorReturnT> pVisitor);

  /**
   * Returns the expression type of this <code>SymbolicExpression</code>.
   *
   * @return the expression type of this <code>SymbolicExpression</code>
   */
  public abstract Type getType();

  /**
   * Returns whether this <code>SymbolicExpression</code> is always true and does only contain
   * explicit values.
   *
   * @return <code>true</code> if this <code>SymbolicExpression</code> is always true and does only
   * contain explicit values, <code>false</code> otherwise
   */
  public abstract boolean isTrivial();

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    throw new UnsupportedOperationException(
        "Symbolic expressions can't be expressed as numeric values");
  }

  @Override
  public Long asLong(CType type) {
    throw new UnsupportedOperationException(
        "Symbolic expressions can't be expressed as numeric values");
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(representedLocation);
  }

  @Override
  public boolean equals(final Object pObj) {
    return pObj != null
        && pObj.getClass().equals(getClass())
        && Objects.equals(representedLocation,
        ((SymbolicExpression) pObj).representedLocation);
  }
}
