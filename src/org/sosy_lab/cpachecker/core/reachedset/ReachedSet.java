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
package org.sosy_lab.cpachecker.core.reachedset;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Set;

/**
 * Interface representing a set of reached states, including storing a
 * precision for each one.
 *
 * In all its operations it preserves the order in which the state were added.
 * All the collections returned from methods of this class ensure this ordering, too.
 *
 * Classes implementing this interface may not allow null values for states and precisions.
 * All methods do not return null except when stated explicitly.
 */
public interface ReachedSet extends UnmodifiableReachedSet {

  @Override
  Set<AbstractState> asCollection();

  /**
   * Add a state with a precision to the reached set and to the waitlist.
   * If the state is already in the reached set and the precisions are equal,
   * nothing is done.
   *
   * @param state     An AbstractState.
   * @param precision The Precision for the AbstractState
   * @throws IllegalArgumentException If the state is already in the reached set, but with a
   *                                  different precision.
   */
  void add(AbstractState state, Precision precision) throws IllegalArgumentException;


  void addAll(Iterable<Pair<AbstractState, Precision>> toAdd);

  /**
   * Re-add a state to the waitlist which is already contained in the reached set.
   */
  void reAddToWaitlist(AbstractState s);

  /**
   * Change the precision of a state that is already in the reached set.
   */
  void updatePrecision(AbstractState s, Precision newPrecision);

  void remove(AbstractState state);

  void removeAll(Iterable<? extends AbstractState> toRemove);

  void removeOnlyFromWaitlist(AbstractState state);

  void clear();

  AbstractState popFromWaitlist();

  <T extends AbstractState> boolean contains(AbstractState s, Class<T> type);
}
