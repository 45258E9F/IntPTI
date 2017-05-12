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
package org.sosy_lab.cpachecker.cpa.shape.constraint;

import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;

public enum SymbolicKind {

  /**
   * the value of symbolic expression is an instance of {@link ShapeSymbolicValue}
   */
  SYMBOLIC,
  /**
   * the value of symbolic expression is an instance of {@link ShapeExplicitValue}
   */
  EXPLICIT,
  /**
   * the value of symbolic expression cannot be represented using neither a symbolic value nor
   * explicit value.
   */
  UNKNOWN

}
