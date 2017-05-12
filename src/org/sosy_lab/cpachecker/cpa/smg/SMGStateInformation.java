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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

public class SMGStateInformation {

  private static final SMGStateInformation EMPTY = new SMGStateInformation();

  private final Set<SMGEdgeHasValue> hvEdges;
  private final Map<Integer, SMGEdgePointsTo> ptEdges;

  private SMGStateInformation() {
    hvEdges = ImmutableSet.of();
    ptEdges = ImmutableMap.of();
  }

  private SMGStateInformation(
      Set<SMGEdgeHasValue> pHvEdges,
      Map<Integer, SMGEdgePointsTo> pPtEdges) {
    hvEdges = ImmutableSet.copyOf(pHvEdges);
    ptEdges = ImmutableMap.copyOf(pPtEdges);
  }

  public static SMGStateInformation of() {
    return EMPTY;
  }

  public static SMGStateInformation of(
      Set<SMGEdgeHasValue> pHvEdges,
      Map<Integer, SMGEdgePointsTo> pPtEdges) {
    return new SMGStateInformation(pHvEdges, pPtEdges);
  }

  public Map<Integer, SMGEdgePointsTo> getPtEdges() {
    return ptEdges;
  }

  public Set<SMGEdgeHasValue> getHvEdges() {
    return hvEdges;
  }

  @Override
  public String toString() {
    return hvEdges.toString() + "\n" + ptEdges.toString();
  }
}