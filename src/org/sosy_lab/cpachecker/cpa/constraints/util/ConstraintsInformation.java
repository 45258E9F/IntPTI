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
package org.sosy_lab.cpachecker.cpa.constraints.util;

import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;

import java.util.Collections;
import java.util.Set;

/**
 * Information about {@link Constraint}s needed for symbolic interpolation.
 */
public class ConstraintsInformation {

  public static final ConstraintsInformation EMPTY =
      new ConstraintsInformation(Collections.<Constraint>emptySet(),
          new IdentifierAssignment());

  private final Set<Constraint> constraints;
  private final IdentifierAssignment definiteValueAssignments;

  public ConstraintsInformation(
      final Set<Constraint> pConstraints,
      final IdentifierAssignment pDefiniteSymIdAssignments
  ) {
    constraints = pConstraints;
    definiteValueAssignments = pDefiniteSymIdAssignments;
  }

  public Set<Constraint> getConstraints() {
    return constraints;
  }

  public IdentifierAssignment getAssignments() {
    return definiteValueAssignments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstraintsInformation that = (ConstraintsInformation) o;

    return constraints.size() == that.constraints.size()
        && constraints.containsAll(that.constraints);

  }

  @Override
  public int hashCode() {
    return constraints.hashCode();
  }

  @Override
  public String toString() {
    return "ConstraintsInformation[" +
        "constraints=" + constraints +
        ']';
  }
}
