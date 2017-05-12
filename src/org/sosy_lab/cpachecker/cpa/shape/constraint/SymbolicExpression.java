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
package org.sosy_lab.cpachecker.cpa.shape.constraint;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SEVisitor;

/**
 * A symbolic expression is a representation of C expression where operands are replaced with
 * symbolic values and/or explicit values.
 */
public interface SymbolicExpression extends ConstraintRepresentation {

  ShapeValue getValue();

  SymbolicKind getValueKind();

  CType getType();

  CExpression getOriginalExpression();

  CExpression getSymbolicExpression();

  <T> T accept(SEVisitor<T> pVisitor);

}
