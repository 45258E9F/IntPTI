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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.refinement.DelegatingARGBasedRefiner;

/**
 * This class allows to create a delegating BAM-refiner
 * for a combination of value analysis and predicate analysis (in this order!).
 */
public abstract class ValueAnalysisDelegatingBAMRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName()
          + " could not find the ValueAnalysisCPA");
    }

    ValueAnalysisCPA valueCpa = ((WrapperCPA) cpa).retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueCpa == null) {
      throw new InvalidConfigurationException(
          ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    PredicateCPA predicateCpa = ((WrapperCPA) cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    LogManager logger = valueCpa.getLogger();

    // first value analysis refiner, then predicate analysis refiner
    return BAMBasedRefiner.forARGBasedRefiner(
        new DelegatingARGBasedRefiner(
            logger,
            ValueAnalysisRefiner.create(cpa).asARGBasedRefiner(),
            BAMPredicateRefiner.create0(cpa)),
        cpa);
  }
}
