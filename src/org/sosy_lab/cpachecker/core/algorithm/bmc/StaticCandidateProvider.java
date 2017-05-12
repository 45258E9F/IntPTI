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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


public class StaticCandidateProvider implements CandidateGenerator {

  private final ImmutableSet<CandidateInvariant> allCandidates;

  private final Set<CandidateInvariant> confirmedInvariants = Sets.newLinkedHashSet();

  private final Set<CandidateInvariant> candidates = Sets.newLinkedHashSet();

  private boolean produced = false;

  public StaticCandidateProvider(Iterable<? extends CandidateInvariant> pCandidates) {
    Iterables.addAll(candidates, pCandidates);
    allCandidates = ImmutableSet.copyOf(pCandidates);
  }

  @Override
  public boolean produceMoreCandidates() {
    if (produced) {
      return false;
    }
    produced = true;
    return !candidates.isEmpty();
  }

  @Override
  public boolean hasCandidatesAvailable() {
    return produced && !candidates.isEmpty();
  }

  @Override
  public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
    for (CandidateInvariant candidate : pCandidates) {
      candidates.remove(candidate);
    }
    Iterables.addAll(confirmedInvariants, pCandidates);
  }

  @Override
  public Iterator<CandidateInvariant> iterator() {
    if (!produced) {
      return Collections.<CandidateInvariant>emptyIterator();
    }
    return candidates.iterator();
  }

  @Override
  public Set<CandidateInvariant> getConfirmedCandidates() {
    return Collections.unmodifiableSet(confirmedInvariants);
  }

  public Set<CandidateInvariant> getAllCandidates() {
    return allCandidates;
  }

}
