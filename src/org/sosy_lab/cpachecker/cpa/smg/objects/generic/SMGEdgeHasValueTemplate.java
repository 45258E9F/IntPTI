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
package org.sosy_lab.cpachecker.cpa.smg.objects.generic;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class SMGEdgeHasValueTemplate extends SMGEdgeTemplate
    implements SMGEdgeHasValueTemplateWithConcreteValue {

  private final CType type;

  public SMGEdgeHasValueTemplate(
      SMGObjectTemplate pAbstractObject,
      int pAbstractValue,
      int pOffset,
      CType pType) {
    super(pAbstractObject, pAbstractValue, pOffset);
    type = pType;
  }

  @Override
  public CType getType() {
    return type;
  }

  @Override
  public int getValue() {
    return getAbstractValue();
  }

  @Override
  public String toString() {
    return getObjectTemplate().toString() + " O" + getOffset() + "B->" + getValue();
  }
}