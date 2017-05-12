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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

@Options(prefix = "cpa.predicate")
public class PredicatePrecisionBootstrapper implements StatisticsProvider {

  @Option(secure = true, name = "abstraction.initialPredicates",
      description = "get an initial map of predicates from a list of files (see source doc/examples/predmap.txt for an example)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> predicatesFiles = ImmutableList.of();

  @Option(secure = true, description = "always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  private final FormulaManagerView formulaManagerView;
  private final AbstractionManager abstractionManager;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

  private static class PrecisionBootstrapStatistics extends AbstractStatistics {
  }

  private final PrecisionBootstrapStatistics statistics = new PrecisionBootstrapStatistics();

  public PredicatePrecisionBootstrapper(
      Configuration config, LogManager logger, CFA cfa,
      AbstractionManager abstractionManager, FormulaManagerView formulaManagerView)
      throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.cfa = cfa;

    this.abstractionManager = abstractionManager;
    this.formulaManagerView = formulaManagerView;

    config.inject(this);
  }

  private PredicatePrecision internalPrepareInitialPredicates()
      throws InvalidConfigurationException {

    PredicatePrecision result = PredicatePrecision.empty();

    if (checkBlockFeasibility) {
      result = result.addGlobalPredicates(
          Collections.<AbstractionPredicate>singleton(abstractionManager.makeFalsePredicate()));
    }

    if (!predicatesFiles.isEmpty()) {
      PredicateMapParser parser =
          new PredicateMapParser(config, cfa, logger, formulaManagerView, abstractionManager);

      for (Path predicatesFile : predicatesFiles) {
        try {
          result = result.mergeWith(parser.parsePredicates(predicatesFile));

        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not read predicate map from file");

        } catch (PredicateParsingFailedException e) {
          logger.logUserException(Level.WARNING, e, "Could not read predicate map");
        }
      }
    }

    return result;
  }

  /**
   * Read the (initial) precision (predicates to track) from a file.
   */
  public PredicatePrecision prepareInitialPredicates() throws InvalidConfigurationException {
    PredicatePrecision result = internalPrepareInitialPredicates();

    statistics.addKeyValueStatistic("Init. global predicates", result.getGlobalPredicates().size());
    statistics
        .addKeyValueStatistic("Init. location predicates", result.getLocalPredicates().size());
    statistics
        .addKeyValueStatistic("Init. function predicates", result.getFunctionPredicates().size());

    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
