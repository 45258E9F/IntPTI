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
package org.sosy_lab.cpachecker.cpa.boundary;

import com.google.common.collect.Iterables;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.boundary.info.CallStackInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.LoopStackInfo;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collection;

public class BoundaryCPA extends AbstractCPA {

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BoundaryCPA.class);
  }

  public BoundaryCPA(Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    super("sep", "sep", new BoundaryTransferRelation(pConfig, pLogger,
        pCFA.getLoopStructure().orNull()));
    cfa = pCFA;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) {
    LoopStackInfo loopStack = LoopStackInfo.of();
    CallStackInfo callStack = null;
    if (cfa.getLoopStructure().isPresent()) {
      LoopStructure loopStructure = cfa.getLoopStructure().get();
      Collection<Loop> artificialLoops = loopStructure.getLoopsForFunction(
          CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
      if (!artificialLoops.isEmpty()) {
        Loop loop = Iterables.getOnlyElement(artificialLoops);
        if (loop.getLoopNodes().contains(node)) {
          callStack = CallStackInfo.of(null, CFASingleLoopTransformation.
              ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME, node);
        }
      }
    }
    if (callStack == null) {
      callStack = CallStackInfo.of(null, node.getFunctionName(), node);
    }
    return new BoundaryState(callStack, loopStack);
  }
}
