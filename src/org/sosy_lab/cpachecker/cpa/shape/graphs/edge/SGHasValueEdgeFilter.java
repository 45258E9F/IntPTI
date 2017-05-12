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
package org.sosy_lab.cpachecker.cpa.shape.graphs.edge;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

import java.util.Set;

public class SGHasValueEdgeFilter {

  public static SGHasValueEdgeFilter objectFilter(SGObject pObject) {
    SGHasValueEdgeFilter filter = new SGHasValueEdgeFilter();
    filter = filter.filterByObject(pObject);
    return filter;
  }

  private SGObject object = null;
  private Long value = null;
  private boolean valueComplement = false;
  private Integer offset = null;
  private CType type = null;

  /* ****************************************** */
  /* the filter for the specified memory region */
  /* ****************************************** */

  private Integer lower = null;
  private Integer upper = null;

  public SGHasValueEdgeFilter filterByObject(SGObject pObject) {
    object = pObject;
    return this;
  }

  public SGHasValueEdgeFilter filterHavingValue(Long pValue) {
    value = pValue;
    valueComplement = false;
    return this;
  }

  public SGHasValueEdgeFilter filterNotHavingValue(Long pValue) {
    value = pValue;
    valueComplement = true;
    return this;
  }

  public SGHasValueEdgeFilter filterAtOffset(Integer pOffset) {
    offset = pOffset;
    return this;
  }

  public SGHasValueEdgeFilter filterByType(CType pType) {
    type = pType;
    return this;
  }

  /**
   * A range filter filters has-value edges the start position of which is within lower and upper
   * bounds while the end position is allowed to exceed the upper bound.
   * The bounds are inclusive.
   */
  public SGHasValueEdgeFilter filterByRange(Integer pLower, Integer pUpper) {
    lower = pLower;
    upper = pUpper;
    return this;
  }

  /**
   * Test if the specified has-value edge can pass current filter
   *
   * @param pEdge a has-value edge
   * @return whether this edge passes the filter test
   */
  public boolean holdsFor(SGHasValueEdge pEdge) {
    if (object != null && object != pEdge.getObject()) {
      return false;
    }
    if (value != null) {
      if (valueComplement && pEdge.getValue() == value) {
        return false;
      } else if (!valueComplement && pEdge.getValue() != value) {
        return false;
      }
    }
    if (offset != null && offset != pEdge.getOffset()) {
      return false;
    }
    if (type != null && !type.getCanonicalType().equals(pEdge.getType().getCanonicalType())) {
      return false;
    }
    if (lower != null && pEdge.getOffset() < lower) {
      return false;
    }
    if (upper != null && pEdge.getOffset() > upper) {
      return false;
    }
    return true;
  }

  public Set<SGHasValueEdge> filterSet(Set<SGHasValueEdge> pEdges) {
    return FluentIterable.from(pEdges).filter(new Predicate<SGHasValueEdge>() {
      @Override
      public boolean apply(SGHasValueEdge pSGHasValueEdge) {
        return holdsFor(pSGHasValueEdge);
      }
    }).toSet();
  }

  public boolean containsEligibleEdge(Set<SGHasValueEdge> pEdges) {
    assert (value != null);
    assert (object != null);
    assert (offset != null);
    assert (type != null);

    return !filterSet(pEdges).isEmpty();
  }

}
