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
package org.sosy_lab.cpachecker.cpa.smg.objects.generic;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GenericAbstraction extends SMGObject implements SMGAbstractObject {

  /**
   * These maps contains as keys abstract pointers and as values concrete pointers.
   * For every abstract pointer that exist in the abstraction and points to/from
   * an abstract Objects that represents concrete regions in the smg, map to a concrete pointer
   * in the smg, that points to/from the represented region.
   */
  private final Map<Integer, Integer> abstractToConcretePointerMap;

  /**
   * This map contains as keys abstract pointers and as values a list of
   * materialisation steps. The abstract pointers represent concrete pointers
   * in a smg, that point to a concrete region that has yet to be materialized.
   */
  private final Map<Integer, List<MaterlisationStep>> materlisationStepMap;

  protected GenericAbstraction(
      int pSize, String pLabel,
      Map<Integer, List<MaterlisationStep>> pMaterlisationSteps,
      Map<Integer, Integer> pAbstractToConcretePointerMap) {
    super(pSize, pLabel);
    abstractToConcretePointerMap = ImmutableMap.copyOf(pAbstractToConcretePointerMap);
    materlisationStepMap = ImmutableMap.copyOf(pMaterlisationSteps);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return false;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return false;
  }

  public static GenericAbstraction valueOf(
      Map<Integer, List<MaterlisationStep>> pMaterlisationSteps,
      Map<Integer, Integer> pAbstractToConcretePointerMap) {
    return new GenericAbstraction(100, "generic abtraction ID " + SMGValueFactory.getNewValue(),
        pMaterlisationSteps, pAbstractToConcretePointerMap);
  }

  public List<SMG> materialize(SMG pSMG, int pointer) {
    List<MaterlisationStep> steps = getSteps(pointer);

    List<SMG> result = new ArrayList<>(steps.size());

    for (MaterlisationStep step : steps) {
      result.add(step.materialize(pSMG, abstractToConcretePointerMap));
    }

    return result;
  }

  private List<MaterlisationStep> getSteps(int pPointer) {

    for (Entry<Integer, Integer> entry : abstractToConcretePointerMap.entrySet()) {
      if (entry.getValue() == pPointer) {
        return materlisationStepMap.get(entry.getKey());
      }
    }
    throw new AssertionError();
  }

  public Map<Integer, List<MaterlisationStep>> getMaterlisationStepMap() {
    return materlisationStepMap;
  }

  public Map<Integer, Integer> getAbstractToConcretePointerMap() {
    return abstractToConcretePointerMap;
  }

  @Override
  public String toString() {
    return "Generic Abstraction:\n"
        + "pointersToThisAbstraction " + abstractToConcretePointerMap.toString() + "\n"
        + "pointersToThisAbstraction " + abstractToConcretePointerMap.toString() + "\n"
        + "materlisationSteps " + materlisationStepMap.toString();
  }

  public GenericAbstractionCandidateTemplate createCandidateTemplate() {
    return GenericAbstractionCandidateTemplate.valueOf(materlisationStepMap);
  }

  @Override
  public SMGObject copy() {
    return new GenericAbstraction(getSize(), getLabel(), materlisationStepMap,
        abstractToConcretePointerMap);
  }
}