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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.IterationStatistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExporter;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA.PartitionState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "cpa.arg")
public class ARGStatistics implements IterationStatistics {

  @Option(secure = true, name = "dumpAfterIteration", description = "Dump all ARG related statistics files after each iteration of the CPA algorithm? (for debugging and demonstration)")
  private boolean dumpArgInEachCpaIteration = false;

  @Option(secure = true, name = "export", description = "export final ARG as .dot file")
  private boolean exportARG = true;

  @Option(secure = true, name = "file",
      description = "export final ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("ARG.dot");

  @Option(secure = true, name = "proofWitness",
      description = "export a proof as .graphml file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path proofWitness = null;

  @Option(secure = true, name = "simplifiedARG.file",
      description = "export final ARG as .dot file, showing only loop heads and function entries/exits")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Paths.get("ARGSimplified.dot");

  @Option(secure = true, name = "refinements.file",
      description = "export simplified ARG that shows all refinements to .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path refinementGraphFile = Paths.get("ARGRefinements.dot");

  private final ARGCPA cpa;

  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;
  private final
  @Nullable
  CEXExporter cexExporter;
  private final ARGPathExporter argPathExporter;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private final LogManager logger;

  public ARGStatistics(
      Configuration config, LogManager pLogger, ARGCPA pCpa,
      MachineModel pMachineModel, @Nullable CEXExporter pCexExporter,
      ARGPathExporter pARGPathExporter) throws InvalidConfigurationException {
    config.inject(this);

    logger = pLogger;
    cpa = pCpa;
    assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(config, logger, pMachineModel);
    cexExporter = pCexExporter;
    argPathExporter = pARGPathExporter;

    if (argFile == null && simplifiedArgFile == null && refinementGraphFile == null
        && proofWitness == null) {
      exportARG = false;
    }
  }

  ARGToDotWriter getRefinementGraphWriter() {
    if (!exportARG || refinementGraphFile == null) {
      return null;
    }

    if (refinementGraphWriter == null) {
      // Open output file for refinement graph,
      // we continuously write into this file during analysis.
      // We do this lazily so that the file is written only if there are refinements.
      try {
        refinementGraphUnderlyingWriter = Files.openOutputFile(refinementGraphFile);
        refinementGraphWriter = new ARGToDotWriter(refinementGraphUnderlyingWriter);
      } catch (IOException e) {
        if (refinementGraphUnderlyingWriter != null) {
          try {
            refinementGraphUnderlyingWriter.close();
          } catch (IOException innerException) {
            e.addSuppressed(innerException);
          }
        }

        logger.logUserException(Level.WARNING, e,
            "Could not write refinement graph to file");

        refinementGraphFile = null; // ensure we won't try again
        refinementGraphUnderlyingWriter = null;
        refinementGraphWriter = null;
      }
    }

    // either both are null or none
    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    return refinementGraphWriter;
  }

  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(
      PrintStream pOut, Result pResult,
      ReachedSet pReached) {

    if (cexExporter == null && !exportARG) {
      return;
    }

    if (argFile != null) {
      argFile = Paths.get(simplifyPath(argFile.getPath()));
    }
    if (simplifiedArgFile != null) {
      simplifiedArgFile = Paths.get(simplifyPath(simplifiedArgFile.getPath()));
    }
    if (refinementGraphFile != null) {
      refinementGraphFile = Paths.get(simplifyPath(refinementGraphFile.getPath()));
    }
    if (proofWitness != null) {
      proofWitness = Paths.get(simplifyPath(proofWitness.getPath()));
    }

    final Map<ARGState, CounterexampleInfo> counterexamples = getAllCounterexamples(pReached);

    if (cexExporter != null) {
      int cexIndex = 0;
      for (Map.Entry<ARGState, CounterexampleInfo> cex : counterexamples.entrySet()) {
        cexExporter.exportCounterexample(cex.getKey(), cex.getValue(), cexIndex++);
      }
    }

    if (exportARG) {
      final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
      for (CounterexampleInfo cex : counterexamples.values()) {
        allTargetPathEdges.addAll(cex.getTargetPath().getStatePairs());
      }

      // The state space might be partitioned ...
      // ... so we would export a separate ARG for each partition ...
      final Set<ARGState> rootStates = ARGUtils.getRootStates(pReached);

      for (ARGState rootState : rootStates) {
        exportARG(rootState, Predicates.in(allTargetPathEdges));
      }
    }
  }

  private String simplifyPath(String path) {
    List<String> names = FluentIterable.from(Splitter.on('/').split(path)).filter(
        new Predicate<String>() {
          @Override
          public boolean apply(String pS) {
            return !pS.equals(".");
          }
        }).toList();
    if (names.isEmpty()) {
      return path;
    }
    String rootName = names.get(0);
    int index;
    for (index = 1; index < names.size(); index++) {
      if (names.get(index).equals(rootName)) {
        break;
      }
    }
    if (index >= names.size() - 1) {
      // no break
      return path;
    }
    List<String> newNames = new ArrayList<>();
    for (int i = 0; i < index; i++) {
      newNames.add(names.get(i));
    }
    newNames.add(names.get(names.size() - 1));
    String newPath = Joiner.on('/').join(newNames);
    return "./".concat(newPath);
  }

  private Path adjustPathNameForPartitioning(ARGState rootState, Path pPath) {
    if (pPath == null) {
      return null;
    }

    PartitionState partyState = AbstractStates.extractStateByType(rootState, PartitionState.class);
    if (partyState == null) {
      return pPath;
    }

    final String partitionKey = partyState.getStateSpacePartition().getPartitionKey().toString();

    int sepIx = pPath.getPath().lastIndexOf(".");
    String prefix = pPath.getPath().substring(0, sepIx);
    String extension = pPath.getPath().substring(sepIx, pPath.getPath().length());
    return Paths.get(prefix + "-" + partitionKey + extension);
  }

  private void exportARG(
      final ARGState rootState,
      final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge) {
    SetMultimap<ARGState, ARGState> relevantSuccessorRelation =
        ARGUtils.projectARG(rootState, ARGUtils.CHILDREN_OF_STATE, ARGUtils.RELEVANT_STATE);
    Function<ARGState, Collection<ARGState>> relevantSuccessorFunction =
        Functions.forMap(relevantSuccessorRelation.asMap(), ImmutableSet.<ARGState>of());

    if (proofWitness != null) {
      try (Writer w = Files
          .openOutputFile(adjustPathNameForPartitioning(rootState, proofWitness))) {
        argPathExporter.writeProofWitness(w, rootState,
            Predicates.alwaysTrue(),
            Predicates.alwaysTrue());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (argFile != null) {
      try (Writer w = Files.openOutputFile(adjustPathNameForPartitioning(rootState, argFile))) {
        ARGToDotWriter.write(w, rootState,
            ARGUtils.CHILDREN_OF_STATE,
            Predicates.alwaysTrue(),
            isTargetPathEdge);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (simplifiedArgFile != null) {
      try (Writer w = Files
          .openOutputFile(adjustPathNameForPartitioning(rootState, simplifiedArgFile))) {
        ARGToDotWriter.write(w, rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            Predicates.alwaysFalse());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    if (refinementGraphUnderlyingWriter != null) {
      try (Writer w = refinementGraphUnderlyingWriter) { // for auto-closing
        // TODO: Support for partitioned state spaces
        refinementGraphWriter.writeSubgraph(rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            Predicates.alwaysFalse());
        refinementGraphWriter.finish();

      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write refinement graph to file");
      }
    }
  }

  private Map<ARGState, CounterexampleInfo> getAllCounterexamples(final ReachedSet pReached) {
    Map<ARGState, CounterexampleInfo> probableCounterexample = cpa.getCounterexamples();
    // This map may contain too many counterexamples
    // (for target states that were in the mean time removed from the ReachedSet),
    // as well as too feww counterexamples
    // (for target states where we don't have a CounterexampleInfo
    // because we did no refinement).
    // So we create a map with all target states,
    // adding the CounterexampleInfo where we have it (null otherwise).

    Map<ARGState, CounterexampleInfo> counterexamples = new HashMap<>();

    for (AbstractState targetState : from(pReached).filter(IS_TARGET_STATE)) {
      ARGState s = (ARGState) targetState;
      CounterexampleInfo cex = probableCounterexample.get(s);
      if (cex == null) {
        ARGPath path = ARGUtils.getOnePathTo(s);
        if (path.getInnerEdges().contains(null)) {
          // path is invalid,
          // this might be a partial path in BAM, from an intermediate TargetState to root of its ReachedSet.
          // TODO this check does not avoid dummy-paths in BAM, that might exist in main-reachedSet.
        } else {

          CFAPathWithAssumptions assignments = createAssignmentsForPath(path);
          // we use the imprecise version of the CounterexampleInfo, due to the possible
          // merges which are done in the used CPAs, but if we can compute a path with assignments,
          // it is probably precise
          if (!assignments.isEmpty()) {
            cex = CounterexampleInfo.feasiblePrecise(path, assignments);
          } else {
            cex = CounterexampleInfo.feasibleImprecise(path);
          }
        }
      }
      if (cex != null) {
        counterexamples.put(s, cex);
      }
    }

    return counterexamples;
  }

  private CFAPathWithAssumptions createAssignmentsForPath(ARGPath pPath) {

    FluentIterable<ConfigurableProgramAnalysisWithConcreteCex> cpas =
        CPAs.asIterable(cpa).filter(ConfigurableProgramAnalysisWithConcreteCex.class);

    CFAPathWithAssumptions result = null;

    // TODO Merge different paths
    for (ConfigurableProgramAnalysisWithConcreteCex wrappedCpa : cpas) {
      ConcreteStatePath path = wrappedCpa.createConcreteStatePath(pPath);
      CFAPathWithAssumptions cexPath = CFAPathWithAssumptions.of(path, assumptionToEdgeAllocator);

      if (result != null) {
        result = result.mergePaths(cexPath);
      } else {
        result = cexPath;
      }
    }

    if (result == null) {
      return CFAPathWithAssumptions.empty();
    } else {
      return result;
    }
  }

  @Override
  public void printIterationStatistics(PrintStream pOut, ReachedSet pReached) {
    if (dumpArgInEachCpaIteration) {
      printStatistics(pOut, Result.UNKNOWN, pReached);
    }
  }
}
