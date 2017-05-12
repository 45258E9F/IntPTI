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
package org.sosy_lab.cpachecker.core.interfaces.function;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.FieldAccessSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * The combinator of abstract domain elements. This compound structure is used to denote
 * structures and arrays consisting of abstract domain elements.
 */
public class ADCombinator<S> {

  /**
   * Partial identifier -> Abstract domain unit
   */
  private final Map<String, ADUnit<S>> values;

  private final
  @Nullable
  AccessPath parentPath;

  public ADCombinator(AccessPath pPath) {
    parentPath = pPath;
    values = Maps.newHashMap();
  }

  public void insertValue(ADUnit<S> newValue) {
    if (newValue.getParentPath().equals(parentPath)) {
      // only consistent values are appended in current combinator
      String prefix = newValue.getPartialIdentifier();
      values.put(prefix, newValue);
    }
  }

  public
  @Nullable
  S queryValueByPathSegments(List<PathSegment> segments) {
    String key = exportPathSegments(segments);
    ADUnit<S> value = values.get(key);
    if (value != null) {
      return value.getValue();
    } else {
      return null;
    }
  }

  public
  @Nullable
  S queryValueByIndex(Long index) {
    List<PathSegment> segList = new LinkedList<>();
    segList.add(new ArrayConstIndexSegment(index));
    return queryValueByPathSegments(segList);
  }

  public
  @Nullable
  S queryValueByFieldName(String name) {
    List<PathSegment> segList = new LinkedList<>();
    segList.add(new FieldAccessSegment(name));
    return queryValueByPathSegments(segList);
  }

  private String exportPathSegments(List<PathSegment> segments) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (PathSegment segment : segments) {
      if (!first) {
        builder.append('.');
      } else {
        first = false;
      }
      builder.append(segment.getName());
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return "[" + (parentPath == null ? "EMPTY" : parentPath.toString()) + "] : " + values.values()
        .toString();
  }
}
