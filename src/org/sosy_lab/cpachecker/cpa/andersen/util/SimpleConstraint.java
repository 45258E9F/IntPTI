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
package org.sosy_lab.cpachecker.cpa.andersen.util;


/**
 * This class models a SimpleConstraint in pointer analysis. This constraint has the
 * structure <code>a \subseteq b</code>.
 */
public class SimpleConstraint extends Constraint {

  /**
   * Creates a new {@link SimpleConstraint} with the given variables for the sub- and superset.
   *
   * @param subVar   Indentifies the subset variable in this Constraint.
   * @param superVar Indentifies the superset variable in this Constraint.
   */
  public SimpleConstraint(String subVar, String superVar) {
    super(subVar, superVar);
  }
}
