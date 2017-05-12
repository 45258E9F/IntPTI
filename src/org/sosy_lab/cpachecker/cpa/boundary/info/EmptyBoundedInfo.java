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
package org.sosy_lab.cpachecker.cpa.boundary.info;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import java.util.Collection;

public class EmptyBoundedInfo implements BoundedInfo<Void> {

  public static final EmptyBoundedInfo EMPTY = new EmptyBoundedInfo();

  @Override
  public Void getBoundedObject() {
    return null;
  }

  @Override
  public CFAEdge getEntry() {
    return null;
  }

  @Override
  public Collection<CFAEdge> getExit() {
    return null;
  }
}
