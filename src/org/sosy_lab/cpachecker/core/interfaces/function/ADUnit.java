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
package org.sosy_lab.cpachecker.core.interfaces.function;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.util.access.AccessPath;

import java.util.List;

/**
 * The unit class for ADCombinator
 *
 * @param <S> the type of abstract domain
 */
public class ADUnit<S> {

  private S value;
  private List<String> identifier;

  private AccessPath parentPath;

  public ADUnit(S pValue, List<String> pIdentifier, AccessPath path) {
    value = pValue;
    identifier = Lists.newLinkedList(pIdentifier);
    parentPath = AccessPath.copyOf(path);
  }

  public S getValue() {
    return value;
  }

  public String getIdentifier() {
    List<String> prefix = AccessPath.toStrList(parentPath);
    prefix.addAll(identifier);
    return Joiner.on('.').join(prefix);
  }

  public AccessPath getParentPath() {
    return parentPath;
  }

  /**
   * A partial identifier does not contain parent access path
   *
   * @return a partial identifier
   */
  public String getPartialIdentifier() {
    return Joiner.on('.').join(identifier);
  }

  @Override
  public String toString() {
    return getIdentifier() + "=" + value.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, identifier, parentPath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !getClass().equals(obj.getClass())) {
      return false;
    }
    // according to type erasure, we know nothing about type parameter
    ADUnit that = (ADUnit) obj;
    return value.equals(that.value) && identifier.equals(that.identifier) && parentPath.equals
        (that.parentPath);
  }

}
