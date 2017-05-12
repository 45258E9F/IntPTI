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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import com.google.common.base.Optional;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Deque;
import java.util.HashSet;

/**
 * Edge interpolator for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 * Creates {@link SymbolicInterpolant SymbolicInterpolants} based on a combination of
 * {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} and
 * <code>ConstraintsCPA</code>.
 */

@Options(prefix = "cpa.value.symbolic.refinement")
public class ElementTestingSymbolicEdgeInterpolator
    implements SymbolicEdgeInterpolator {

  private enum RefinementStrategy {
    CONSTRAINTS_FIRST,
    VALUES_FIRST,
    VALUES_ONLY
  }

  @Option(description = "Whether to try to not use any constraints in refinement")
  private boolean avoidConstraints = true;

  @Option(description = "The refinement strategy to use")
  private RefinementStrategy strategy = RefinementStrategy.CONSTRAINTS_FIRST;

  private final FeasibilityChecker<ForgettingCompositeState> checker;
  private final StrongestPostOperator<ForgettingCompositeState> strongestPost;
  private final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant>
      interpolantManager;
  private final MachineModel machineModel;

  private final ShutdownNotifier shutdownNotifier;
  private Precision valuePrecision;

  private int interpolationQueries = 0;

  public ElementTestingSymbolicEdgeInterpolator(
      final FeasibilityChecker<ForgettingCompositeState> pChecker,
      final StrongestPostOperator<ForgettingCompositeState> pStrongestPost,
      final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> pInterpolantManager,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    pConfig.inject(this);

    checker = pChecker;
    strongestPost = pStrongestPost;
    interpolantManager = pInterpolantManager;
    shutdownNotifier = pShutdownNotifier;
    valuePrecision = VariableTrackingPrecision.createStaticPrecision(
        pConfig, pCfa.getVarClassification(), ValueAnalysisCPA.class);
    machineModel = pCfa.getMachineModel();
  }

  @Override
  public SymbolicInterpolant deriveInterpolant(
      final ARGPath pErrorPath,
      final CFAEdge pCurrentEdge,
      final Deque<ForgettingCompositeState> pCallstack,
      final PathPosition pLocationInPath,
      final SymbolicInterpolant pInputInterpolant
  ) throws CPAException, InterruptedException {

    interpolationQueries = 0;

    ForgettingCompositeState originState = pInputInterpolant.reconstructState();
    Optional<ForgettingCompositeState> maybeSuccessor =
        strongestPost.getStrongestPost(originState, valuePrecision, pCurrentEdge);

    if (!maybeSuccessor.isPresent()) {
      return interpolantManager.getFalseInterpolant();
    }

    ForgettingCompositeState successorState = maybeSuccessor.get();

    // if nothing changed we keep the same interpolant
    if (originState.equals(successorState)) {
      return pInputInterpolant;
    }

    ARGPath suffix = pLocationInPath.iterator().getSuffixExclusive();

    // if the suffix is contradicting by itself, the interpolant can be true
    if (!isPathFeasible(suffix, ForgettingCompositeState.getInitialState(machineModel))) {
      return interpolantManager.getTrueInterpolant();
    }

    ForgettingCompositeState necessaryInfo = reduceToNecessaryState(successorState, suffix);

    return interpolantManager.createInterpolant(necessaryInfo);
  }

  private ForgettingCompositeState reduceToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    ForgettingCompositeState reducedState = pSuccessorState;
    boolean reduceConstraints = true;

    if (avoidConstraints) {
      reducedState = removeAllConstraints(pSuccessorState);

      if (isPathFeasible(pSuffix, reducedState)) {
        reducedState = pSuccessorState;
      } else {
        reduceConstraints = false;
      }
    }

    switch (strategy) {
      case CONSTRAINTS_FIRST:
        if (reduceConstraints) {
          reducedState = reduceConstraintsToNecessaryState(reducedState, pSuffix);
        }
        reducedState = reduceValuesToNecessaryState(reducedState, pSuffix);
        break;
      case VALUES_ONLY:
        reducedState = reduceValuesToNecessaryState(reducedState, pSuffix);
        break;
      case VALUES_FIRST:
        reducedState = reduceValuesToNecessaryState(reducedState, pSuffix);
        reducedState = reduceConstraintsToNecessaryState(reducedState, pSuffix);
        break;
      default:
        throw new AssertionError("Unhandled strategy " + strategy);
    }

    return reducedState;
  }

  private ForgettingCompositeState removeAllConstraints(final ForgettingCompositeState pState) {
    IdentifierAssignment definiteAssignments = pState.getConstraintsState().getDefiniteAssignment();

    return new ForgettingCompositeState(pState.getValueState(),
        new ConstraintsState(new HashSet<Constraint>(), definiteAssignments));
  }

  private ForgettingCompositeState reduceConstraintsToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    for (Constraint c : pSuccessorState.getTrackedConstraints()) {
      shutdownNotifier.shutdownIfNecessary();
      pSuccessorState.forget(c);

      // if the suffix is feasible without the just removed constraint, it is necessary
      // for proving the error path's infeasibility and as such we have to re-add it.
      if (isPathFeasible(pSuffix, pSuccessorState)) {
        pSuccessorState.remember(c);
      }
    }

    return pSuccessorState;
  }

  private ForgettingCompositeState reduceValuesToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    for (MemoryLocation l : pSuccessorState.getTrackedMemoryLocations()) {
      shutdownNotifier.shutdownIfNecessary();

      ValueAnalysisInformation forgottenInfo = pSuccessorState.forget(l);

      // if the suffix is feasible without the just removed constraint, it is necessary
      // for proving the error path's infeasibility and as such we have to re-add it.
      //noinspection ConstantConditions
      if (isPathFeasible(pSuffix, pSuccessorState)) {
        pSuccessorState.remember(l, forgottenInfo);
      }
    }

    return pSuccessorState;
  }

  private boolean isPathFeasible(
      final ARGPath pRemainingErrorPath,
      final ForgettingCompositeState pState
  ) throws CPAException, InterruptedException {
    interpolationQueries++;
    return checker.isFeasible(pRemainingErrorPath, pState);
  }

  @Override
  public int getNumberOfInterpolationQueries() {
    return interpolationQueries;
  }
}
