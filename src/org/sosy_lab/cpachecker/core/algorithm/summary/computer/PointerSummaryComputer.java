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
package org.sosy_lab.cpachecker.core.algorithm.summary.computer;

import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.summary.CPABasedSummaryComputer;
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
import org.sosy_lab.cpachecker.core.summary.instance.pointer.PointerFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.pointer.PointerLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.pointer.PointerSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryStore;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.MergeCalledFunctions;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.PointerFunctionSummary;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.PointerLoopSummary;
import org.sosy_lab.cpachecker.cpa.pointer2.summary.Summary;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class PointerSummaryComputer extends CPABasedSummaryComputer {

  private Set<SummarySubject> calculatedSubjects;
  private Integer functionNum;

  public PointerSummaryComputer(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier) throws InvalidConfigurationException {
    super(config, logger, shutdownNotifier);
    initSummary();
    calculatedSubjects = new HashSet<>();
    functionNum = summary.size();
  }

  @Override
  public void initSummary() {
    CFA cfa = cfaInfo.getCFA();
    for (FunctionEntryNode entry : cfa.getAllFunctionHeads()) {
      FunctionSubject subject = FunctionSubject.of(entry);
      update(subject, new PointerFunctionInstance(subject.getFunctionEntry()));
    }
  }

  @Override
  public List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> build() {
    PointerSummaryStore store = new PointerSummaryStore();
    for (SummarySubject sub : summary.keySet()) {
      SummaryInstance instance = summary.get(sub);
      if (sub instanceof FunctionSubject) {
        store.addFunctionInstance(((FunctionSubject) sub).getFunctionEntry().getFunctionName(),
            (PointerFunctionInstance) instance);
      } else if (sub instanceof LoopSubject) {
        store.addLoopInstance(((LoopSubject) sub).getLoop(), (PointerLoopInstance) instance);
      }
    }
    Triple<SummaryType, SummaryName, PointerSummaryStore> triple = Triple.of(SummaryType
        .FUNCTION_SUMMARY, SummaryName.POINTER_SUMMARY, store);
    List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> list = new ArrayList<>();
    list.add(triple);
    return list;
  }

  @Override
  protected ConfigurableProgramAnalysis createCPA(
      Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    ConfigurableProgramAnalysis cpa = null;
    try {
      CoreComponentsFactory factory =
          new CoreComponentsFactory(pConfig, pLogger, pShutdownNotifier);
      CFA cfa = cfaInfo.getCFA();
      cpa = factory.createCPA(cfa, null, SpecAutomatonCompositionType.NONE);

    } catch (InvalidConfigurationException | CPAException e) {
      e.printStackTrace();
    }
    return cpa;
  }

  @Override
  protected ReachedSet initReachedSetForSubject(
      SummarySubject pSubject,
      ConfigurableProgramAnalysis pCpa) {
    if (pSubject instanceof LoopSubject || calculatedSubjects.contains(pSubject)) {
      return null;
    }
    ReachedSet reached = null;
    ReachedSetFactory reachedSetFactory;
    try {
      reachedSetFactory = new ReachedSetFactory(config);
      reached = reachedSetFactory.create();
    } catch (InvalidConfigurationException pE) {
      pE.printStackTrace();
    }
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    CFANode loc = ((FunctionSubject) pSubject).getFunctionEntry();
    AbstractState initialState = pCpa.getInitialState(loc, partition);
    Precision initialPrecision = pCpa.getInitialPrecision(loc, partition);
    reached.add(initialState, initialPrecision);
    return reached;
  }

  @Override
  public Set<SummarySubject> computeFor(SummarySubject subject) throws Exception {
    SummaryInstance oldInstance = summary.get(subject);

    // Pick next state using strategy
    // BFS, DFS or top sort according to the configuration

    ReachedSet reached = initReachedSetForSubject(subject, cpa);
    //Preconditions.checkNotNull(reached);

    Set<SummarySubject> influenced = Sets.newHashSet();

    if (calculatedSubjects.size() == functionNum) {
      functionNum--;
    }

    Map<? extends SummarySubject, ? extends SummaryInstance> partialSummary = new HashMap<>();
    if (reached != null && !calculatedSubjects.contains(subject)) {
      partialSummary = computeFor0(reached, subject, oldInstance);
      calculatedSubjects.add(subject);
      influenced.add(subject);
    }
    if (calculatedSubjects.size() > functionNum) {
      partialSummary = handleCalledFunction(subject);
    }

    // update the set of summary entries that should be modified
    // collect their dependers (which will trigger re-computation for them)

    for (Map.Entry<? extends SummarySubject, ? extends SummaryInstance> entry : partialSummary
        .entrySet()) {
      SummarySubject s = entry.getKey();
      SummaryInstance inst = entry.getValue();
      if (update(s, inst) && calculatedSubjects.size() > functionNum) {
        influenced.addAll(getDepender(s));
      }
    }
    return influenced;
  }

  private SummaryInstance getNewInstance(SummarySubject pSubject, Summary<?> pSummary) {
    SummaryInstance currentInstance = summary.get(pSubject);
    if (pSubject instanceof FunctionSubject) {
      if (((PointerFunctionInstance) currentInstance).getFunctionSummary().equals(pSummary)) {
        return currentInstance;
      } else {
        return new PointerFunctionInstance((PointerFunctionSummary) pSummary);
      }
    } else if (pSubject instanceof LoopSubject) {
      if (((PointerLoopInstance) currentInstance).getLoopSummary().equals(pSummary)) {
        return currentInstance;
      } else {
        return new PointerLoopInstance((PointerLoopSummary) pSummary);
      }
    }
    return null;
  }

  private Map<? extends SummarySubject, ? extends SummaryInstance> handleCalledFunction(
      SummarySubject pSummarySubject) {
    Map<SummarySubject, SummaryInstance> updatedInstances = new HashMap<>();
    Set<SummarySubject> dependers = getDepender(pSummarySubject);
    PointerFunctionInstance currentInstance = (PointerFunctionInstance) summary.get
        (pSummarySubject);
    for (SummarySubject depender : dependers) {
      SummaryInstance dependInstance = summary.get(depender);
      Summary<?> newSummary;
      if (depender instanceof FunctionSubject) {
        newSummary = MergeCalledFunctions.Instance.MergeSummary(null, (
                (PointerFunctionInstance) dependInstance).getFunctionSummary(),
            currentInstance.getFunctionSummary
                ());
        updatedInstances.put(depender, getNewInstance(depender, newSummary));
      } else if (depender instanceof LoopSubject) {
        newSummary = MergeCalledFunctions.Instance.MergeSummary(null, (
                (PointerLoopInstance) dependInstance).getLoopSummary(),
            currentInstance.getFunctionSummary());
        updatedInstances.put(depender, getNewInstance(depender, newSummary));
      }
    }
    return updatedInstances;
  }

  private void handleLoopsInFunction(PointerState pState) {
    for (Loop loop : pState.getLoopSummaries().keySet()) {
      LoopSubject loopSubject = LoopSubject.of(loop);
      // update with the new loop summary
      update(loopSubject, new PointerLoopInstance(new Stack<Loop>(), loopSubject.getLoop()));
      PointerLoopSummary loopSummary = pState.getLoopSummaries().get(loop);
      for (CFAEdge edge : loopSummary.getCalledFunctions()) {
        CFANode preNode = edge.getPredecessor();
        int outEdgeNum = preNode.getNumLeavingEdges();
        for (int i = 0; i < outEdgeNum; i++) {
          CFAEdge outEdge = preNode.getLeavingEdge(i);
          if (outEdge instanceof FunctionCallEdge) {
            FunctionSubject functionSubject = FunctionSubject.of(((FunctionCallEdge) outEdge)
                .getSuccessor());
            setDependency(loopSubject, functionSubject);
            break;
          }
        }
      }
    }
  }

  @Override
  protected Map<? extends SummarySubject, ? extends SummaryInstance> summarize(
      SummarySubject pSubject, ReachedSet pReachedSet,
      SummaryInstance pOld) {
    if (pSubject instanceof FunctionSubject) {
      Map<SummarySubject, SummaryInstance> map = new HashMap<>();
      AbstractState state = pReachedSet.getLastState();
      if (state instanceof ARGState) {
        PointerState pointerState = AbstractStates.extractStateByType(state, PointerState.class);
        if (pointerState != null) {
          handleLoopsInFunction(pointerState);
          map.put(pSubject, new PointerFunctionInstance(pointerState.getCurFunctionSummary()));
          for (Loop loop : pointerState.getLoopSummaries().keySet()) {
            map.put(LoopSubject.of(loop),
                new PointerLoopInstance(pointerState.getLoopSummaries().get
                    (loop)));
          }
          return map;
        }
      }
    }
    return null;
  }

}
