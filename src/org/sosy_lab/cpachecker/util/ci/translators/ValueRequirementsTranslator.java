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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;


public class ValueRequirementsTranslator
    extends CartesianRequirementsTranslator<ValueAnalysisState> {

  public ValueRequirementsTranslator(final LogManager pLog) {
    super(ValueAnalysisState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final ValueAnalysisState pRequirement) {
    List<String> list = new ArrayList<>(pRequirement.getConstantsMapView().size());
    for (MemoryLocation memLoc : pRequirement.getConstantsMapView().keySet()) {
      list.add(memLoc.getAsSimpleString());
    }
    return list;
  }

  @Override
  protected List<String> getListOfIndependentRequirements(
      final ValueAnalysisState pRequirement,
      final SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (MemoryLocation memLoc : pRequirement.getConstantsMapView().keySet()) {
      Value integerValue = pRequirement.getConstantsMapView().get(memLoc);
      if (!integerValue.isNumericValue() || !(integerValue.asNumericValue()
          .getNumber() instanceof Integer)) {
        logger.log(Level.SEVERE, "The value " + integerValue + " of the MemoryLocation " + memLoc
            + " is not an Integer.");
      } else {
        if (pRequiredVars == null || pRequiredVars.contains(memLoc.getAsSimpleString())) {
          list.add("(= " + getVarWithIndex(memLoc.getAsSimpleString(), pIndices) + " "
              + integerValue.asNumericValue().getNumber() + ")");
        }
      }
    }
    // TODO getRequirement(..) hinzufuegen
    return list;
  }
}
