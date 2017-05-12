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
package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * Full precision. Tracks all constraints at every location.
 */
public class FullConstraintsPrecision implements ConstraintsPrecision {

  public static FullConstraintsPrecision getInstance() {
    return new FullConstraintsPrecision();
  }

  private FullConstraintsPrecision() {
    // DO NOTHING
  }

  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pNode) {
    return true;
  }

  @Override
  public ConstraintsPrecision join(ConstraintsPrecision pOther) {
    throw new UnsupportedOperationException(
        FullConstraintsPrecision.class.getSimpleName() + " can't be joined");
  }

  @Override
  public ConstraintsPrecision withIncrement(Increment pIncrement) {
    throw new UnsupportedOperationException(
        FullConstraintsPrecision.class.getSimpleName() + " can't be incremented"
    );
  }
}
