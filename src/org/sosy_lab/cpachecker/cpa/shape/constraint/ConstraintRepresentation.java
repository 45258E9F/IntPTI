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

import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.CRVisitor;

/**
 * All constraint representation should implement this interface.
 */
public interface ConstraintRepresentation {

  <T> T accept(CRVisitor<T> pVisitor);

}
