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
package org.sosy_lab.cpachecker.core.phase;

import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationWithCheck;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.phase.result.AlgorithmPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.phase.util.CPAPhases;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "phase.result")
public class AnalyzingResultPhase extends CPAPhase {

  private AlgorithmStatus algorithmStatus;
  private
  @Nullable
  ReachedSet reachedSet;

  @Option(secure = true, description = "Do not report unknown if analysis terminates")
  private boolean unknownAsTrue = false;

  public AnalyzingResultPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
    algorithmStatus = null;
    reachedSet = null;
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    Collection<? extends CPAPhase> algorithmPhases =
        CPAPhases.extractPhasesByResultType(prevPhases, AlgorithmPhaseResult.class);
    if (algorithmPhases.size() != 1) {
      throw new InvalidConfigurationException(
          "One algorithm phase allowed in predecessors");
    }
    CPAPhase onlyAlgorithmPhase = Iterables.getOnlyElement(algorithmPhases);
    AlgorithmPhaseResult result = (AlgorithmPhaseResult) (onlyAlgorithmPhase.getResult());
    algorithmStatus = result.getStatus();
    reachedSet = result.getReachedSet();
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    // dump delayed reported errors
    Optional<ConfigurableProgramAnalysis> currentCPA = GlobalInfo.getInstance().getCPA();
    if (currentCPA.isPresent()) {
      ConfigurableProgramAnalysis cpa = currentCPA.get();
      CompositeCPA compositeCPA = CPAs.retrieveCPA(cpa, CompositeCPA.class);
      if (compositeCPA != null) {
        for (ConfigurableProgramAnalysis singleCPA : compositeCPA.getWrappedCPAs()) {
          TransferRelation transfer = singleCPA.getTransferRelation();
          if (transfer instanceof TransferRelationWithCheck) {
            Collection<ErrorReport> errors = ((TransferRelationWithCheck) transfer)
                .dumpErrorsAfterAnalysis();
            for (ErrorReport error : errors) {
              GlobalInfo.getInstance().updateErrorCollector(error);
            }
          }
        }
      }
    }

    String violatedPropertyDescription = "";
    Result finalResult;
    if (reachedSet != null) {
      Set<Property> violatedProperties = findViolatedProperties(reachedSet);
      if (!violatedProperties.isEmpty()) {
        // there is violated property
        violatedPropertyDescription = Joiner.on(", ").join(violatedProperties);
        if (!algorithmStatus.isPrecise()) {
          finalResult = Result.UNKNOWN;
        } else {
          finalResult = Result.FALSE;
        }
      } else {
        finalResult = analyzeResult(reachedSet, algorithmStatus.isSound());
        if (unknownAsTrue && finalResult == Result.UNKNOWN) {
          finalResult = Result.TRUE;
        }
      }
    } else {
      if (GlobalInfo.getInstance().getBugSize() > 0) {
        finalResult = Result.FALSE;
      } else {
        finalResult = Result.TRUE;
      }
    }
    // create a {@link CPAcheckerResult} object as the final result
    currResult = new CPAcheckerResult(finalResult, violatedPropertyDescription,
        reachedSet, stats);

    return CPAPhaseStatus.SUCCESS;
  }

  static Set<Property> findViolatedProperties(final ReachedSet reached) {
    final Set<Property> result = Sets.newHashSet();
    for (AbstractState e : FluentIterable.from(reached).filter(IS_TARGET_STATE)) {
      Targetable t = (Targetable) e;
      result.addAll(t.getViolatedProperties());
    }
    return result;
  }

  private Result analyzeResult(ReachedSet pReached, boolean isSound) {
    if (pReached.hasWaitingState()) {
      logger.log(Level.WARNING, "Analysis not completed: there are still states to be processed");
      return Result.UNKNOWN;
    }

    if (!isSound) {
      logger.log(Level.WARNING, "Analysis incomplete: no error found, but coverage is not full");
      return Result.UNKNOWN;
    }
    // by default, there are no violated properties
    return Result.TRUE;
  }
}
