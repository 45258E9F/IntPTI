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
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Set;

/**
 * Interface for components that can verify the feasibility of a counterexample.
 *
 * A counterexample is a finite set of loop-free paths in the ARG that form a
 * DAG with a single source (the root state of the ARG) and a single sink
 * (the target state).
 */
public interface CounterexampleChecker {

  /**
   * Check feasibility of counterexample.
   *
   * @param rootState       The source of the counterexample paths.
   * @param errorState      The sink of the counterexample paths.
   * @param errorPathStates All state that belong to the counterexample paths.
   * @return True if the counterexample is feasible.
   * @throws CPAException         If something goes wrong.
   * @throws InterruptedException If the thread was interrupted.
   */
  boolean checkCounterexample(
      ARGState rootState, ARGState errorState,
      Set<ARGState> errorPathStates)
      throws CPAException, InterruptedException;

}
