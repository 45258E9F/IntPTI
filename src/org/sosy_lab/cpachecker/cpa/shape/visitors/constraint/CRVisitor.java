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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalAndContainer;
import org.sosy_lab.cpachecker.cpa.shape.constraint.LogicalOrContainer;

/**
 * Visitor for {@link ConstraintRepresentation}
 */
public interface CRVisitor<T> extends SEVisitor<T> {

  T visit(LogicalOrContainer pContainer);

  T visit(LogicalAndContainer pContainer);

}
