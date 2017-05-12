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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ReachedSet that forwards all calls to another instance.
 * The target instance is changeable. Remembers and provides all instances
 * to which calls are forwarded
 */
public class HistoryForwardingReachedSet extends ForwardingReachedSet {

  private final List<ReachedSet> usedReachedSets;
  private final List<ConfigurableProgramAnalysis> cpas;

  public HistoryForwardingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
    usedReachedSets = new ArrayList<>();
    cpas = new ArrayList<>();
  }

  @Override
  public void setDelegate(ReachedSet pDelegate) {
    super.setDelegate(pDelegate);
    usedReachedSets.add(pDelegate);
  }

  public List<ReachedSet> getAllReachedSetsUsedAsDelegates() {
    return ImmutableList.copyOf(usedReachedSets);
  }

  public void saveCPA(ConfigurableProgramAnalysis pCurrentCpa) {
    Preconditions.checkArgument(pCurrentCpa != null);
    cpas.add(pCurrentCpa);
  }

  public List<ConfigurableProgramAnalysis> getCPAs() {
    return ImmutableList.copyOf(cpas);
  }

}
