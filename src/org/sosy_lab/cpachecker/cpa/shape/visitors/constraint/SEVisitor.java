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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;

/**
 * Visitor for {@link SymbolicExpression}
 */
public interface SEVisitor<T> {

  T visit(ConstantSE pValue);

  T visit(BinarySE pValue);

  T visit(UnarySE pValue);

  T visit(CastSE pValue);

}
