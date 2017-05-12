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

/**
 * Different {@link org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision
 * ConstraintsPrecisions}. {@link org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.FullConstraintsPrecision
 * FullConstraintsPrecision} tracks all constraints for all locations, {@link
 * org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision
 * RefinableConstraintsPrecision} is a refinable precision that's initially empty.
 */
package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;