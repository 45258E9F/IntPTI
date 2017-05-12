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
package org.sosy_lab.cpachecker.util.ci.translators;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonState;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonInterval;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;


public class OctagonRequirementsTranslator extends CartesianRequirementsTranslator<OctagonState> {

  public OctagonRequirementsTranslator(Class<OctagonState> pAbstractStateClass, LogManager pLog) {
    super(pAbstractStateClass, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(OctagonState pRequirement) {
    List<String> list = new ArrayList<>();
    for (Entry<MemoryLocation, OctagonInterval> entry : pRequirement.getVariablesWithBounds()
        .entrySet()) {
      list.add(entry.getKey().getAsSimpleString());
    }
    return list;
  }

  @Override
  protected List<String> getVarsInRequirements(
      final OctagonState pRequirement,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (Entry<MemoryLocation, OctagonInterval> entry : pRequirement.getVariablesWithBounds()
        .entrySet()) {
      String var = entry.getKey().getAsSimpleString();
      if (!entry.getValue().isEmpty() || pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(var);
      }
    }
    return list;
  }

  @Override
  protected List<String> getListOfIndependentRequirements(
      final OctagonState pRequirement,
      final SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (Entry<MemoryLocation, OctagonInterval> entry : pRequirement.getVariablesWithBounds()
        .entrySet()) {
      String var = entry.getKey().getAsSimpleString();
      if (!entry.getValue().isEmpty() || pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(getRequirement(getVarWithIndex(var, pIndices), entry.getValue()));
      }
    }
    return list;
  }

  private String getRequirement(final String pVar, final OctagonInterval pVals) {
    if (pVals.getLow() == null) {
      return TranslatorsUtils.getVarLessOrEqualValRequirement(pVar, pVals.getHigh().getValue());
    } else if (pVals.getHigh() == null) {
      return TranslatorsUtils.getVarGreaterOrEqualValRequirement(pVar, pVals.getLow().getValue());
    }
    return TranslatorsUtils
        .getVarInBoundsRequirement(pVar, pVals.getLow().getValue(), pVals.getHigh().getValue());
  }
}
