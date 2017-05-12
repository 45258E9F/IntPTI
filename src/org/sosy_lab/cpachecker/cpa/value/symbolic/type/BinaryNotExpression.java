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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link UnarySymbolicExpression} representing the 'binary not' operation.
 */
public class BinaryNotExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = -84948336461412258L;

  protected BinaryNotExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  protected BinaryNotExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public BinaryNotExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new BinaryNotExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return "~" + getOperand();
  }

  @Override
  public String getOperationString() {
    return "~";
  }
}
