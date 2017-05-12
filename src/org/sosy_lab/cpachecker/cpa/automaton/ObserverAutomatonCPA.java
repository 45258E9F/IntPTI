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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;

public class ObserverAutomatonCPA extends ControlAutomatonCPA {

  private ObserverAutomatonCPA(
      @OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig,
      LogManager pLogger,
      CFA cfa)
      throws InvalidConfigurationException {
    super(pAutomaton, pConfig, pLogger, cfa);
    super.getAutomaton().assertObserverAutomaton();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ObserverAutomatonCPA.class);
  }

}
