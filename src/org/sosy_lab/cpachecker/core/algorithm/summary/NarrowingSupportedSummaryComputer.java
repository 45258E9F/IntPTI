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
package org.sosy_lab.cpachecker.core.algorithm.summary;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.summary.subjects.FunctionSubject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Map;

@Options(prefix = "summary.narrow")
public abstract class NarrowingSupportedSummaryComputer extends CPABasedSummaryComputer {

  @Option(secure = true, name = "allow", description = "whether narrowing is performed")
  private boolean allowNarrow = false;

  private final NarrowingRefinementAlgorithm narrowingAlgorithm;

  protected NarrowingSupportedSummaryComputer(
      Configuration config, LogManager logger,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    super(config, logger, shutdownNotifier);
    // Note: to inject an abstract class, one must specify the class name
    config.inject(this, NarrowingSupportedSummaryComputer.class);
    narrowingAlgorithm = new NarrowingRefinementAlgorithm(config, cpa);
  }

  @Override
  protected Map<? extends SummarySubject, ? extends SummaryInstance> computeFor0(
      ReachedSet reachedSet, SummarySubject subject, SummaryInstance old) throws Exception {
    Map<? extends SummarySubject, ? extends SummaryInstance> partialSummary;
    while (reachedSet.hasWaitingState()) {
      final AbstractState state = reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(state);
      try {
        if (handleState(state, precision, reachedSet)) {
          break;
        }
      } catch (Exception e) {
        reachedSet.reAddToWaitlist(state);
        throw e;
      }
    }
    if (allowNarrow) {
      // perform narrowing here
      Multimap<CFANode, AbstractState> location2State = narrowingAlgorithm.run0(reachedSet, (
          (FunctionSubject) subject).getFunctionEntry());
      partialSummary = summarize(subject, location2State, old);
    } else {
      partialSummary = summarize(subject, reachedSet, old);
    }
    if (partialSummary == null) {
      partialSummary = Maps.newHashMap();
    }
    return partialSummary;
  }

  protected abstract Map<? extends SummarySubject, ? extends SummaryInstance>
  summarize(
      SummarySubject subject, Multimap<CFANode, AbstractState> location2State,
      SummaryInstance old);

}
