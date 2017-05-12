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
package org.sosy_lab.cpachecker.cpa.livevar;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.LiveVariables;

import java.util.Collections;
import java.util.logging.Level;

@Options
public class LiveVariablesCPA implements ConfigurableProgramAnalysis {

  @Option(secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN"},
      description = "which merge operator to use for LiveVariablesCPA")
  private String mergeType = "JOIN";

  @Option(secure = true, name = "stop", toUppercase = true, values = {"SEP", "JOIN", "NEVER"},
      description = "which stop operator to use for LiveVariablesCPA")
  private String stopType = "SEP";

  private final AbstractDomain domain;
  private final LiveVariablesTransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final LogManager logger;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LiveVariablesCPA.class);
  }

  private LiveVariablesCPA(
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA cfa) throws InvalidConfigurationException {
    pConfig.inject(this, LiveVariablesCPA.class);
    logger = pLogger;
    domain = DelegateAbstractDomain.<LiveVariablesState>getInstance();

    if (!cfa.getVarClassification().isPresent() && cfa.getLanguage() == Language.C) {
      throw new AssertionError("Without information of the variable classification"
          + " the live variables analysis cannot be used.");
    }
    transfer =
        new LiveVariablesTransferRelation(cfa.getVarClassification(), pConfig, cfa.getLanguage());

    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else {
      merge = new MergeJoinOperator(domain);
    }

    if (stopType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    } else {
      stop = new StopSepOperator(domain);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (pNode instanceof FunctionExitNode) {
      FunctionExitNode eNode = (FunctionExitNode) pNode;
      Optional<? extends AVariableDeclaration> returnVarName =
          eNode.getEntryNode().getReturnVariable();

      // p.e. a function void foo();
      if (!returnVarName.isPresent()) {
        return new LiveVariablesState();

        // all other function types
      } else {

        final Wrapper<ASimpleDeclaration> wrappedVar =
            LiveVariables.LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration) returnVarName.get());
        transfer.putInitialLiveVariables(pNode, Collections.singleton(wrappedVar));
        return new LiveVariablesState(ImmutableSet.of(wrappedVar));
      }

    } else {
      logger.log(Level.FINEST,
          "No FunctionExitNode given, thus creating initial state without having the return variable.");
      return new LiveVariablesState();
    }
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  /**
   * Returns the liveVariables that are currently computed. Calling this method
   * makes only sense if the analysis was completed
   *
   * @return a Multimap containing the variables that are live at each location
   */
  public Multimap<CFANode, Wrapper<ASimpleDeclaration>> getLiveVariables() {
    return transfer.getLiveVariables();
  }

}
