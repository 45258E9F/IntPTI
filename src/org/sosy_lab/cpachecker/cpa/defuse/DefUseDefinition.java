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
package org.sosy_lab.cpachecker.cpa.defuse;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Objects;

public class DefUseDefinition implements AbstractState {
  private final String variableName;
  private final CFAEdge assigningEdge;

  public DefUseDefinition(String variableName, CFAEdge assigningEdge) {
    this.variableName = Preconditions.checkNotNull(variableName);
    this.assigningEdge = assigningEdge;
  }

  public String getVariableName() {
    return variableName;
  }

  public CFAEdge getAssigningEdge() {
    return assigningEdge;
  }

  @Override
  public int hashCode() {
    return variableName.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DefUseDefinition)) {
      return false;
    }

    DefUseDefinition otherDef = (DefUseDefinition) other;
    return otherDef.variableName.equals(this.variableName)
        && Objects.equals(otherDef.assigningEdge, this.assigningEdge);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
