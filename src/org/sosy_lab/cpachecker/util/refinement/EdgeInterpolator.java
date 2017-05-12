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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Deque;

/**
 * Classes implementing this interface are able to derive interpolants from edges.
 */
public interface EdgeInterpolator<S extends ForgetfulState<?>, I extends Interpolant<S>> {

  I deriveInterpolant(
      ARGPath errorPath,
      CFAEdge currentEdge,
      Deque<S> callstack,
      PathPosition offset,
      I inputInterpolant
  ) throws CPAException, InterruptedException;

  int getNumberOfInterpolationQueries();
}
