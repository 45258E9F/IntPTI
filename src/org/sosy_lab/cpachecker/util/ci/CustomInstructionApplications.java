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
package org.sosy_lab.cpachecker.util.ci;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Map;


public class CustomInstructionApplications {

  private final Map<CFANode, AppliedCustomInstruction> cis;
  private final CustomInstruction ci;

  /**
   * Constructor of CustomInstructionApplications
   *
   * @param pCis ImmutableMap
   */
  public CustomInstructionApplications(
      final Map<CFANode, AppliedCustomInstruction> pCis,
      final CustomInstruction pCi) {
    cis = pCis;
    ci = pCi;
  }

  /**
   * Checks if the ImmutableMap cis contains the given CFANode
   * (after it is extracted out of the AbstractState)
   *
   * @param pState AbstractState
   * @return true if cis contains the given node
   * @throws CPAException if the given node can't be extracted
   */
  public boolean isStartState(final AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("The state " + pState + " has to contain a location state!");
    }
    return cis.containsKey(locState);
  }

  /**
   * Checks if the given AbstractState pIsEnd is an endNode of the given AbsractState pCISart
   *
   * @param pIsEnd   AbstractState
   * @param pCIStart AbstractState
   * @return true if pIsEnd is an endNode of pCISart
   */
  public boolean isEndState(final AbstractState pIsEnd, final AbstractState pCIStart)
      throws CPAException {
    return isEndState(pIsEnd, AbstractStates.extractLocation(pCIStart));
  }

  /**
   * Checks if the given AbstractState pIsEnd is an endNode of the given CFANode pCISart
   *
   * @param pIsEnd   AbstractState
   * @param pCIStart CFANode
   * @return true if pIsEnd is an endNode of pCISart
   */
  public boolean isEndState(final AbstractState pIsEnd, final CFANode pCIStart)
      throws CPAException {
    assert (cis.containsKey(pCIStart));
    return cis.get(pCIStart).isEndState(pIsEnd);
  }

  public AppliedCustomInstruction getAppliedCustomInstructionFor(final ARGState pState)
      throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);

    if (locState == null) {
      throw new CPAException("The state " + pState + " has to contain a location state!");
    }

    if (!isStartState(pState)) {
      throw new CPAException("The state does not represent start of known custom instruction");
    }

    return cis.get(locState);
  }

  public CustomInstruction getCustomInstruction() {
    return ci;
  }

  public Map<CFANode, AppliedCustomInstruction> getMapping() {
    return cis;
  }

}