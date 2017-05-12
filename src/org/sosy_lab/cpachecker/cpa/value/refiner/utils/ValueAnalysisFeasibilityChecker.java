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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import com.google.common.base.Optional;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.GenericFeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

import java.util.ArrayList;
import java.util.List;

public class ValueAnalysisFeasibilityChecker
    extends GenericFeasibilityChecker<ValueAnalysisState> {

  private final StrongestPostOperator<ValueAnalysisState> strongestPostOp;
  private final VariableTrackingPrecision precision;
  private final MachineModel machineModel;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa    the cfa in use
   */
  public ValueAnalysisFeasibilityChecker(
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOp,
      final LogManager pLogger,
      final CFA pCfa,
      final Configuration config
  ) throws InvalidConfigurationException {

    super(
        pStrongestPostOp,
        new ValueAnalysisState(pCfa.getMachineModel()),
        ValueAnalysisCPA.class,
        pLogger,
        config,
        pCfa);

    strongestPostOp = pStrongestPostOp;
    precision = VariableTrackingPrecision
        .createStaticPrecision(config, pCfa.getVarClassification(), ValueAnalysisCPA.class);
    machineModel = pCfa.getMachineModel();
  }

  public List<Pair<ValueAnalysisState, CFAEdge>> evaluate(final ARGPath path)
      throws CPAException, InterruptedException {

    try {
      List<Pair<ValueAnalysisState, CFAEdge>> reevaluatedPath = new ArrayList<>();
      ValueAnalysisState next = new ValueAnalysisState(machineModel);

      PathIterator iterator = path.pathIterator();
      while (iterator.hasNext()) {
        Optional<ValueAnalysisState> successor = strongestPostOp.getStrongestPost(
            next,
            precision,
            iterator.getOutgoingEdge());

        if (!successor.isPresent()) {
          return reevaluatedPath;
        }

        // extract singleton successor state
        next = successor.get();

        reevaluatedPath.add(Pair.of(next, iterator.getOutgoingEdge()));

        iterator.advance();
      }

      return reevaluatedPath;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(),
          e);
    }
  }
}
