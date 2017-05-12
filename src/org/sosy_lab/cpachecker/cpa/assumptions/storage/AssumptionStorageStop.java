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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

import java.util.Collection;

/**
 * Stop operator for the assumption storage CPA. Stops if the stop flag is
 * true.
 */
public class AssumptionStorageStop implements StopOperator {

  @Override
  public boolean stop(
      AbstractState pElement,
      Collection<AbstractState> reached,
      Precision precision) {
    AssumptionStorageState element = (AssumptionStorageState) pElement;

    if (element.isStop()) {
      // normally we want to keep this element so that the assumption is not lost
      // but we may return true if the new assumption is implied by the old ones

      for (AbstractState ae : reached) {
        AssumptionStorageState reachedState = (AssumptionStorageState) ae;

        // implication check is costly, so we do a fast syntactical approximation
        if (reachedState.isStopFormulaFalse()
            || reachedState.getStopFormula().equals(element.getStopFormula())) {
          return true;
        }
      }
      return false;

    } else {
      // always true, because we never want to prevent the element from being covered
      return true;
    }
  }
}