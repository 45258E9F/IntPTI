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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.AuxiliaryComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.CachingRelevantPredicatesComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RefineableOccurrenceComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;


/**
 * Implements an BAM-based predicate CPA.
 */
@Options(prefix = "cpa.predicate.bam")
public class BAMPredicateCPA extends PredicateCPA implements ConfigurableProgramAnalysisWithBAM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMPredicateCPA.class).withOptions(BAMBlockOperator.class);
  }

  private final BAMPredicateReducer reducer;
  private final BAMBlockOperator blk;
  private RelevantPredicatesComputer relevantPredicatesComputer;

  @Option(secure = true, description = "whether to use auxiliary predidates for reduction")
  private boolean auxiliaryPredicateComputer = true;


  private BAMPredicateCPA(
      Configuration config, LogManager logger,
      BAMBlockOperator pBlk, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException {
    super(config, logger, pBlk, pCfa, pShutdownNotifier);

    config.inject(this, BAMPredicateCPA.class);

    FormulaManagerView fmgr = getSolver().getFormulaManager();
    RelevantPredicatesComputer relevantPredicatesComputer;
    if (auxiliaryPredicateComputer) {
      relevantPredicatesComputer = new AuxiliaryComputer(fmgr);
    } else {
      relevantPredicatesComputer = new RefineableOccurrenceComputer(fmgr);
    }
    this.relevantPredicatesComputer =
        new CachingRelevantPredicatesComputer(relevantPredicatesComputer);

    reducer = new BAMPredicateReducer(fmgr.getBooleanFormulaManager(), this);
    blk = pBlk;
  }

  RelevantPredicatesComputer getRelevantPredicatesComputer() {
    return relevantPredicatesComputer;
  }

  void setRelevantPredicatesComputer(RelevantPredicatesComputer pRelevantPredicatesComputer) {
    relevantPredicatesComputer = pRelevantPredicatesComputer;
  }

  BlockPartitioning getPartitioning() {
    return blk.getPartitioning();
  }

  @Override
  public BAMPredicateReducer getReducer() {
    return reducer;
  }

  public void setPartitioning(BlockPartitioning partitioning) {
    blk.setPartitioning(partitioning);
  }
}
