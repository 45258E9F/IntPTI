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
package org.sosy_lab.cpachecker.cpa.shape.merge.util;

import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class PointerInfo {

  private Long value;
  private Integer offset;

  /**
   * Constructor.
   *
   * @param pOffset offset of point-to edge
   * @param pValue  value of point-to edge
   */
  private PointerInfo(int pOffset, long pValue) {
    value = pValue;
    offset = pOffset;
  }

  public static PointerInfo of(int pOffset, long pValue) {
    return new PointerInfo(pOffset, pValue);
  }

  public Integer getOffset() {
    return offset;
  }

  public Long getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, offset);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof PointerInfo)) {
      return false;
    }
    PointerInfo that = (PointerInfo) obj;
    return this.value.equals(that.value) && this.offset.equals(that.offset);
  }

  /**
   * Re-organize a set of (offset, value) pairs into a mapping from offset to value.
   */
  public static Map<Integer, Long> toMap(Collection<PointerInfo> pairs) {
    // we require total ordering on offset, thus a tree map is used here
    Map<Integer, Long> map = new TreeMap<>();
    for (PointerInfo pair : pairs) {
      map.put(pair.getOffset(), pair.getValue());
    }
    return Collections.unmodifiableMap(map);
  }
}
