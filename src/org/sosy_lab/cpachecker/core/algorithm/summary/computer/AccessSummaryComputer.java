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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessExternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessInternalLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessLoopInstance;
import org.sosy_lab.cpachecker.core.summary.instance.access.AccessSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryStore;
import org.sosy_lab.cpachecker.cpa.access.AccessAnalysisState;
import org.sosy_lab.cpachecker.cpa.access.summary.AccessSummaryAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by tomgu on 2/7/17.
 * Access summary computer
 * Notes:
 * 1. In each function computation phase, we compute the loops in that function,
 * all the loop summaries will be added into result in summarize();
 * 2. For recursive function call we can use the depth to control analysis process.
 * Currently, we do not use it.
 */
public class AccessSummaryComputer extends CPABasedSummaryComputer {

  private final String DEBUG_SWITCHER_PREFIX = "summary.access.debug";

  private final String DEPTH_PREFIX = "summary.access.depth";

  // for depth
  private int depth = 10000;
  // for recording subject depth
  private Map<FunctionSubject, Integer> depthMap;

  // for debug
  private boolean debug = false;
  private long count = 0;

  public AccessSummaryComputer(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier) throws Exception {
    super(pConfig, pLogger, shutdownNotifier);

    initSummary();

    depthMap = Maps.newHashMap();

    // build debug info
    if (config.getProperty(DEBUG_SWITCHER_PREFIX) != null) {
      String str = config.getProperty(DEBUG_SWITCHER_PREFIX);
      debug = Boolean.valueOf(str);
    }

    if (config.getProperty(DEPTH_PREFIX) != null) {
      String str = config.getProperty(DEPTH_PREFIX);
      assert str != null;
      depth = Integer.valueOf(str);
    }
  }

  @Override
  public void initSummary() {
    CFA cfa = cfaInfo.getCFA();
    for (FunctionEntryNode entry : cfa.getAllFunctionHeads()) {
      FunctionSubject subject = FunctionSubject.of(entry);
      update(subject, new AccessFunctionInstance(subject.getFunctionName()));
    }
  }

  /**
   * build result which will be add into provider
   */
  @Override
  public List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> build() {
    AccessSummaryStore store = new AccessSummaryStore(); // external loop and function
    AccessSummaryStore internalLoopStore = new AccessSummaryStore(); // for internal loop

    for (SummarySubject sub : summary.keySet()) {
      SummaryInstance instance = summary.get(sub);
      if (sub instanceof FunctionSubject) {
        store.updateFunctionSummary(((FunctionSubject) sub).getFunctionEntry(),
            (AccessFunctionInstance) instance);
        if (debug) {
          System.out.println(((AccessFunctionInstance) instance).apply());
        }

      } else if (sub instanceof LoopSubject) {
        // it is internal
        if (instance instanceof AccessInternalLoopInstance) {
          internalLoopStore
              .updateLoopSummary(((LoopSubject) sub).getLoop(), (AccessLoopInstance) instance);
        } else if (instance instanceof AccessExternalLoopInstance) {
          store.updateLoopSummary(((LoopSubject) sub).getLoop(), (AccessLoopInstance) instance);
        } else {
          // ignore
        }
        if (debug) {
          System.out.println(((AccessLoopInstance) instance).apply(null));
        }
      }

    }
    if (debug) {
      System.out.println("Access result: " + store);
    }

    // The access summary store instance supports two types of summary store interfaces
    List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> summarized = new ArrayList<>();
    SummaryType[] supportedTypes =
        new SummaryType[]{SummaryType.FUNCTION_SUMMARY, SummaryType.LOOP_SUMMARY};
    for (SummaryType type : supportedTypes) {
      // for loop we have two type
      if (type.equals(SummaryType.LOOP_SUMMARY)) {
        Triple<SummaryType, SummaryName, AccessSummaryStore> triple =
            Triple.of(type, SummaryName.ACCESS_LOOP_INTERNAL, internalLoopStore);
        summarized.add(triple);
      }
      Triple<SummaryType, SummaryName, AccessSummaryStore> triple =
          Triple.of(type, SummaryName.ACCESS_SUMMARY, store);
      summarized.add(triple);
    }

    return summarized;
  }

  @Override
  protected Map<? extends SummarySubject, ? extends SummaryInstance> summarize(
      SummarySubject subject, ReachedSet reachedSet, SummaryInstance old) {

    // not function subject return null
    if (!(subject instanceof FunctionSubject)) {
      return null;
    }
    // depth
    FunctionSubject fSub = (FunctionSubject) subject;
    if (!depthMap.containsKey(subject)) {
      depthMap.put(fSub, 0);
    }
    // we have enough depth, return null;
    if (depthMap.get(fSub) > depth) {
      return null;
    }
    depthMap.put(fSub, depthMap.get(fSub) + 1);
    count++;

    // we have to get the accessAnalysisState
    // we should get the loop summary from state
    // Note: the last state may has child, which may cover by previous state, so we need check
    // whether there is a child
    AbstractState state = reachedSet.getLastState();
    if (state instanceof ARGState) {
      ARGState argState = (ARGState) state;
      if (argState.getChildren().size() > 0) {
        argState = getLastState(argState);
      }

      AccessAnalysisState
          accessState = AbstractStates.extractStateByType(argState, AccessAnalysisState.class);
      if (accessState != null) {
        AccessSummaryAnalysisCPA asCPA =
            ((ARGCPA) cpa).retrieveWrappedCpa(AccessSummaryAnalysisCPA.class);

        // clear parameters: for only pointer and array type will change value, where C is call by value
        accessState = asCPA.clearParameters(accessState, fSub.getFunctionName());
        AccessFunctionInstance newInstance = new AccessFunctionInstance(fSub.getFunctionName(),
            accessState.readTree, accessState.writeTree);

        // save loop
        // we get the cpa
        // keep index declaration : keep i
        Map<Loop, AccessAnalysisState> internalMaps = Maps.newHashMap();
        // remove index : compute int i;
        Map<Loop, AccessAnalysisState> externalMaps = Maps.newHashMap();
        // get internal and external loop summary
        asCPA.getLoopSummaryMap(internalMaps, externalMaps);
        // save loop : key always the same
        for (Loop loop : internalMaps.keySet()) {
          // for internal
          LoopSubject inSub = LoopSubject.ofInternal(loop);
          AccessLoopInstance internalLoopInstance = new AccessInternalLoopInstance(loop,
              internalMaps.get(loop).readTree, internalMaps.get(loop).writeTree);
          update(inSub, internalLoopInstance);

          // for external
          LoopSubject exSub = LoopSubject.of(loop);
          AccessLoopInstance externalLoopInstance = new AccessExternalLoopInstance(loop,
              externalMaps.get(loop).readTree, externalMaps.get(loop).writeTree);
          update(exSub, externalLoopInstance);
        }

        if (!newInstance.isEqualTo(old)) {
          if (debug) {
            System.out.println(newInstance);
          }
          return createSingleEntryMap(subject, newInstance);
        }
      }
    }
    return null;
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
      // it should always be this cpa
      if (!(cpa instanceof ARGCPA)) {
        return null;
      }
      AccessSummaryAnalysisCPA asCPA =
          ((ARGCPA) cpa).retrieveWrappedCpa(AccessSummaryAnalysisCPA.class);
      // we set computer and cpa dependency here, for cpa need summary which is private variable
      if (asCPA == null) {
        return null;
      }
      asCPA.setComputer(this);
    } catch (CPAException pE) {
      pE.printStackTrace();
    }

    return cpa;
  }

  /**
   * Here, we have to update summary stored in the cpa
   */
  @Override
  protected ReachedSet initReachedSetForSubject(
      SummarySubject subject, ConfigurableProgramAnalysis cpa) {

    Preconditions.checkArgument(subject instanceof FunctionSubject);
    FunctionSubject fSubject = (FunctionSubject) subject;
    if (debug) {
      System.out.println("\n\ncount = " + count + "\n");
      System.out.println("we are in function: " + subject.toString() + "\n" +
          fSubject.getFunctionEntry().describeFileLocation());
      if (fSubject.getFunctionName().startsWith("")) {
        System.out.println("bug ---------------------------------------------------------");
      }
    }
    ReachedSet reached = null;
    ReachedSetFactory reachedSetFactory;
    try {
      reachedSetFactory = new ReachedSetFactory(
          config);// for bfs WaitlistFactory waitlistFactory = traversalMethod;
      reached = reachedSetFactory.create();

      // function without definition, it should never happen
      if (fSubject.getFunctionEntry() == null) {
        return reached;
      }
      CFANode loc = fSubject.getFunctionEntry().getExitNode();
      StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
      //
      AbstractState initialState = cpa.getInitialState(loc, partition);
      Precision initialPrecision = cpa.getInitialPrecision(loc, partition);
      reached.add(initialState, initialPrecision);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
    return reached;
  }

  public void initializeSummaryForCPA(String fName, Map<String, AccessAnalysisState> pSummary) {
    // for this function, it must have definition
    Preconditions.checkArgument(GlobalInfo.getInstance().getCFAInfo().isPresent());
    FunctionEntryNode node = cfaInfo.getCFA().getFunctionHead(fName);
    FunctionSubject pSub = FunctionSubject.of(node);
    Set<SummarySubject> dependee = getDependee(pSub);
    if (debug) {
      System.out.println("dependee size = " + dependee.size());
      System.out.println("depender size = " + getDepender(pSub).size());
    }
    for (SummarySubject sub : dependee) {
      if (!(sub instanceof FunctionSubject)) {
        continue;
      }
      FunctionSubject currentSubject = (FunctionSubject) sub;
      String name = currentSubject.getFunctionName();
      AccessFunctionInstance instance = (AccessFunctionInstance) summary.get(currentSubject);
      if (instance == null) {
        pSummary.put(name, new AccessAnalysisState());
      } else {
        pSummary
            .put(name, new AccessAnalysisState(instance.getReadTree(), instance.getWriteTree()));
      }
    }
  }


  // find the last abstract state
  private ARGState getLastState(ARGState pArgState) {
    if (pArgState.getChildren().size() > 0) {
      return getLastState(pArgState.getChildren().iterator().next());
    }
    return pArgState;
  }
}
