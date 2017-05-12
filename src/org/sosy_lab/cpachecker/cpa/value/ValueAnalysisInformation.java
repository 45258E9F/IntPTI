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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collections;
import java.util.Map;

/**
 * Information about value assignments needed for symbolic interpolation.
 */
public class ValueAnalysisInformation {

  public static final ValueAnalysisInformation EMPTY = new ValueAnalysisInformation();

  private final Map<MemoryLocation, Value> assignments;
  private final Map<MemoryLocation, Type> locationTypes;

  protected ValueAnalysisInformation(
      final Map<MemoryLocation, Value> pAssignments,
      final Map<MemoryLocation, Type> pLocationTypes
  ) {
    assignments = pAssignments;
    locationTypes = pLocationTypes;
  }

  private ValueAnalysisInformation() {
    assignments = Collections.emptyMap();
    locationTypes = Collections.emptyMap();
  }

  public Map<MemoryLocation, Value> getAssignments() {
    return assignments;
  }

  public Map<MemoryLocation, Type> getLocationTypes() {
    return locationTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ValueAnalysisInformation that = (ValueAnalysisInformation) o;

    if (!assignments.equals(that.assignments)) {
      return false;
    }

    if (!locationTypes.equals(that.locationTypes)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = assignments.hashCode();
    result = 31 * result + locationTypes.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ValueInformation[" + assignments.toString() + "]";
  }
}
