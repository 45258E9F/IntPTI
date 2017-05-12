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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;

import java.util.ArrayList;
import java.util.List;


public class SMGPrefixProvider extends GenericPrefixProvider<SMGState> {

  public SMGPrefixProvider(
      LogManager pLogger,
      CFA pCfa, Configuration pConfig, SMGState pInitialState)
      throws InvalidConfigurationException {


    super(
        new SMGStrongestPostOperator(pLogger, pConfig, pCfa),
        pInitialState,
        pLogger,
        pCfa,
        pConfig,
        SMGCPA.class);
  }

  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(ARGPath pPath, SMGState pInitial)
      throws CPAException, InterruptedException {

    List<InfeasiblePrefix> prefixes = super.extractInfeasiblePrefixes(pPath, pInitial);

    // Due to SMGCPA producing infeasible paths without feasible prefixes, that may contain no state
    // after the last edge of the error path (for example invalid read in an assumption edge), check
    // if the prefix is the whole path, and return the path

    List<Integer> wrongPrefixes = new ArrayList<>();

    for (int i = 0; i < prefixes.size(); i++) {
      InfeasiblePrefix prefix = prefixes.get(i);

      if (prefix.getPath().size() == pPath.size()) {
        wrongPrefixes.add(i);
      }
    }

    for (int i : wrongPrefixes) {
      prefixes.remove(i);
    }

    return prefixes;
  }
}
