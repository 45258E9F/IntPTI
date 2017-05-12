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
package org.sosy_lab.cpachecker.util.refinement;

import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Map;

/**
 * Classes implementing this interface derive interpolants of a whole path.
 *
 * @param <I> the type of interpolant created by the implementation
 */
public interface PathInterpolator<I extends Interpolant<?>> extends Statistics {

  Map<ARGState, I> performInterpolation(
      ARGPath errorPath,
      I interpolant
  ) throws CPAException, InterruptedException;
}
