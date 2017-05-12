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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPathExporter;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;
import org.sosy_lab.cpachecker.util.cwriter.PathToConcreteProgramTranslator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Options(prefix = "cpa.arg.errorPath")
public class CEXExporter {

  enum CounterexampleExportType {
    CBMC,
    CONCRETE_EXECUTION;
  }

  @Option(secure = true, name = "export",
      description = "export error path to file, if one is found")
  private boolean exportErrorPath = true;

  @Option(secure = true, name = "file",
      description = "export error path as text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathFile = PathTemplate.ofFormatString("ErrorPath.%d.txt");

  @Option(secure = true, name = "core",
      description = "export error path core as text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathCoreFile = PathTemplate.ofFormatString("ErrorPath.%d.core.txt");

  @Option(secure = true, name = "source",
      description = "export error path as source file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathSourceFile = PathTemplate.ofFormatString("ErrorPath.%d.c");

  @Option(secure = true, name = "exportAsSource",
      description = "export error path as source file")
  private boolean exportSource = true;

  @Option(secure = true, name = "json",
      description = "export error path as JSON file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathJson = PathTemplate.ofFormatString("ErrorPath.%d.json");

  @Option(secure = true, name = "graph",
      description = "export error path as graph")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathGraphFile = PathTemplate.ofFormatString("ErrorPath.%d.dot");

  @Option(secure = true, name = "automaton",
      description = "export error path as automaton")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathAutomatonFile = PathTemplate.ofFormatString("ErrorPath.%d.spc");

  @Option(secure = true, name = "exportWitness",
      description = "export error path as witness/graphml file")
  private boolean exportWitness = true;

  @Option(secure = true, name = "graphml",
      description = "export error path to file as GraphML automaton")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathAutomatonGraphmlFile =
      PathTemplate.ofFormatString("ErrorPath.%d.graphml");

  @Option(secure = true, name = "codeStyle",
      description = "exports either CMBC format or a concrete path program")
  private CounterexampleExportType codeStyle = CounterexampleExportType.CBMC;

  private final LogManager logger;
  private final ARGPathExporter witnessExporter;


  public CEXExporter(Configuration config, LogManager logger, ARGPathExporter pARGPathExporter)
      throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
    this.witnessExporter = pARGPathExporter;

    if (!exportSource) {
      errorPathSourceFile = null;
    }
    if (!exportWitness) {
      errorPathAutomatonGraphmlFile = null;
    }
    if (errorPathCoreFile == null && errorPathFile == null
        && errorPathGraphFile == null && errorPathJson == null && errorPathSourceFile == null
        && errorPathAutomatonFile == null && errorPathAutomatonGraphmlFile == null) {
      exportErrorPath = false;
    }
  }

  /**
   * Export an Error Trace in different formats, for example as C-file, dot-file or automaton.
   *
   * @param pTargetState        state of an ARG, used as fallback, if pCounterexampleInfo contains
   *                            no targetPath.
   * @param pCounterexampleInfo contains further information and the (optional) targetPath. If the
   *                            targetPath is available, it will be used for the output. Otherwise
   *                            we use backwards reachable states from pTargetState.
   * @param cexIndex            should be a unique index for the CEX and will be used to enumerate
   *                            files.
   */
  public void exportCounterexample(
      final ARGState pTargetState,
      final CounterexampleInfo pCounterexampleInfo,
      int cexIndex) {
    checkNotNull(pTargetState);
    checkNotNull(pCounterexampleInfo);

    if (exportErrorPath) {
      exportCounterexample(pTargetState, cexIndex, pCounterexampleInfo);
    }
  }

  private void exportCounterexample(
      final ARGState lastState,
      final int cexIndex,
      final CounterexampleInfo counterexample) {

    final ARGPath targetPath = counterexample.getTargetPath();
    final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge = Predicates.in(
        new HashSet<>(targetPath.getStatePairs()));
    final ARGState rootState = targetPath.getFirstState();

    writeErrorPathFile(errorPathFile, cexIndex, counterexample);

    if (errorPathCoreFile != null) {
      // the shrinked errorPath only includes the nodes,
      // that are important for the error, it is not a complete path,
      // only some nodes of the targetPath are part of it
      ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
      List<CFAEdge> shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);
      writeErrorPathFile(errorPathCoreFile, cexIndex,
          Appenders.forIterable(Joiner.on('\n'), shrinkedErrorPath));
    }

    writeErrorPathFile(errorPathJson, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        counterexample.toJSON(pAppendable);
      }
    });

    final Set<ARGState> pathElements;
    Appender pathProgram = null;
    if (counterexample.isPreciseCounterExample()) {
      pathElements = targetPath.getStateSet();

      if (errorPathSourceFile != null) {
        switch (codeStyle) {
          case CONCRETE_EXECUTION:
            pathProgram = PathToConcreteProgramTranslator
                .translateSinglePath(targetPath, counterexample.getCFAPathWithAssignments());
            break;
          case CBMC:
            pathProgram = PathToCTranslator.translateSinglePath(targetPath);
            break;
          default:
            throw new AssertionError("Unhandled case statement: " + codeStyle);
        }
      }

    } else {
      // Imprecise error path.
      // For the text export, we have no other chance,
      // but for the C code and graph export we use all existing paths
      // to avoid this problem.
      pathElements = ARGUtils.getAllStatesOnPathsTo(lastState);

      if (errorPathSourceFile != null) {
        switch (codeStyle) {
          case CONCRETE_EXECUTION:
            logger.log(Level.WARNING,
                "Cannot export imprecise counterexample to C code for concrete execution.");
            break;
          case CBMC:
            pathProgram = PathToCTranslator.translatePaths(rootState, pathElements);
            break;
          default:
            throw new AssertionError("Unhandled case statement: " + codeStyle);
        }
      }
    }

    if (pathProgram != null) {
      writeErrorPathFile(errorPathSourceFile, cexIndex, pathProgram);
    }

    writeErrorPathFile(errorPathGraphFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        ARGToDotWriter.write(pAppendable, rootState,
            ARGUtils.CHILDREN_OF_STATE,
            Predicates.in(pathElements),
            isTargetPathEdge);
      }
    });

    writeErrorPathFile(errorPathAutomatonFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        ARGUtils.producePathAutomaton(pAppendable, rootState, pathElements,
            "ErrorPath" + cexIndex,
            counterexample);
      }
    });

    for (Pair<Object, PathTemplate> info : counterexample.getAllFurtherInformation()) {
      if (info.getSecond() != null) {
        writeErrorPathFile(info.getSecond(), cexIndex, info.getFirst());
      }
    }

    writeErrorPathFile(errorPathAutomatonGraphmlFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        witnessExporter.writeErrorWitness(pAppendable, rootState,
            Predicates.in(pathElements),
            isTargetPathEdge,
            counterexample);
      }
    });
  }

  private void writeErrorPathFile(PathTemplate template, int cexIndex, Object content) {
    if (template != null) {
      // fill in index in file name
      Path file = template.getPath(cexIndex);

      try {
        Files.writeFile(file, content);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write information about the error path to file");
      }
    }
  }
}
