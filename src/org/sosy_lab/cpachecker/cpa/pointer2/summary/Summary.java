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
package org.sosy_lab.cpachecker.cpa.pointer2.summary;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Set;

/**
 * Created by landq on 11/28/16.
 */
public interface Summary<T> {

  /**
   * get the function name where the summary belongs to
   */
  public String getFunctionName();

  /**
   * add changed variable
   */
  public void addChanged(MemoryLocation pMemoryLocation, LocationSet pLocationSet);

  /**
   * get functions called in the summary's corresponding edges
   */
  public Set<CFAEdge> getCalledFunctions();

  /**
   * @return a copy of this object
   */
  public T copyOf();
}
