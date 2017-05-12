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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.defaults.BreakOnTargetsPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.AutomatonInfo;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * This class implements an AutomatonAnalysis as described in the related Documentation.
 */
@Options(prefix = "cpa.automaton")
public class ControlAutomatonCPA
    implements ConfigurableProgramAnalysis, StatisticsProvider, ConfigurableProgramAnalysisWithBAM,
               ProofChecker {

  @Option(secure = true, name = "dotExport",
      description = "export automaton to file")
  private boolean export = false;

  @Option(secure = true, name = "dotExportFile",
      description = "file for saving the automaton in DOT format (%s will be replaced with automaton name)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportFile = PathTemplate.ofFormatString("%s.dot");

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ControlAutomatonCPA.class);
  }

  @Option(secure = true, required = false,
      description = "file with automaton specification for ObserverAutomatonCPA and ControlAutomatonCPA")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path inputFile = null;

  @Option(secure = true, description = "signal the analysis to break in case the given number of error state is reached ")
  private int breakOnTargetState = 1;

  @Option(secure = true, description =
      "the maximum number of iterations performed after the initial error is found, despite the limit"
          + "given as cpa.automaton.breakOnTargetState is not yet reached")
  private int extraIterationsLimit = -1;

  @Option(secure = true, description = "Whether to treat automaton states with an internal error state as targets. This should be the standard use case.")
  private boolean treatErrorsAsTargets = true;

  @Option(secure = true, description = "Merge two automata states if one of them is TOP.")
  private boolean mergeOnTop = false;

  private final Automaton automaton;
  private final AutomatonState topState = new AutomatonState.TOP(this);
  private final AutomatonState bottomState = new AutomatonState.BOTTOM(this);

  private final AbstractDomain automatonDomain = new FlatLatticeDomain(topState);
  private final StopOperator stopOperator = new StopSepOperator(automatonDomain);
  private final AutomatonTransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final MergeOperator mergeOperator;
  private final Statistics stats = new AutomatonStatistics(this);

  private final CFA cfa;
  private final LogManager logger;

  protected ControlAutomatonCPA(
      @OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {

    pConfig.inject(this, ControlAutomatonCPA.class);

    this.cfa = pCFA;
    this.logger = pLogger;

    this.transferRelation = new AutomatonTransferRelation(this, pLogger);
    this.precisionAdjustment = composePrecisionAdjustmentOp(pConfig);

    if (mergeOnTop) {
      this.mergeOperator = new AutomatonTopMergeOperator(automatonDomain, topState);
    } else {
      this.mergeOperator = MergeSepOperator.getInstance();
    }

    if (pAutomaton != null) {
      this.automaton = pAutomaton;

    } else if (inputFile == null) {
      throw new InvalidConfigurationException(
          "Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");

    } else {
      this.automaton = constructAutomataFromFile(pConfig, inputFile);
    }

    pLogger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");

    if (export && exportFile != null) {
      try (Writer w = Files.openOutputFile(exportFile.getPath(automaton.getName()))) {
        automaton.writeDotFile(w);
      } catch (IOException e) {
        pLogger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
    }
  }

  private Automaton constructAutomataFromFile(Configuration pConfig, Path pFile)
      throws InvalidConfigurationException {

    Scope scope = cfa.getLanguage() == Language.C
                  ? new CProgramScope(cfa, logger)
                  : DummyScope.getInstance();

    List<Automaton> lst = AutomatonParser
        .parseAutomatonFile(pFile, pConfig, logger, cfa.getMachineModel(), scope,
            cfa.getLanguage());

    if (lst.isEmpty()) {
      throw new InvalidConfigurationException(
          "Could not find automata in the file " + inputFile.toAbsolutePath());
    } else if (lst.size() > 1) {
      throw new InvalidConfigurationException("Found " + lst.size()
          + " automata in the File " + inputFile.toAbsolutePath()
          + " The CPA can only handle ONE Automaton!");
    }

    return lst.get(0);
  }

  private PrecisionAdjustment composePrecisionAdjustmentOp(Configuration pConfig)
      throws InvalidConfigurationException {

    final PrecisionAdjustment lPrecisionAdjustment;

    if (breakOnTargetState > 0) {
      final int pFoundTargetLimit = breakOnTargetState;
      final int pExtraIterationsLimit = extraIterationsLimit;
      lPrecisionAdjustment =
          new BreakOnTargetsPrecisionAdjustment(pFoundTargetLimit, pExtraIterationsLimit);

    } else {
      lPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    }

    return new ControlAutomatonPrecisionAdjustment(pConfig, topState, lPrecisionAdjustment);
  }

  Automaton getAutomaton() {
    return this.automaton;
  }

  public void registerInAutomatonInfo(AutomatonInfo info) {
    info.register(automaton, this);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return automatonDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return AutomatonState
        .automatonStateFactory(automaton.getInitialVariables(), automaton.getInitialState(), this,
            0, 0, null);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public AutomatonTransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

  public AutomatonState getBottomState() {
    return this.bottomState;
  }

  public AutomatonState getTopState() {
    return this.topState;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement,
      List<AbstractState> otherStates,
      CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    return pSuccessors.equals(getTransferRelation().getAbstractSuccessorsForEdge(
        pElement, otherStates, SingletonPrecision.getInstance(), pCfaEdge));
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException, InterruptedException {
    return getAbstractDomain().isLessOrEqual(pElement, pOtherElement);
  }

  MachineModel getMachineModel() {
    return cfa.getMachineModel();
  }

  LogManager getLogManager() {
    return logger;
  }

  boolean isTreatingErrorsAsTargets() {
    return treatErrorsAsTargets;
  }
}
