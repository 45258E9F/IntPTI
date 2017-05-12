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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Set;

import javax.annotation.Nullable;

public class SMGEdgeHasValueFilter {

  public static SMGEdgeHasValueFilter objectFilter(SMGObject pObject) {
    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();
    filter.filterByObject(pObject);

    return filter;
  }

  private SMGObject object = null;

  private Integer value = null;
  private boolean valueComplement = false;
  private Integer offset = null;
  private CType type = null;

  public SMGEdgeHasValueFilter filterByObject(SMGObject pObject) {
    object = pObject;
    return this;
  }

  public SMGEdgeHasValueFilter filterHavingValue(Integer pValue) {
    value = pValue;
    valueComplement = false;
    return this;
  }

  public SMGEdgeHasValueFilter filterNotHavingValue(Integer pValue) {
    value = pValue;
    valueComplement = true;
    return this;
  }

  public SMGEdgeHasValueFilter filterAtOffset(Integer pOffset) {
    offset = pOffset;
    return this;
  }

  public SMGEdgeHasValueFilter filterByType(CType pType) {
    type = pType;
    return this;
  }

  public boolean holdsFor(SMGEdgeHasValue pEdge) {
    if (object != null && object != pEdge.getObject()) {
      return false;
    }

    if (value != null) {
      if (valueComplement && pEdge.getValue() == value) {
        return false;
      } else if ((!valueComplement) && pEdge.getValue() != value) {
        return false;
      }
    }

    if (offset != null && offset != pEdge.getOffset()) {
      return false;
    }

    if (type != null && !type.getCanonicalType().equals(pEdge.getType().getCanonicalType())) {
      return false;
    }

    return true;
  }

  public Set<SMGEdgeHasValue> filterSet(Set<SMGEdgeHasValue> pEdges) {
    return FluentIterable.from(pEdges).filter(new Predicate<SMGEdgeHasValue>() {
      @Override
      public boolean apply(@Nullable SMGEdgeHasValue pSMGEdgeHasValue) {
        return holdsFor(pSMGEdgeHasValue);
      }
    }).toSet();
  }

  public boolean edgeContainedIn(Set<SMGEdgeHasValue> pEdges) {

    assert value != null;
    assert object != null;
    assert offset != null;
    assert type != null;

    for (SMGEdgeHasValue edge : pEdges) {
      if (holdsFor(edge)) {
        return true;
      }
    }

    return false;
  }

  public Predicate<SMGEdgeHasValue> asPredicate() {
    return new Predicate<SMGEdgeHasValue>() {
      @Override
      public boolean apply(SMGEdgeHasValue pEdge) {
        return SMGEdgeHasValueFilter.this.holdsFor(pEdge);
      }
    };
  }
}