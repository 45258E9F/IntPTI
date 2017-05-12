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
package org.sosy_lab.cpachecker.util.ci.translators;

import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;


public class PredicateRequirementsTranslator
    extends AbstractRequirementsTranslator<PredicateAbstractState> {

  private final FormulaManagerView fmgr;
  private int counter;

  public PredicateRequirementsTranslator(PredicateCPA cpa) {
    super(PredicateAbstractState.class);
    fmgr = cpa.getSolver().getFormulaManager();
    counter = 0;
  }

  @Override
  protected Pair<List<String>, String> convertToFormula(
      final PredicateAbstractState pRequirement,
      final SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) throws CPAException {

    if (!pRequirement.isAbstractionState()) {
      throw new CPAException("The PredicateAbstractState " + pRequirement
          + " is not an abstractionState. Ensure that property cpa.predicate.blk.alwaysAtExplicitNodes is set to true");
    }

    BooleanFormula formulaBool =
        fmgr.instantiate(pRequirement.getAbstractionFormula().asFormula(), pIndices);

    Pair<String, List<String>> pair = PredicatePersistenceUtils.splitFormula(fmgr, formulaBool);
    List<String> list = new ArrayList<>(pair.getSecond());
    List<String> removeFromList = new ArrayList<>();
    for (String stmt : list) {
      if (!stmt.startsWith("(declare") && !stmt.startsWith("(define")) {
        removeFromList.add(stmt);
      }
    }
    list.removeAll(removeFromList);

    String secReturn;
    String element = pair.getFirst();
    // element =(assert ...)
    element = element.substring(element.indexOf('t') + 1, element.length() - 1);
    secReturn = "(define-fun .defci" + (counter++) + " () Bool " + element + ")";


    return Pair.of(list, secReturn);
  }


}
