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
import org.sosy_lab.cpachecker.cpa.constraints.constraint.UnaryConstraint;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link UnarySymbolicExpression} representing the 'logical not' operation.
 */
public class LogicalNotExpression extends UnarySymbolicExpression implements UnaryConstraint {

  private static final long serialVersionUID = 1538819641715577876L;

  protected LogicalNotExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  protected LogicalNotExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public LogicalNotExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new LogicalNotExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "!";
  }
}
