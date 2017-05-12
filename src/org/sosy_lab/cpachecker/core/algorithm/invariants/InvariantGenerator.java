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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


/**
 * Interface for methods to generate invariants about the program.
 *
 * First {@link #start(CFANode)} needs to be called with the entry point
 * of the CFA, and then {@link #get()} can be called to retrieve the reached
 * set with the invariants.
 *
 * It is a good idea to call {@link #start(CFANode)} as soon as possible
 * and {@link #get()} as late as possible to minimize waiting times
 * if the generator is configured for asynchronous execution.
 *
 * It is also a good idea to call {@link #get()} only if really necessary
 * (in synchronous case, it is expensive).
 */
public interface InvariantGenerator {

  /**
   * Prepare invariant generation, and optionally start the algorithm.
   * May be called only once.
   */
  void start(CFANode initialLocation);

  /**
   * Cancel the invariant generation algorithm, if running.
   * Can be called only after {@link #start(CFANode)} was called.
   */
  void cancel();

  /**
   * Retrieve the generated invariant.
   * Can be called only after {@link #start(CFANode)} was called.
   *
   * Depending on the invariant generator, this method may either block
   * for some time during the invariant generation runs,
   * or return a current snapshot of the invariants quickly.
   *
   * @throws CPAException         If the invariant generation failed.
   * @throws InterruptedException If the invariant generation was interrupted.
   */
  InvariantSupplier get() throws CPAException, InterruptedException;

  /**
   * Retrieve the generated invariant as an expression tree.
   * Can be called only after {@link #start(CFANode)} was called.
   *
   * Depending on the invariant generator, this method may either block
   * for some time during the invariant generation runs,
   * or return a current snapshot of the invariants quickly.
   *
   * @throws CPAException         If the invariant generation failed.
   * @throws InterruptedException If the invariant generation was interrupted.
   */
  ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException;

  /**
   * Return whether the invariant generation has already proved
   * that the specification holds, and no further checks are necessary.
   * If possible, this method should be cheap.
   */
  boolean isProgramSafe();

  /**
   * Add a specific invariant that is guaranteed to hold to the set of facts
   * this invariant generator may return.
   * Note that it is not guaranteed that the invariant returned
   * by a call to {@link #get()} includes or implies the injected invariant.
   *
   * @param pLocation   The location where the invariant holds.
   * @param pAssumption A guard that is guaranteed to hold at the given location.
   * @throws UnrecognizedCodeException if a problem occurred during the injection.
   */
  void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) throws UnrecognizedCodeException;
}