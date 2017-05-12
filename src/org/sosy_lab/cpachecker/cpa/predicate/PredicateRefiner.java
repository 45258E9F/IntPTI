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
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class PredicateRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

  public static ARGBasedRefiner create0(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          PredicateRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    RefinementStrategy strategy = new PredicateAbstractionRefinementStrategy(
        predicateCpa.getConfiguration(),
        predicateCpa.getLogger(),
        predicateCpa.getPredicateManager(),
        predicateCpa.getSolver());

    return new PredicateCPARefinerFactory(pCpa).create(strategy);
  }
}
