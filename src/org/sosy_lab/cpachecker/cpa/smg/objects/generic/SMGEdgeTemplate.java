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


public class SMGEdgeTemplate {

  private final SMGObjectTemplate abstractObject;
  private final int abstractValue;
  private final int offset;

  public SMGEdgeTemplate(SMGObjectTemplate pAbstractObject, int pAbstractValue, int pOffset) {
    abstractObject = pAbstractObject;
    abstractValue = pAbstractValue;
    offset = pOffset;
  }

  public SMGObjectTemplate getObjectTemplate() {
    return abstractObject;
  }

  public int getAbstractValue() {
    return abstractValue;
  }

  public int getOffset() {
    return offset;
  }
}