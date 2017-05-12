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
package org.sosy_lab.cpachecker.util.automaton;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;


public interface TargetLocationProvider {

  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(CFANode pRootNode);

  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      CFANode pRootNode, Optional<Automaton> pAutomaton);

}