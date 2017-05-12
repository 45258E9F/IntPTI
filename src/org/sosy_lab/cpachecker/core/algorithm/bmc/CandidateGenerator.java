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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


public interface CandidateGenerator extends Iterable<CandidateInvariant> {

  /**
   * Tries to produce more candidates.
   *
   * @return {@code true} if more candidates were produced, {@code false} otherwise.
   */
  boolean produceMoreCandidates();

  /**
   * Checks if there are candidates currently available.
   *
   * If no candidates are available, more can be requested to be produced by
   * calling {@link #produceMoreCandidates}.
   *
   * @return {@code true} if there are any candidates, {@code false} if more need to be produced
   * first.
   */
  boolean hasCandidatesAvailable();

  /**
   * Confirms the given candidates, so that they are no longer provided as
   * candidates.
   */
  void confirmCandidates(Iterable<CandidateInvariant> pCandidates);

  /**
   * Returns the confirmed candidate invariants.
   */
  Set<? extends CandidateInvariant> getConfirmedCandidates();

  @Override
  Iterator<CandidateInvariant> iterator();

  public static CandidateGenerator EMPTY_GENERATOR =
      new CandidateGenerator() {

        @Override
        public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
          // Do nothing
        }

        @Override
        public boolean produceMoreCandidates() {
          return false;
        }

        @Override
        public boolean hasCandidatesAvailable() {
          return false;
        }

        @Override
        public Iterator<CandidateInvariant> iterator() {
          return Collections.emptyIterator();
        }

        @Override
        public Set<CandidateInvariant> getConfirmedCandidates() {
          return Collections.emptySet();
        }
      };

}
