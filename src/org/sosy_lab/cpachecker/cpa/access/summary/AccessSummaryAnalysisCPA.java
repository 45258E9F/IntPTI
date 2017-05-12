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
package org.sosy_lab.cpachecker.cpa.access.summary;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.summary.computer.AccessSummaryComputer;
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
import org.sosy_lab.cpachecker.cpa.access.AccessAnalysisState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by tomgu on 2/7/17.
 * This CPA analyzes what variables are read/write (access mode) of a single function,
 * which means, the variables may change the state of the program.
 * That is the variables in function depend on outside operation.
 * For read, outside will read this variable
 * For write, this variable
 *
 * In details: parameters of function will be read, pointer or array will be written.
 * global will be read, written
 *
 * For example:
 * f(int *p){*p = readInt();} => p|* is written
 * main(){int *p = _; f(p)}
 * When we analyze main, f(p) statement, we know p in f() is written
 *
 * It should be:
 * (1) used in backward analysis (with LocationCPABackwards)
 * (2) use CPABasedSummaryComputer to get summary
 *
 * The result is an over-approximation with respect to the prefix relation. I.e.,
 * if an accesspath is read (write),
 * the computation result should contain one of its prefix being read (write).
 */
@Options(prefix = "cpa.access.summary") // for now, no Option is injected
public class AccessSummaryAnalysisCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain domain;
  private final AccessSummaryAnalysisTransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final LogManager logger;

  private AccessSummaryComputer computer;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AccessSummaryAnalysisCPA.class);
  }

  private AccessSummaryAnalysisCPA(
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA cfa) throws InvalidConfigurationException {
    pConfig.inject(this, AccessSummaryAnalysisCPA.class);

    logger = pLogger;
    domain = DelegateAbstractDomain.<AccessAnalysisState>getInstance();
    transfer = new AccessSummaryAnalysisTransferRelation();
    merge = new MergeJoinOperator(domain);
    stop = new StopSepOperator(domain);
  }

  public void setComputer(AccessSummaryComputer pAccessSummaryComputer) {
    this.computer = pAccessSummaryComputer;
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
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) {
    // first we need to update summary from CPABasedSummaryComputer
    transfer.summary.clear();
    computer.initializeSummaryForCPA(node.getFunctionName(), transfer.summary);
    // init loop
    transfer.initLoopState(node.getFunctionName());
    return new AccessAnalysisState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  /**
   * summarize loop summary instance
   */
  public void getLoopSummaryMap(
      Map<Loop, AccessAnalysisState> internalMaps,
      Map<Loop, AccessAnalysisState> externalMaps) {
    // before we return the loop result, we need erase all the inner variable declaration
    transfer.getLoopStateMapCopy(internalMaps);
    transfer.eraseLoopVariable();
    transfer.getLoopStateMapCopy(externalMaps);
  }

  // tomgu note : here we have to remove parameter's write -> for call by value, only pointer
  //              and array will change the value. we check here, the Util do not have to check
  //      void changeValue(int i, int *p){*p = _; i = _;} => the original write is [p*, i] but only [p*] is written
  public AccessAnalysisState clearParameters(AccessAnalysisState pAccessState, String funName) {
    Preconditions.checkArgument(GlobalInfo.getInstance().getCFAInfo().isPresent());
    AccessAnalysisState state = pAccessState;
    CFunctionDeclaration functionDeclaration =
        GlobalInfo.getInstance().getCFAInfo().get().getFunctionDecls().get
            (funName);
    for (CParameterDeclaration declaration : functionDeclaration.getParameters()) {
      CType type = declaration.getType();
      if (type instanceof CPointerType || type instanceof CArrayType) {
        continue;
      } else {
        // clear this parameter's writeTree
        List<String> path = new ArrayList<>();
        path.add(declaration.getQualifiedName());
        state = state.eraseWrite(path);
      }
    }
    return state;
  }

}
