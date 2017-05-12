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
package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class RestrictedProgramDomainAlgorithm implements Algorithm {

  private final Algorithm innerAlgorithm;
  private final CFA cfa;

  public RestrictedProgramDomainAlgorithm(Algorithm pAlgorithm, CFA pCfa) {
    this.innerAlgorithm = pAlgorithm;
    this.cfa = pCfa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
                                                            CPAEnabledAnalysisPropertyViolationException {
    if (cfa.getVarClassification().isPresent()) {
      if (cfa.getVarClassification().get().hasRelevantNonIntAddVars()) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
    }

    return innerAlgorithm.run(pReachedSet);
  }

}
