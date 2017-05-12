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
package org.sosy_lab.cpachecker.util.automaton;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import java.util.logging.Level;


public class TargetLocationProviderImpl implements TargetLocationProvider {

  private final ReachedSetFactory reachedSetFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logManager;
  private final Configuration config;
  private final CFA cfa;
  private final ImmutableSet<CFANode> allNodes;

  private final static String specificationPropertyName = "specification";

  public TargetLocationProviderImpl(
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier,
      LogManager pLogManager, Configuration pConfig, CFA pCfa) {
    reachedSetFactory = pReachedSetFactory;
    shutdownNotifier = pShutdownNotifier;
    logManager = pLogManager.withComponentName("TargetLocationProvider");
    config = pConfig;
    cfa = pCfa;
    allNodes = ImmutableSet.copyOf(cfa.getAllNodes());
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      CFANode pRootNode, Optional<Automaton> pAutomaton) {
    try {
      // Create new configuration with default set of CPAs
      ConfigurationBuilder configurationBuilder = Configuration.builder();
      if (!pAutomaton.isPresent() && config.hasProperty(specificationPropertyName)) {
        configurationBuilder.copyOptionFrom(config, specificationPropertyName);
      }
      configurationBuilder.setOption("output.disable", "true");
      configurationBuilder.setOption("CompositeCPA.cpas",
          "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA");
      configurationBuilder.setOption("cpa.callstack.skipRecursion", "true");

      Configuration configuration = configurationBuilder.build();
      CPABuilder cpaBuilder =
          new CPABuilder(configuration, logManager, shutdownNotifier, reachedSetFactory);
      final ConfigurableProgramAnalysis cpa;
      if (pAutomaton.isPresent()) {
        cpa =
            cpaBuilder.buildsCPAWithWitnessAutomataAndSpecification(
                cfa, Lists.newArrayList(pAutomaton.get()));
      } else {
        cpa = cpaBuilder.buildCPAWithSpecAutomatas(cfa);
      }

      ReachedSet reached = reachedSetFactory.create();
      reached.add(
          cpa.getInitialState(pRootNode, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(pRootNode, StateSpacePartition.getDefaultPartition()));
      CPAAlgorithm targetFindingAlgorithm =
          CPAAlgorithm.create(cpa, logManager, configuration, shutdownNotifier);
      try {

        while (reached.hasWaitingState()) {
          targetFindingAlgorithm.run(reached);
        }

      } finally {
        CPAs.closeCpaIfPossible(cpa, logManager);
        CPAs.closeIfPossible(targetFindingAlgorithm, logManager);
      }

      // Order of reached is the order in which states were created,
      // toSet() keeps ordering, so the result is deterministic.
      return from(reached)
          .filter(AbstractStates.IS_TARGET_STATE)
          .transform(AbstractStates.EXTRACT_LOCATION)
          .toSet();

    } catch (InvalidConfigurationException | CPAException e) {

      if (!e.toString().toLowerCase().contains("recursion")) {
        logManager.logUserException(Level.WARNING, e,
            "Unable to find target locations. Defaulting to selecting all locations as potential target locations.");
      } else {
        logManager.log(Level.INFO,
            "Recursion detected. Defaulting to selecting all locations as potential target locations.");
        logManager.logDebugException(e);
      }

      return allNodes;

    } catch (InterruptedException e) {
      if (!shutdownNotifier.shouldShutdown()) {
        logManager.logException(Level.WARNING, e,
            "Unable to find target locations. Defaulting to selecting all locations as potential target locations.");
      } else {
        logManager.logDebugException(e);
      }
      return allNodes;
    }
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider#tryGetAutomatonTargetLocations(org.sosy_lab.cpachecker.cfa.model.CFANode)
   */
  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(CFANode pRootNode) {
    return tryGetAutomatonTargetLocations(pRootNode, Optional.<Automaton>absent());
  }
}
