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
package org.sosy_lab.cpachecker.cpa.coverage;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.CoverageMode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import java.io.PrintStream;
import java.util.Set;

@Options
public class CoverageStatistics extends AbstractStatistics {

  @Option(secure = true, name = "coverage.stdout",
      description = "print coverage summary to stdout")
  private boolean writeToStdout = true;

  @Option(secure = true, name = "coverage.export",
      description = "print coverage info to file")
  private boolean writeToFile = true;

  @Option(secure = true, name = "coverage.file",
      description = "print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Paths.get("coverage.info");

  private final LogManager logger;
  private final CoverageData cov;
  private final CFA cfa;

  public CoverageStatistics(Configuration pConfig, LogManager pLogger, CFA pCFA, CoverageData pCov)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.logger = pLogger;
    this.cov = pCov;
    this.cfa = pCFA;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {

    if (cov.getCoverageMode() == CoverageMode.REACHED) {
      computeCoverageFromReached(pReached);
    }

    if (writeToStdout) {
      CoverageReportStdoutSummary writer = new CoverageReportStdoutSummary();
      writer.write(cov, pOut);
    }

    if (writeToFile && outputCoverageFile != null) {
      CoverageReportGcov writer = new CoverageReportGcov(logger);
      writer.write(cov, outputCoverageFile);
    }

  }

  @Override
  public String getName() {
    return String.format("Code Coverage (Mode: %s)", cov.getCoverageMode().toString());
  }

  public void computeCoverageFromReached(
      final ReachedSet pReached) {

    Set<CFANode> reachedLocations = getAllLocationsFromReached(pReached);

    //Add information about visited locations
    for (CFANode node : cfa.getAllNodes()) {
      //This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        boolean visited = reachedLocations.contains(edge.getPredecessor())
            && reachedLocations.contains(edge.getSuccessor());

        if (edge instanceof MultiEdge) {
          for (CFAEdge innerEdge : ((MultiEdge) edge).getEdges()) {
            cov.handleEdgeCoverage(innerEdge, visited);
          }
        } else {
          cov.handleEdgeCoverage(edge, visited);
        }
      }
    }

    // Add information about visited functions
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      if (cov.putExistingFunction(entryNode)) {
        if (reachedLocations.contains(entryNode)) {
          cov.addVisitedFunction(entryNode);
        }
      }
    }

  }

  private Set<CFANode> getAllLocationsFromReached(ReachedSet pReached) {
    if (pReached instanceof ForwardingReachedSet) {
      pReached = ((ForwardingReachedSet) pReached).getDelegate();
    }

    if (pReached instanceof LocationMappedReachedSet) {
      return ((LocationMappedReachedSet) pReached).getLocations();

    } else {
      return from(pReached)
          .transform(EXTRACT_LOCATION)
          .filter(notNull())
          .toSet();
    }
  }

}
