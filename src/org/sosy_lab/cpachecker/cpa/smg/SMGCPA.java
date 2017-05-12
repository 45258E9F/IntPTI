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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;

import java.util.logging.Level;

@Options(prefix = "cpa.smg")
public class SMGCPA
    implements ConfigurableProgramAnalysis, ConfigurableProgramAnalysisWithConcreteCex {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SMGCPA.class);
  }

  @Option(secure = true, name = "runtimeCheck", description = "Sets the level of runtime checking: NONE, HALF, FULL")
  private SMGRuntimeCheck runtimeCheck = SMGRuntimeCheck.NONE;

  @Option(secure = true, name = "memoryErrors", description = "Determines if memory errors are target states")
  private boolean memoryErrors = true;

  @Option(secure = true, name = "unknownOnUndefined", description = "Emit messages when we encounter non-target undefined behavior")
  private boolean unknownOnUndefined = true;

  @Option(secure = true, name = "stop", toUppercase = true, values = {"SEP", "NEVER"},
      description = "which stop operator to use for the SMGCPA")
  private String stopType = "SEP";

  @Option(secure = true, name = "externalAllocationSize", description = "Default size of externally allocated memory")
  private int externalAllocationSize = Integer.MAX_VALUE;

  public int getExternalAllocationSize() {
    return externalAllocationSize;
  }

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private SMGPrecisionAdjustment precisionAdjustment;

  private final MachineModel machineModel;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private VariableTrackingPrecision precision;


  private SMGCPA(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa) throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    cfa = pCfa;
    machineModel = cfa.getMachineModel();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(config, logger, machineModel);
    precisionAdjustment = new SMGPrecisionAdjustment();

    abstractDomain = DelegateAbstractDomain.<SMGState>getInstance();
    mergeOperator = MergeSepOperator.getInstance();
    //mergeOperator = SMGMerge.getInstance();

    if (stopType.equals("NEVER")) {
      stopOperator = new StopNeverOperator();
    } else {
      stopOperator = new StopSepOperator(abstractDomain);
    }

    precision = initializePrecision(config, pCfa);

    transferRelation = new SMGTransferRelation(config, logger, machineModel);
  }

  public void injectRefinablePrecision() throws InvalidConfigurationException {
    // replace the full precision with an empty, refinable precision
    precision = VariableTrackingPrecision.createRefineablePrecision(config, precision);
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  public SMGState getInitialState(CFANode pNode) {
    SMGState initState = new SMGState(logger, machineModel, memoryErrors, unknownOnUndefined,
        runtimeCheck, externalAllocationSize);

    try {
      initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
    } catch (SMGInconsistentException exc) {
      logger.log(Level.SEVERE, exc.getMessage());
    }

    if (pNode instanceof CFunctionEntryNode) {
      CFunctionEntryNode functionNode = (CFunctionEntryNode) pNode;
      try {
        initState.addStackFrame(functionNode.getFunctionDefinition());
        initState.performConsistencyCheck(SMGRuntimeCheck.FULL);
      } catch (SMGInconsistentException exc) {
        logger.log(Level.SEVERE, exc.getMessage());
      }
    }

    return initState;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return getInitialState(pNode);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  private VariableTrackingPrecision initializePrecision(Configuration config, CFA cfa)
      throws InvalidConfigurationException {
    return VariableTrackingPrecision
        .createStaticPrecision(config, cfa.getVarClassification(), getClass());
  }

  @Override
  public ConcreteStatePath createConcreteStatePath(ARGPath pPath) {

    return new SMGConcreteErrorPathAllocator(assumptionToEdgeAllocator)
        .allocateAssignmentsToPath(pPath);
  }

  public LogManager getLogger() {
    return logger;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public CFA getCFA() {
    return cfa;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }
}
