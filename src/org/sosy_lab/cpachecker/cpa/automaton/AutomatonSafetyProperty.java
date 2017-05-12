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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.Property;

import javax.annotation.Nonnull;


public class AutomatonSafetyProperty implements Property {

  @Nonnull
  private final Automaton automaton;
  @Nonnull
  private final AutomatonTransition automatonTrans;
  @Nonnull
  private final String propertyInstanceDescription;

  public AutomatonSafetyProperty(
      Automaton pAutomaton,
      AutomatonTransition pTransition,
      String pDesc) {
    this.automaton = Preconditions.checkNotNull(pAutomaton);
    this.automatonTrans = Preconditions.checkNotNull(pTransition);
    this.propertyInstanceDescription = Preconditions.checkNotNull(pDesc);
  }

  public AutomatonSafetyProperty(Automaton pAutomaton, AutomatonTransition pTransition) {
    this.automaton = Preconditions.checkNotNull(pAutomaton);
    this.automatonTrans = Preconditions.checkNotNull(pTransition);
    this.propertyInstanceDescription = "";
  }

  public AutomatonTransition getAutomatonTransition() {
    return automatonTrans;
  }

  @Override
  public String toString() {
    return propertyInstanceDescription.length() > 0
           ? propertyInstanceDescription
           : automaton.getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + automaton.hashCode();
    result = prime * result + automatonTrans.hashCode();
    result = prime * result + propertyInstanceDescription.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AutomatonSafetyProperty other = (AutomatonSafetyProperty) obj;

    if (!automatonTrans.equals(other.automatonTrans)) {
      return false;
    }

    if (!automaton.equals(other.automaton)) {
      return false;
    }

    if (!propertyInstanceDescription.equals(other.propertyInstanceDescription)) {
      return false;
    }

    return true;
  }

}
