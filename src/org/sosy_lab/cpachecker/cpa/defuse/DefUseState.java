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
package org.sosy_lab.cpachecker.cpa.defuse;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Iterator;
import java.util.Set;

public class DefUseState implements AbstractState, Iterable<DefUseDefinition> {
  private final Set<DefUseDefinition> definitions;

  public DefUseState(Set<DefUseDefinition> definitions) {
    this.definitions = ImmutableSet.copyOf(definitions);
  }

  public DefUseState(DefUseState definitions, DefUseDefinition newDefinition) {
    ImmutableSet.Builder<DefUseDefinition> builder = ImmutableSet.builder();
    builder.add(newDefinition);
    for (DefUseDefinition def : definitions.definitions) {
      if (!def.getVariableName().equals(newDefinition.getVariableName())) {
        builder.add(def);
      }
    }
    this.definitions = builder.build();
  }

  @Override
  public Iterator<DefUseDefinition> iterator() {
    return definitions.iterator();
  }

  public boolean containsAllOf(DefUseState other) {
    return definitions.containsAll(other.definitions);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof DefUseState)) {
      return false;
    }

    DefUseState otherDefUse = (DefUseState) other;
    return otherDefUse.definitions.equals(this.definitions);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public int hashCode() {
    return definitions.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('{');

    boolean hasAny = false;
    for (DefUseDefinition def : definitions) {
      CFAEdge assigningEdge = def.getAssigningEdge();
      builder.append('(').append(def.getVariableName()).append(", ");

      if (assigningEdge != null) {
        builder.append(assigningEdge.getPredecessor().getNodeNumber());
      } else {
        builder.append(0);
      }

      builder.append(", ");

      if (assigningEdge != null) {
        builder.append(assigningEdge.getSuccessor().getNodeNumber());
      } else {
        builder.append(0);
      }

      builder.append("), ");
      hasAny = true;
    }

    if (hasAny) {
      builder.replace(builder.length() - 2, builder.length(), "}");
    } else {
      builder.append('}');
    }

    return builder.toString();
  }
}
