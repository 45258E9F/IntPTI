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
package org.sosy_lab.cpachecker.cpa.edgeexclusion;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/**
 * Instances of this class are precisions for the edge exclusion CPA. They
 * control which edges are excluded.
 *
 * The instances of this class are immutable.
 */
public class EdgeExclusionPrecision implements Precision {

  /**
   * The empty precision does not exclude any edges.
   */
  private static final EdgeExclusionPrecision EMPTY =
      new EdgeExclusionPrecision(ImmutableSet.<CFAEdge>of());

  /**
   * The excluded edges.
   */
  private final ImmutableSet<CFAEdge> excludedEdges;

  /**
   * Gets the empty precision, which does not exclude any edges.
   *
   * @return the empty precision, which does not exclude any edges.
   */
  public static EdgeExclusionPrecision getEmptyPrecision() {
    return EMPTY;
  }

  /**
   * Creates a new edge exclusion precision for excluding the given edges.
   *
   * @param pExcludedEdges the edges to exclude.
   */
  private EdgeExclusionPrecision(ImmutableSet<CFAEdge> pExcludedEdges) {
    this.excludedEdges = pExcludedEdges;
  }

  /**
   * Returns a precision that excludes all edges excluded by this precision
   * plus all given edges.
   *
   * @param pAdditionalExcludedEdges the additional edges to exclude.
   * @return a precision that excludes all edges excluded by this precision plus all given edges.
   */
  public EdgeExclusionPrecision excludeMoreEdges(Iterable<CFAEdge> pAdditionalExcludedEdges) {
    if (pAdditionalExcludedEdges instanceof Collection) {
      return excludeMoreEdges((Collection<CFAEdge>) pAdditionalExcludedEdges);
    }
    return excludeMoreEdges(FluentIterable.from(pAdditionalExcludedEdges).toSet());
  }

  /**
   * Returns a precision that excludes all edges excluded by this precision
   * plus all given edges.
   *
   * @param pAdditionalExcludedEdges the additional edges to exclude.
   * @return a precision that excludes all edges excluded by this precision plus all given edges.
   */
  public EdgeExclusionPrecision excludeMoreEdges(Collection<CFAEdge> pAdditionalExcludedEdges) {
    if (excludedEdges.containsAll(pAdditionalExcludedEdges)) {
      return this;
    }
    ImmutableSet.Builder<CFAEdge> setBuilder = ImmutableSet.<CFAEdge>builder();
    setBuilder.addAll(excludedEdges);
    Queue<CFAEdge> waitlist = new ArrayDeque<>(pAdditionalExcludedEdges);
    while (!waitlist.isEmpty()) {
      CFAEdge current = waitlist.poll();
      if (current instanceof MultiEdge) {
        for (CFAEdge edge : (MultiEdge) current) {
          waitlist.offer(edge);
        }
      }
      setBuilder.add(current);
    }
    return new EdgeExclusionPrecision(setBuilder.build());
  }

  /**
   * Checks if the given edge is excluded.
   *
   * @param pEdge the edge to check.
   * @return {@code true} if the edge is excluded, {@code false} otherwise.
   */
  public boolean isExcluded(CFAEdge pEdge) {
    if (excludedEdges.contains(pEdge)) {
      return true;
    }
    if (pEdge instanceof MultiEdge) {
      for (CFAEdge edge : (MultiEdge) pEdge) {
        if (isExcluded(edge)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return excludedEdges.isEmpty() ? "no precision" : excludedEdges.toString();
  }

}
