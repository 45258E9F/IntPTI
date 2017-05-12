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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


public abstract class AbstractInvariantGenerator implements InvariantGenerator {

  @Override
  public abstract void start(CFANode pInitialLocation);

  @Override
  public abstract void cancel();

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    return ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  @Override
  public abstract boolean isProgramSafe();

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption)
      throws UnrecognizedCodeException {
    // Do nothing
  }

}
