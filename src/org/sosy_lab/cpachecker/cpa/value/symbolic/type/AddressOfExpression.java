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
 * Representation of ampersand expression in C. Example: <code>int a; int b = &a</code>
 */
public class AddressOfExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = -4583179464566332040L;

  protected AddressOfExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  protected AddressOfExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public AddressOfExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new AddressOfExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "&";
  }
}
