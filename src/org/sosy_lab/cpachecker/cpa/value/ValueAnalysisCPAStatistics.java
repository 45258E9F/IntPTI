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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.logging.Level;

@Options(prefix = "cpa.value")
public class ValueAnalysisCPAStatistics implements Statistics {

  @Option(secure = true, description = "target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path precisionFile = null;

  private final ValueAnalysisCPA cpa;

  public ValueAnalysisCPAStatistics(ValueAnalysisCPA cpa, Configuration config)
      throws InvalidConfigurationException {
    this.cpa = cpa;

    config.inject(this, ValueAnalysisCPAStatistics.class);
  }

  @Override
  public String getName() {
    return "ValueAnalysisCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    StatInt numberOfVariables = new StatInt(StatKind.COUNT, "Number of variables");
    StatInt numberOfGlobalVariables = new StatInt(StatKind.COUNT, "Number of global variables");

    for (AbstractState currentAbstractState : reached) {
      ValueAnalysisState currentState =
          AbstractStates.extractStateByType(currentAbstractState, ValueAnalysisState.class);

      numberOfVariables.setNextValue(currentState.getSize());
      numberOfGlobalVariables.setNextValue(currentState.getNumberOfGlobalVariables());
    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(numberOfVariables);
    writer.put(numberOfGlobalVariables);

    if (precisionFile != null) {
      exportPrecision(reached);
    }
  }

  /**
   * This method exports the precision to file.
   *
   * @param reached the set of reached states.
   */
  private void exportPrecision(ReachedSet reached) {
    VariableTrackingPrecision consolidatedPrecision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(reached);
    try (Writer writer = Files.openOutputFile(precisionFile)) {
      consolidatedPrecision.serialize(writer);
    } catch (IOException e) {
      cpa.getLogger()
          .logUserException(Level.WARNING, e, "Could not write value-analysis precision to file");
    }
  }
}
