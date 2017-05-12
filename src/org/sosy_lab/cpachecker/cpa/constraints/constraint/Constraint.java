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

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;

/**
 * A single constraint.
 *
 * <p>A constraint is a boolean relation or operation over one or more operands.</p>
 *
 * <p>Possible examples would be relations like <code>'5 < 10'</code>, <code>'n == 10'</code>
 * or <code>'not true'</code></p>
 */
public interface Constraint extends SymbolicValue {

  /**
   * Returns the expression type of the constraint
   */
  Type getType();

  /**
   * Returns whether this constraint is trivial.
   * A constraint is trivial if it does not contain any symbolic identifiers.
   *
   * <p>This method does not check whether a occurring symbolic identifier has a definite
   * assignment, but always returns <code>false</code>, if one exists. To consider
   * definite assignments, use
   * {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintTrivialityChecker}.</p>
   *
   * @return <code>true</code> if the given constraint does not contain any symbolic identifiers,
   * <code>false</code> otherwise</code>
   */
  boolean isTrivial();
}
