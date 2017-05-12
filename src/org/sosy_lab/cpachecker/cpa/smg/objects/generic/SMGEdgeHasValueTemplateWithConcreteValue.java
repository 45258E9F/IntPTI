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

public interface SMGEdgeHasValueTemplateWithConcreteValue {

  public SMGObjectTemplate getObjectTemplate();

  public int getValue();

  public int getOffset();

  public CType getType();
}