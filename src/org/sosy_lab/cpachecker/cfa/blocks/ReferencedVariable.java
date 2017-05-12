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
package org.sosy_lab.cpachecker.cfa.blocks;

import java.util.Set;

/**
 * Represents a reference to a variable in the CFA.
 */
public class ReferencedVariable {
  private final String ident;
  private final boolean occursInCondition;

  // This set contains all variables,that are used in assignment of this variable.
  // Example: influencingVariables of "a" are "b" and "c" from "a=b+c;"
  // The Set can be empty, if the variable is not assigned (in the current BAM-block).
  // Set is not included in equals and hashcode, because it can be changed later and may contain circular references.
  private final Set<ReferencedVariable> influencingVariables;

  public ReferencedVariable(
      String pIdent, boolean pOccursInCondition,
      Set<ReferencedVariable> pInfluencingByVariables) {
    ident = pIdent.replaceAll("[ \n\t]", ""); //mimic behavior of CtoFormulaConverter.exprToVarName
    occursInCondition = pOccursInCondition;
    influencingVariables = pInfluencingByVariables;
  }

  public boolean occursInCondition() {
    return occursInCondition;
  }

  public String getName() {
    return ident;
  }

  public Set<ReferencedVariable> getInfluencingVariables() {
    return influencingVariables;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ReferencedVariable)) {
      return false;
    }

    ReferencedVariable rhs = (ReferencedVariable) o;
    return ident.equals(rhs.ident) && occursInCondition == rhs.occursInCondition;
  }

  @Override
  public int hashCode() {
    return ident.hashCode() + (occursInCondition ? 7 : 0);
  }

  @Override
  public String toString() {
    return ident;
  }
}
