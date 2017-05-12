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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ci.CIUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;


public abstract class CartesianRequirementsTranslator<T extends AbstractState>
    extends AbstractRequirementsTranslator<T> {

  protected final LogManager logger;

  public CartesianRequirementsTranslator(final Class<T> pAbstractStateClass, final LogManager log) {
    super(pAbstractStateClass);
    logger = log;
  }

  private List<String> writeVarDefinition(
      final List<String> vars, final SSAMap ssaMap,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    String def;
    for (String var : vars) {
      if (pRequiredVars == null || pRequiredVars.contains(var)) {
        def = "(declare-fun " + getVarWithIndex(var, ssaMap);
        def += " () Int)";
        list.add(def);
      }
    }
    return list;
  }

  protected abstract List<String> getVarsInRequirements(final T requirement);

  protected List<String> getVarsInRequirements(
      final T requirement,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (String var : getVarsInRequirements(requirement)) {
      if (pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(var);
      }
    }
    return list;
  }

  @Override
  protected Pair<List<String>, String> convertToFormula(
      final T requirement,
      final SSAMap indices,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> firstReturn =
        writeVarDefinition(getVarsInRequirements(requirement), indices, pRequiredVars);

    String secReturn;
    List<String> listOfIndependentRequirements =
        getListOfIndependentRequirements(requirement, indices, pRequiredVars);
    if (listOfIndependentRequirements.isEmpty()) {
      secReturn = "true";
    } else if (listOfIndependentRequirements.size() == 1) {
      secReturn = listOfIndependentRequirements.get(0);
    } else {
      secReturn = computeConjunction(listOfIndependentRequirements);
    }

    secReturn = "(define-fun req () Bool " + secReturn + ")";
    return Pair.of(firstReturn, secReturn);
  }

  private String computeConjunction(final List<String> list) {
    StringBuilder sb = new StringBuilder();
    int BracketCounter = 0;

    if (list.isEmpty()) {
      return "true";
    }

    String last = list.get(list.size() - 1);
    for (String var : list) {
      if (var.equals(last)) {
        sb.append(var);
      } else {
        sb.append("(and ");
        sb.append(var);
        BracketCounter++;
      }
    }

    for (int i = 0; i < BracketCounter; i++) {
      sb.append(")");
    }
    return sb.toString();
  }

  protected abstract List<String> getListOfIndependentRequirements(
      final T requirement,
      final SSAMap indices,
      final @Nullable Collection<String> pRequiredVars);

  public static String getVarWithIndex(final String var, final SSAMap indices) {
    assert (indices != null);
    assert (var != null);

    int index = indices.getIndex(var);

    if (index == -1) {
      return CIUtils.getSMTName(var);
    } else {
      return CIUtils.getSMTName(var + "@" + index);
    }
  }

}