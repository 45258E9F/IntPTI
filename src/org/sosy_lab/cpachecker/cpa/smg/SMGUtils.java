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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgeHasValueTemplateWithConcreteValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGEdgePointsToTemplate;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGObjectTemplate;

import java.util.Set;

/**
 * This class contains smg utilities, for example filters.
 */
public final class SMGUtils {


  public static class FilterFieldsOfValue
      implements Predicate<SMGEdgeHasValueTemplate> {

    private final int value;

    public FilterFieldsOfValue(int pValue) {
      value = pValue;
    }

    @Override
    public boolean apply(SMGEdgeHasValueTemplate pEdge) {
      return value == pEdge.getAbstractValue();
    }
  }

  private SMGUtils() {
  }

  public static Set<SMGEdgeHasValue> getFieldsOfObject(SMGObject pSmgObject, SMG pInputSMG) {

    SMGEdgeHasValueFilter edgeFilter = SMGEdgeHasValueFilter.objectFilter(pSmgObject);
    return pInputSMG.getHVEdges(edgeFilter);
  }

  public static Set<SMGEdgePointsTo> getPointerToThisObject(SMGObject pSmgObject, SMG pInputSMG) {
    Set<SMGEdgePointsTo> result = FluentIterable.from(pInputSMG.getPTEdges().values())
        .filter(new FilterTargetObject(pSmgObject)).toSet();
    return result;
  }

  public static Set<SMGEdgeHasValue> getFieldsofThisValue(int value, SMG pInputSMG) {
    SMGEdgeHasValueFilter valueFilter = new SMGEdgeHasValueFilter();
    valueFilter.filterHavingValue(value);
    return pInputSMG.getHVEdges(valueFilter);
  }

  public static class FilterTargetTemplate implements Predicate<SMGEdgePointsToTemplate> {

    private final SMGObjectTemplate objectTemplate;

    public FilterTargetTemplate(SMGObjectTemplate pObjectTemplate) {
      objectTemplate = pObjectTemplate;
    }

    @Override
    public boolean apply(SMGEdgePointsToTemplate ptEdge) {
      return ptEdge.getObjectTemplate() == objectTemplate;
    }
  }

  public static class FilterTemplateObjectFieldsWithConcreteValue
      implements Predicate<SMGEdgeHasValueTemplateWithConcreteValue> {

    private final SMGObjectTemplate objectTemplate;

    public FilterTemplateObjectFieldsWithConcreteValue(SMGObjectTemplate pObjectTemplate) {
      objectTemplate = pObjectTemplate;
    }

    @Override
    public boolean apply(SMGEdgeHasValueTemplateWithConcreteValue ptEdge) {
      return ptEdge.getObjectTemplate() == objectTemplate;
    }
  }

  public static class FilterTargetObject implements Predicate<SMGEdgePointsTo> {

    private final SMGObject object;

    public FilterTargetObject(SMGObject pObject) {
      object = pObject;
    }

    @Override
    public boolean apply(SMGEdgePointsTo ptEdge) {
      return ptEdge.getObject() == object;
    }
  }
}