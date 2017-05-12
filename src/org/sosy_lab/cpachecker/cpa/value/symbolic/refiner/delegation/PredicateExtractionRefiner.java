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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.delegation;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * {@link Refiner} for {@link ValueAnalysisCPA} using symbolic values and {@link ConstraintsCPA}
 * that extracts a precision for both CPAs from the precision created by {@link PredicateRefiner}.
 */
public abstract class PredicateExtractionRefiner implements Refiner {

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    final ConstraintsCPA constraintsCpa = CPAs.retrieveCPA(pCpa, ConstraintsCPA.class);
    final PredicateCPA predicateCPA = CPAs.retrieveCPA(pCpa, PredicateCPA.class);

    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(
          PredicateExtractionRefiner.class.getSimpleName()
              + " needs a ValueAnalysisCPA");
    }

    if (constraintsCpa == null) {
      throw new InvalidConfigurationException(
          PredicateExtractionRefiner.class.getSimpleName()
              + " needs a ConstraintsCPA");
    }

    if (predicateCPA == null) {
      throw new InvalidConfigurationException(
          PredicateExtractionRefiner.class.getSimpleName()
              + " needs a PredicateCPA");
    }

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();

    RefinementStrategy strategy =
        new SymbolicPrecisionRefinementStrategy(
            config,
            logger,
            predicateCPA.getPredicateManager(),
            predicateCPA.getSolver());

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new PredicateCPARefinerFactory(pCpa).forbidStaticRefinements().create(strategy), pCpa);
  }
}
