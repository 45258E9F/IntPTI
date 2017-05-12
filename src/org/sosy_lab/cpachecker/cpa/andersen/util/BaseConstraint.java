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
 * This class models a BaseConstraint in pointer analysis. This constraint has the
 * structure <code>{a} \subseteq b</code>.
 */
public class BaseConstraint extends Constraint {

  /**
   * Creates a new {@link BaseConstraint} with the given variables for the sub- and superset.
   *
   * @param subVar   Indentifies the only element of the subset in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   */
  public BaseConstraint(String subVar, String superVar) {
    super(subVar, superVar);
  }
}
