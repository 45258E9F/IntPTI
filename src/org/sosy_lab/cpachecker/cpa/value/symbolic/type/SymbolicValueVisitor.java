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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

/**
 * Visitor for {@link SymbolicValue}s.
 *
 * @param <T> return type of visit methods
 */
public interface SymbolicValueVisitor<T> {

  T visit(SymbolicIdentifier pValue);

  T visit(ConstantSymbolicExpression pExpression);

  T visit(AdditionExpression pExpression);

  T visit(SubtractionExpression pExpression);

  T visit(MultiplicationExpression pExpression);

  T visit(DivisionExpression pExpression);

  T visit(ModuloExpression pExpression);

  T visit(BinaryAndExpression pExpression);

  T visit(BinaryNotExpression pExpression);

  T visit(BinaryOrExpression pExpression);

  T visit(BinaryXorExpression pExpression);

  T visit(ShiftRightExpression pExpression);

  T visit(ShiftLeftExpression pExpression);

  T visit(LogicalNotExpression pExpression);

  T visit(LessThanOrEqualExpression pExpression);

  T visit(LessThanExpression pExpression);

  T visit(EqualsExpression pExpression);

  T visit(LogicalOrExpression pExpression);

  T visit(LogicalAndExpression pExpression);

  T visit(CastExpression pExpression);

  T visit(PointerExpression pExpression);

  T visit(AddressOfExpression pExpression);

  T visit(NegationExpression pExpression);
}
