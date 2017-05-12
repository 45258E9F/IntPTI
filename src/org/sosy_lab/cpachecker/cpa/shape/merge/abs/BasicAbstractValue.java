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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

import com.google.common.base.Objects;

public class BasicAbstractValue implements AbstractValue {

  private Long value;

  public BasicAbstractValue(Long pValue) {
    value = pValue;
  }

  @Override
  public Long getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof BasicAbstractValue)) {
      return false;
    }
    BasicAbstractValue that = (BasicAbstractValue) obj;
    return Objects.equal(value, that.value);
  }
}
