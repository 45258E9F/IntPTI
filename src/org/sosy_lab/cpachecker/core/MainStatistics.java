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
package org.sosy_lab.cpachecker.core;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.interfaces.AlgorithmIterationListener;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseResult;

import java.util.Collection;

public interface MainStatistics extends Statistics, AlgorithmIterationListener, CPAPhaseResult {

  boolean transferCFAStatistics(MainStatistics pStatistics);

  void setCFACreator(CFACreator pCFACreator);

  void setCFA(CFA pCFA);

  CFA getCFA();

  Statistics getCFAStatistics();

  Collection<Statistics> getSubStatistics();

  void startCPACreationTimer();

  void stopCPACreationTimer();

  void startAnalysisTimer();

  void stopAnalysisTimer();

}
