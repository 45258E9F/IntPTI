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
package org.sosy_lab.cpachecker.core.summary.instance.arith.convert;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearVariable;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.communicator.CoreCommunicator;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;

/**
 * Resolve linear variable to symbolic value
 */
public class SymbolicValueResolver extends ShapeBasedResolver<ShapeSymbolicValue> {

  public SymbolicValueResolver(ShapeState pShape) {
    super(pShape);
  }

  @Override
  public ShapeSymbolicValue apply(LinearVariable lv) {
    CType type = lv.getType();
    return CoreCommunicator.getInstance().getValueFor(shape, lv.getMemoryLocation(), type);
  }

}
