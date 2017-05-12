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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;

/**
 * Checks if a certain variable has a specific value at a specific location marked by a label in the
 * program.
 */
public class SingleValueChecker extends PerElementPropertyChecker {

  private final MemoryLocation varValRep;
  private final Value varValBigInt;
  private final Value varValLong;
  private final String labelLocVarVal;

  public SingleValueChecker(
      final String varWithSingleValue, final String varValue,
      final String labelForLocationWithSingleValue) {
    varValRep = MemoryLocation.valueOf(varWithSingleValue);
    labelLocVarVal = labelForLocationWithSingleValue;
    varValBigInt = new NumericValue(new BigInteger(varValue));
    varValLong = new NumericValue(Long.parseLong(varValue));
  }

  @Override
  public boolean satisfiesProperty(final AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    // check if value correctly specified at location
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CLabelNode && ((CLabelNode) node).getLabel().equals(labelLocVarVal)) {
      Value value =
          AbstractStates.extractStateByType(pElemToCheck, ValueAnalysisState.class)
              .getConstantsMapView()
              .get(varValRep);
      if (value == null || !value.isExplicitlyKnown() ||
          !(value.equals(varValBigInt) || value.equals(varValLong))) {
        return false;
      }
    }
    return true;
  }

}
