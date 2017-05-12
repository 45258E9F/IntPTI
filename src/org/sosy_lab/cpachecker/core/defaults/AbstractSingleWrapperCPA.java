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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

import java.util.Collection;

/**
 * Base class for CPAs which wrap exactly one other CPA.
 */
public abstract class AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysis, WrapperCPA, StatisticsProvider {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public AbstractSingleWrapperCPA(ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkNotNull(pCpa);

    wrappedCpa = pCpa;
  }

  protected ConfigurableProgramAnalysis getWrappedCpa() {
    return wrappedCpa;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return wrappedCpa.getInitialPrecision(pNode, pPartition);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (wrappedCpa instanceof StatisticsProvider) {
      ((StatisticsProvider) wrappedCpa).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedCpa.getClass())) {
      return pType.cast(wrappedCpa);
    } else if (wrappedCpa instanceof WrapperCPA) {
      return ((WrapperCPA) wrappedCpa).retrieveWrappedCpa(pType);
    } else {
      return null;
    }
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return ImmutableList.of(wrappedCpa);
  }
}