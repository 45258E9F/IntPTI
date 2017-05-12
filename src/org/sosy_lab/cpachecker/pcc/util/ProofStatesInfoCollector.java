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
package org.sosy_lab.cpachecker.pcc.util;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.PrintStream;

import javax.annotation.Nullable;

@Options(prefix = "pcc")
public class ProofStatesInfoCollector implements Statistics {

  @Option(secure = true,
      description = "collects information about value analysis states in proof")
  private boolean collectValueAnalysisStateInfo = false;

  private int numProofStates = 0;
  private int numValuesInValueAnalysisStates = 0;

  public ProofStatesInfoCollector(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public void addInfoForStates(final AbstractState[] partialProofStates) {
    numProofStates += partialProofStates.length;

    if (collectValueAnalysisStateInfo) {
      collectValueAnalysisInfo(partialProofStates);
    }
  }

  private void collectValueAnalysisInfo(final AbstractState[] partialProofStates) {
    ValueAnalysisState vState;
    for (AbstractState state : partialProofStates) {
      vState = AbstractStates.extractStateByType(state, ValueAnalysisState.class);
      if (vState != null) {
        numValuesInValueAnalysisStates += vState.getConstantsMapView().size();
      }
    }
  }

  public String getInfoAsString() {
    if (collectValueAnalysisStateInfo) {
      return "Proof state info:\n #states in proof:\t" + numProofStates
          + "\n #values stored in value analysis:\t" + numValuesInValueAnalysisStates;
    } else {
      return "No proof state information collected.";
    }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    pOut.println(getInfoAsString());

  }

  @Override
  public
  @Nullable
  String getName() {
    return null;
  }

}
