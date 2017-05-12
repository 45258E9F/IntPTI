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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Assignment for {@link SymbolicIdentifier}s.
 */
public class IdentifierAssignment extends ForwardingMap<SymbolicIdentifier, Value> {
  private static final String ERROR_MSG_ASSIGNMENT_REMOVAL =
      "Definite assignments can't be removed!";
  private Map<SymbolicIdentifier, Value> assignment = new HashMap<>();

  public IdentifierAssignment() {
    super();
  }

  public IdentifierAssignment(IdentifierAssignment pAssignment) {
    assignment = Maps.newHashMap(pAssignment);
  }

  @Override
  public Value put(SymbolicIdentifier pIdentifier, Value pValue) {
    assert !pValue.isUnknown();

    return super.put(pIdentifier, pValue);
  }

  @Override
  public Value remove(Object pKey) {
    throw new UnsupportedOperationException(ERROR_MSG_ASSIGNMENT_REMOVAL);
  }

  @Override
  public Value standardRemove(Object pKey) {
    throw new UnsupportedOperationException(ERROR_MSG_ASSIGNMENT_REMOVAL);
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(ERROR_MSG_ASSIGNMENT_REMOVAL);
  }

  @Override
  protected Map<SymbolicIdentifier, Value> delegate() {
    return assignment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IdentifierAssignment that = (IdentifierAssignment) o;

    return assignment.equals(that.assignment);
  }

  @Override
  public int hashCode() {
    return assignment.hashCode();
  }

}
