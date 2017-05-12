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
package org.sosy_lab.cpachecker.core.summary.instance.pointer;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Map;
import java.util.Set;

public class PointerResult {
  private Map<MemoryLocation, Set<MemoryLocation>> pointTo;

  public PointerResult(Map<MemoryLocation, Set<MemoryLocation>> pPointTo) {
    pointTo = pPointTo;
  }

  public Map<MemoryLocation, Set<MemoryLocation>> getPointTo() {
    return pointTo;
  }
}
