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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class ImpactRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          ImpactRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    RefinementStrategy strategy = new ImpactRefinementStrategy(
        predicateCpa.getConfiguration(),
        predicateCpa.getSolver(),
        predicateCpa.getPredicateManager());

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new PredicateCPARefinerFactory(pCpa).create(strategy), pCpa);
  }
}