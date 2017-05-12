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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class SingleSignChecker extends PerElementPropertyChecker {

  private final String varName;
  private final SIGN value;
  private final String label;

  public SingleSignChecker(String pName, String pValue, String pLabel) {
    varName = pName;
    value = SIGN.valueOf(pValue);
    label = pLabel;
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CLabelNode && ((CLabelNode) node).getLabel().equals(label)) {
      SignState state = AbstractStates.extractStateByType(pElemToCheck, SignState.class);
      if (state != null) {
        if (state.getSignForVariable(varName) == value) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

}
