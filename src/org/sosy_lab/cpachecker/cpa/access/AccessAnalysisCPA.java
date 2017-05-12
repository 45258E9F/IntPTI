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
package org.sosy_lab.cpachecker.cpa.access;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
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
import org.sosy_lab.cpachecker.util.access.AccessSummaryUtil;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * This CPA analyzes what variables are read/write (access mode) It should be: (1) used in backward
 * analysis (with LocationCPABackwards) (2) used with function summary
 * (cpa.location.followFunctionCalls = false) (3) used with per-function analysis
 * (analysis.initialStatesFor = FUNCTION_SINKS)
 *
 * The result is an over-approximation with respect to the prefix relation. I.e., if an accesspath
 * is read (write), the computation result should contain one of its prefix being read (write).
 */
@Options(prefix = "cpa.access") // for now, no Option is injected
public class AccessAnalysisCPA implements ConfigurableProgramAnalysis,
                                          SummaryManager<AccessAnalysisState> {

  private final AbstractDomain domain;
  private final AccessAnalysisTransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final LogManager logger;

  private static final Map<String, AccessAnalysisState> summary = new HashMap<>();

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AccessAnalysisCPA.class);
  }

  private AccessAnalysisCPA(
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA cfa) throws InvalidConfigurationException {
    pConfig.inject(this, AccessAnalysisCPA.class);

    logger = pLogger;
    domain = DelegateAbstractDomain.<AccessAnalysisState>getInstance();
    transfer = new AccessAnalysisTransferRelation(this);

    merge = new MergeJoinOperator(domain);
    stop = new StopSepOperator(domain);

    initializeSummary();
  }

  /**
   * Create empty summary for defined functions
   * Guess an over approximation for undefined functions.
   *
   * @param cfa The entire CFA
   */
  private void initializeSummary() {
    Preconditions.checkArgument(GlobalInfo.getInstance().getCFAInfo().isPresent());
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();
    CFA cfa = cfaInfo.getCFA();

    Map<String, CFunctionDeclaration> funDecls = cfaInfo.getFunctionDecls();
    // defined functions
    for (String fn : cfa.getAllFunctionNames()) {
      summary.put(fn, new AccessAnalysisState());
    }
    for (CFunctionDeclaration funDecl : funDecls.values()) {
      if (!summary.containsKey(funDecl.getName())) {
        System.out.println("Found function " + funDecl.getName());
        // no definition, thus it is a declaration
        AccessAnalysisState state = AccessSummaryUtil.mostGeneralSummary(funDecl);
        // add to summary
        summary.put(funDecl.getName(), state);
      }
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
    // always creating empty state
    return new AccessAnalysisState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public AccessAnalysisState getSummary(String pFunctionName) {
    return summary.get(pFunctionName);
  }

  @Override
  public void setSummary(String pFunctionName, AccessAnalysisState pSummary) {
    summary.put(pFunctionName, pSummary);
  }

}
