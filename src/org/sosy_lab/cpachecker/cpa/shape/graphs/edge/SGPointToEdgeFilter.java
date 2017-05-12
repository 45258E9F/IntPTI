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
package org.sosy_lab.cpachecker.cpa.shape.graphs.edge;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

import java.util.Set;

public class SGPointToEdgeFilter {

  private SGObject object = null;
  private Integer offset = null;

  public static SGPointToEdgeFilter objectFilter(SGObject pObject) {
    SGPointToEdgeFilter filter = new SGPointToEdgeFilter();
    filter = filter.filterByObject(pObject);
    return filter;
  }

  public SGPointToEdgeFilter filterByObject(SGObject pObject) {
    object = pObject;
    return this;
  }

  public SGPointToEdgeFilter filterByOffset(Integer pOffset) {
    offset = pOffset;
    return this;
  }

  public boolean holdsFor(SGPointToEdge pEdge) {
    if (object != null && object != pEdge.getObject()) {
      return false;
    }
    if (offset != null && !offset.equals(pEdge.getOffset())) {
      return false;
    }
    return true;
  }

  public Set<SGPointToEdge> filterSet(Set<SGPointToEdge> pEdges) {
    return FluentIterable.from(pEdges).filter(new Predicate<SGPointToEdge>() {
      @Override
      public boolean apply(SGPointToEdge pSGPointToEdge) {
        return holdsFor(pSGPointToEdge);
      }
    }).toSet();
  }

}
