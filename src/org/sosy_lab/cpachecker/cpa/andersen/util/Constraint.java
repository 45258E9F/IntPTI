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
package org.sosy_lab.cpachecker.cpa.andersen.util;


/**
 * This class models an abstract Constraint in pointer analysis. All constraints have a similar
 * structure <code>a \subseteq b</code>.
 */
public abstract class Constraint {

  /**
   * Indentifies the subset variable in this Constraint.
   */
  private final String subVar;

  /**
   * Indentifies the superset variable in this Constraint.
   */
  private final String superVar;

  /**
   * Creates a new {@link Constraint} with the given variables for the sub- and superset.
   *
   * @param subVar   Indentifies the subset variable in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   */
  public Constraint(String subVar, String superVar) {

    this.subVar = subVar;
    this.superVar = superVar;
  }

  /**
   * Returns the String identifying the subset of this constraint.
   *
   * @return the String identifying the subset of this constraint.
   */
  public String getSubVar() {
    return this.subVar;
  }

  /**
   * Returns the String identifying the superset of this constraint.
   *
   * @return the String identifying the superset of this constraint.
   */
  public String getSuperVar() {
    return this.superVar;
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (other == null || !this.getClass().equals(other.getClass())) {
      return false;
    }

    Constraint o = (Constraint) other;

    return this.subVar.equals(o.subVar) && this.superVar.equals(o.superVar);
  }

  @Override
  public int hashCode() {

    int hash = 18;
    hash = 31 * hash + (subVar == null ? 0 : subVar.hashCode());
    hash = 31 * hash + (superVar == null ? 0 : superVar.hashCode());

    return hash;
  }
}
