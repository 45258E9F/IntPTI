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
package org.sosy_lab.cpachecker.cpa.octagon.refiner;

import static org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision.createStaticPrecision;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.refinement.UseDefRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class OctagonAnalysisFeasabilityChecker {

  private final TransferRelation transfer;

  private final ShutdownNotifier shutdownNotifier;
  private final ARGPath checkedPath;
  private final ARGPath foundPath;

  public OctagonAnalysisFeasabilityChecker(
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      ARGPath pPath,
      Class<? extends ConfigurableProgramAnalysis> pClass,
      Optional<VariableClassification> pVarClass,
      TransferRelation pTransfer,
      AbstractState pInitialState)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    shutdownNotifier = pShutdownNotifier;

    // use the normal configuration for creating the transferrelation
    transfer = pTransfer;
    checkedPath = pPath;

    foundPath = getInfeasiblePrefix(createStaticPrecision(pConfig, pVarClass, pClass),
        pInitialState);
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @return true, if the path is feasible, else false
   */
  public boolean isFeasible() {
    return checkedPath.size() == foundPath.size();
  }

  public Multimap<CFANode, MemoryLocation> getPrecisionIncrement() {
    if (isFeasible()) {
      return ArrayListMultimap.<CFANode, MemoryLocation>create();
    } else {

      Multimap<CFANode, MemoryLocation> increment =
          ArrayListMultimap.<CFANode, MemoryLocation>create();
      for (MemoryLocation loc : getMemoryLocationsFromUseDefRelation()) {
        increment.put(new CFANode("BOGUS-NODE"), loc);
      }

      return increment;
    }
  }

  /**
   * This method returns the variables contained in the use-def relation
   * of the last (failing) assume edge in the found error path.
   */
  private FluentIterable<MemoryLocation> getMemoryLocationsFromUseDefRelation() {
    UseDefRelation useDefRelation =
        new UseDefRelation(foundPath, Collections.<String>emptySet(), false);

    return FluentIterable.from(useDefRelation.getUsesAsQualifiedName())
        .transform(MemoryLocation.FROM_STRING_TO_MEMORYLOCATION);
  }

  /**
   * This method obtains the prefix of the path, that is infeasible by itself. If the path is
   * feasible, the whole path is returned
   *
   * @param pPrecision the precision to use
   * @param pInitial   the initial state
   * @return the prefix of the path that is feasible by itself
   */
  private ARGPath getInfeasiblePrefix(
      final VariableTrackingPrecision pPrecision,
      final AbstractState pInitial)
      throws CPAException, InterruptedException {
    try {
      Collection<AbstractState> next = Lists.newArrayList(pInitial);

      Collection<AbstractState> successors = new HashSet<>();

      PathIterator pathIt = checkedPath.fullPathIterator();

      while (pathIt.hasNext()) {
        CFAEdge edge = pathIt.getOutgoingEdge();
        successors.clear();

        for (AbstractState st : next) {
          successors.addAll(transfer.getAbstractSuccessorsForEdge(
              st, Lists.<AbstractState>newArrayList(),
              pPrecision, edge));

          // computing the feasibility check takes sometimes much time with octagons
          // so we let the shutdownNotifer cancel the computation if necessary
          shutdownNotifier.shutdownIfNecessary();
        }

        pathIt.advance();

        // no successors => path is infeasible
        if (successors.isEmpty()) {
          break;
        }

        // get matching successor state and apply precision
        next.clear();
        next.addAll(successors);
      }

      return pathIt.getPrefixInclusive();

    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(),
          e);
    }
  }

}
