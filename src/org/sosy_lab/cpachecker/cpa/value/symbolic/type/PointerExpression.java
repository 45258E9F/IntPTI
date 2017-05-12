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

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link SymbolicExpression} that represents a pointer expression.
 *
 * <p>This can be a classic pointer expression like <code>*p</code> in C or a reference as in
 * Java.</p>
 */
public class PointerExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = -7348176261979912313L;

  protected PointerExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  protected PointerExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public PointerExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new PointerExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "*";
  }
}
