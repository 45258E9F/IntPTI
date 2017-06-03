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
package org.sosy_lab.cpachecker.core.algorithm.summary.computer;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.summary.NarrowingSupportedSummaryComputer;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummarySubject;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummaryType;
import org.sosy_lab.cpachecker.core.algorithm.summary.subjects.FunctionSubject;
import org.sosy_lab.cpachecker.core.algorithm.summary.subjects.LoopSubject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeExternalLoopStore;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionPrecondition;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionStore;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeInternalLoopStore;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryStore;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.summary.RangeSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class RangeSummaryComputer extends NarrowingSupportedSummaryComputer {

  private final Configuration config;

  public RangeSummaryComputer(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier
      pShutdownNotifier) throws InvalidConfigurationException {

    super(pConfig, pLogger, pShutdownNotifier);
    config = pConfig;
    initSummary();
  }

  @Override
  public void initSummary() {
    CFA cfa = cfaInfo.getCFA();
    for (FunctionEntryNode entry : cfa.getAllFunctionHeads()) {
      FunctionSubject subject = FunctionSubject.of(entry);
      update(subject, new RangeFunctionInstance(subject.getFunctionName()));
    }
  }

  @Override
  public List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> build() {
    RangeFunctionStore functionStore = new RangeFunctionStore();
    RangeInternalLoopStore internalStore = new RangeInternalLoopStore();
    RangeExternalLoopStore externalStore = new RangeExternalLoopStore();
    for (SummarySubject subject : summary.keySet()) {
      SummaryInstance instance = summary.get(subject);

      if (subject instanceof FunctionSubject) {
        functionStore.updateSummary(((FunctionSubject) subject).getFunctionEntry(),
            (RangeFunctionInstance) instance);
      } else if (subject instanceof LoopSubject) {
        if (instance instanceof RangeInternalLoopInstance) {
          internalStore.updateSummary(((LoopSubject) subject).getLoop(),
              (RangeInternalLoopInstance) instance);
        } else {
          assert (instance instanceof RangeExternalLoopInstance);
          externalStore.updateSummary(((LoopSubject) subject).getLoop(),
              (RangeExternalLoopInstance) instance);
        }
      }
    }
    List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> totalStore = new ArrayList<>();
    totalStore.add(Triple.of(SummaryType.FUNCTION_SUMMARY, SummaryName.RANGE_SUMMARY,
        functionStore));
    totalStore.add(Triple.of(SummaryType.LOOP_SUMMARY, SummaryName.RANGE_SUMMARY, externalStore));
    totalStore.add(Triple.of(SummaryType.LOOP_SUMMARY, SummaryName.RANGE_LOOP_INTERNAL,
        internalStore));
    // update function precondition --- the internal function summary
    // TODO: in the future we re-design the summary computation in the batch-style algorithm
    RangeFunctionPrecondition.updateBatch();
    return totalStore;
  }

  @Override
  protected ConfigurableProgramAnalysis createCPA(
      Configuration config, LogManager logger, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    ConfigurableProgramAnalysis cpa = null;
    CoreComponentsFactory factory = new CoreComponentsFactory(config, logger, shutdownNotifier);
    CFA cfa = cfaInfo.getCFA();
    try {
      cpa = factory.createCPA(cfa, null, SpecAutomatonCompositionType.NONE);
      RangeSummaryCPA rsCPA = CPAs.retrieveCPA(cpa, RangeSummaryCPA.class);
      if (rsCPA == null) {
        throw new InvalidConfigurationException("Range summary CPA required but missing");
      }
      rsCPA.setSummaryComputer(this);
    } catch (CPAException e) {
      // failed to create the CPA
      e.printStackTrace();
    }
    return cpa;
  }

  @Override
  protected ReachedSet initReachedSetForSubject(
      SummarySubject subject, ConfigurableProgramAnalysis cpa) {
    Preconditions.checkArgument(subject instanceof FunctionSubject);
    FunctionSubject fSubject = (FunctionSubject) subject;

    ReachedSet reachedSet;
    ReachedSetFactory reachedFactory;
    try {
      reachedFactory = new ReachedSetFactory(config);
      reachedSet = reachedFactory.create();
      CFANode entryLoc = fSubject.getFunctionEntry();
      StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
      AbstractState initialState = cpa.getInitialState(entryLoc, partition);
      Precision initialPrecision = cpa.getInitialPrecision(entryLoc, partition);
      reachedSet.add(initialState, initialPrecision);
      // load existing loop information for accelerating the analysis
      RangeSummaryCPA rsCPA = CPAs.retrieveCPA(cpa, RangeSummaryCPA.class);
      assert (rsCPA != null);
      rsCPA.loadLoopSummary(cfaInfo, fSubject.getFunctionName());
    } catch (InvalidConfigurationException e) {
      throw new IllegalArgumentException("Failed to initialize reached set");
    }
    return reachedSet;
  }

  @Override
  protected Map<? extends SummarySubject, ? extends SummaryInstance> summarize(
      SummarySubject subject, ReachedSet reachedSet, SummaryInstance old) {
    if (!(subject instanceof FunctionSubject)) {
      return null;
    }
    RangeSummaryCPA rsCPA = CPAs.retrieveCPA(cpa, RangeSummaryCPA.class);
    assert (rsCPA != null);
    RangeFunctionInstance newInstance = rsCPA.getFunctionSummary();
    Multimap<Loop, Pair<CFAEdge, RangeState>> internalMap = HashMultimap.create();
    Multimap<Loop, Pair<CFAEdge, RangeState>> externalMap = HashMultimap.create();
    rsCPA.getLoopSummary(internalMap, externalMap);
    for (Loop loop : internalMap.keySet()) {
      LoopSubject loopSubject = LoopSubject.ofInternal(loop);
      Map<CFAEdge, RangeState> stateMap = new HashMap<>();
      for (Pair<CFAEdge, RangeState> entry : internalMap.get(loop)) {
        stateMap.put(entry.getFirstNotNull(), entry.getSecondNotNull());
      }
      RangeInternalLoopInstance loopInstance = new RangeInternalLoopInstance(loop, stateMap);
      update(loopSubject, loopInstance);
    }
    for (Loop loop : externalMap.keySet()) {
      LoopSubject loopSubject = LoopSubject.of(loop);
      Map<CFAEdge, RangeState> stateMap = new HashMap<>();
      for (Pair<CFAEdge, RangeState> entry : externalMap.get(loop)) {
        stateMap.put(entry.getFirstNotNull(), entry.getSecondNotNull());
      }
      RangeExternalLoopInstance loopInstance = new RangeExternalLoopInstance(loop, stateMap);
      update(loopSubject, loopInstance);
    }
    if (newInstance != null && !newInstance.isEqualTo(old)) {
      // return partial summary
      return createSingleEntryMap(subject, newInstance);
    }
    return null;
  }

  @Override
  protected Map<? extends SummarySubject, ? extends SummaryInstance> summarize(
      SummarySubject subject,
      Multimap<CFANode, AbstractState> location2State,
      SummaryInstance old) {
    if (!(subject instanceof FunctionSubject)) {
      return null;
    }
    // new function summary is still loaded from CPA
    RangeSummaryCPA rsCPA = CPAs.retrieveCPA(cpa, RangeSummaryCPA.class);
    assert (rsCPA != null);
    RangeFunctionInstance newInstance = rsCPA.getFunctionSummary();
    // loop summary is derived from `location2State` mapping
    Multimap<Loop, Pair<CFAEdge, RangeState>> internalMap = HashMultimap.create();
    Multimap<Loop, Pair<CFAEdge, RangeState>> externalMap = HashMultimap.create();
    rsCPA.getLoopSummary(internalMap, externalMap);
    for (Loop loop : internalMap.keySet()) {
      LoopSubject loopSubject = LoopSubject.ofInternal(loop);
      Map<CFAEdge, RangeState> stateMap = new HashMap<>();
      boolean gen = true;
      for (Pair<CFAEdge, RangeState> entry : internalMap.get(loop)) {
        CFAEdge enteringEdge = entry.getFirstNotNull();
        CFANode entryNode = enteringEdge.getSuccessor();
        Collection<AbstractState> states = location2State.get(entryNode);
        RangeState merged = extractRangeStateFrom(states);
        if (merged == null) {
          gen = false;
          break;
        }
        stateMap.put(enteringEdge, merged);
      }
      if (gen) {
        RangeInternalLoopInstance loopInstance = new RangeInternalLoopInstance(loop, stateMap);
        update(loopSubject, loopInstance);
      }
    }
    for (Loop loop : externalMap.keySet()) {
      LoopSubject loopSubject = LoopSubject.of(loop);
      Map<CFAEdge, RangeState> stateMap = new HashMap<>();
      boolean gen = true;
      for (Pair<CFAEdge, RangeState> entry : externalMap.get(loop)) {
        CFAEdge exitEdge = entry.getFirstNotNull();
        CFANode exitNode = exitEdge.getSuccessor();
        Collection<AbstractState> states = location2State.get(exitNode);
        RangeState merged = extractRangeStateFrom(states);
        if (merged == null) {
          gen = false;
          break;
        }
        stateMap.put(exitEdge, merged);
      }
      if (gen) {
        RangeExternalLoopInstance loopInstance = new RangeExternalLoopInstance(loop, stateMap);
        update(loopSubject, loopInstance);
      }
    }
    if (newInstance != null && !newInstance.isEqualTo(old)) {
      return createSingleEntryMap(subject, newInstance);
    }
    return null;
  }

  @Nullable
  private RangeState extractRangeStateFrom(Collection<AbstractState> states) {
    FluentIterable<RangeState> rangeStates = FluentIterable.from(states).transform(
        new Function<AbstractState, RangeState>() {
          @Override
          public RangeState apply(AbstractState pAbstractState) {
            return AbstractStates.extractStateByType(pAbstractState, RangeState.class);
          }
        }).filter(Predicates.notNull());
    if (rangeStates.isEmpty()) {
      // if such case occurs, we should not alter the original summary
      return null;
    } else {
      List<RangeState> stateList = rangeStates.toList();
      RangeState result = stateList.get(0);
      for (int i = 1; i < stateList.size(); i++) {
        result = result.join(stateList.get(i));
      }
      return result;
    }
  }

  /* ****************** */
  /* customized methods */
  /* ****************** */

  @Nullable
  public RangeFunctionInstance getFunctionSummary(String funcName) {
    SummaryInstance instance = summary.get(FunctionSubject.of(funcName));
    if (instance == null) {
      return null;
    }
    assert (instance instanceof RangeFunctionInstance);
    return (RangeFunctionInstance) instance;
  }

  @Nullable
  public RangeInternalLoopInstance getInternalLoopSummary(Loop loop) {
    SummaryInstance instance = summary.get(LoopSubject.ofInternal(loop));
    if (instance == null) {
      return null;
    }
    assert (instance instanceof RangeInternalLoopInstance);
    return (RangeInternalLoopInstance) instance;
  }

  @Nullable
  public RangeExternalLoopInstance getExternalLoopSummary(Loop loop) {
    SummaryInstance instance = summary.get(LoopSubject.of(loop));
    if (instance == null) {
      return null;
    }
    assert (instance instanceof RangeExternalLoopInstance);
    return (RangeExternalLoopInstance) instance;
  }

}
