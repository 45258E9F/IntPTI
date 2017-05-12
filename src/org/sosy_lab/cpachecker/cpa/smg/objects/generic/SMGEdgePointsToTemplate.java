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


public class SMGEdgePointsToTemplate extends SMGEdgeTemplate {

  public SMGEdgePointsToTemplate(
      SMGObjectTemplate pAbstractObject,
      int pAbstractPointerValue,
      int pOffset) {
    super(pAbstractObject, pAbstractPointerValue, pOffset);
  }

  @Override
  public String toString() {
    return getAbstractValue() + "->" + " O" + getOffset() + "B " + getObjectTemplate().toString();
  }
}