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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.base.Function;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.List;

/**
 * This class represents a strategy to get the sequence of block formulas
 * from an ARG path.
 * This class implements the trivial strategy (just get the formulas from the states),
 * but for example {@link BlockFormulaSlicer} implements a more refined strategy.
 * Typically {@link PredicateCPARefinerFactory} automatically creates the desired strategy.
 */
public class BlockFormulaStrategy {

  static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA =
      new Function<PredicateAbstractState, BooleanFormula>() {

        @Override
        public BooleanFormula apply(PredicateAbstractState e) {
          assert e.isAbstractionState();
          return e.getAbstractionFormula().getBlockFormula().getFormula();
        }
      };

  /**
   * Get the block formulas from a path.
   *
   * @param argRoot           The initial element of the analysis (= the root element of the ARG)
   * @param abstractionStates A list of all abstraction elements
   * @return A list of block formulas for this path.
   * @throws CPATransferException If CFA edges cannot be analyzed (should not happen because the
   *                              main analyses analyzed them successfully).
   * @throws InterruptedException On shutdown request.
   */
  List<BooleanFormula> getFormulasForPath(ARGState argRoot, List<ARGState> abstractionStates)
      throws CPATransferException, InterruptedException {
    return from(abstractionStates)
        .transform(toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toList();
  }
}
