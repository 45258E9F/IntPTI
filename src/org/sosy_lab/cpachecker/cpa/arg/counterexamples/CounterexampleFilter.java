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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

/**
 * This interface defines an abstraction for counterexample filter.
 * When ARGCPA handles multiple counterexamples,
 * there might be many similar counterexamples in the reached set,
 * but the user would probably like to see not all of them,
 * only those that differ significantly.
 * A counterexample filter is used to filter all those unwanted counterexamples.
 *
 * It is expected that a counterexample filter is stateful.
 * It usually keeps track of all previously seen counterexamples
 * (at least of the relevant ones), and compares a new counterexample
 * against this set.
 *
 * IMPORTANT: A counterexample filter should try hard to not have a reference
 * on ARGStates!
 * Doing so would retain a lot of memory, because every ARGState has (transitive)
 * references to the full ARG.
 * Also ARGStates may be deleted later on, which changes their state
 * and thus makes them useless.
 *
 * Instead, prefer keeping references to objects like CFAEdges,
 * or representations of program state in a different form (variable assignments,
 * formulas, etc.).
 *
 * Counterexample filters do not need to be thread-safe.
 *
 * Implementations should define a public constructor with exactly the following
 * three arguments (in this order):
 * - {@link org.sosy_lab.common.configuration.Configuration}
 * - {@link org.sosy_lab.common.log.LogManager}
 * - {@link org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis}
 */
public interface CounterexampleFilter {

  boolean isRelevant(CounterexampleInfo counterexample) throws InterruptedException;
}
