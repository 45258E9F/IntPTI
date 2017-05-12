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
package org.sosy_lab.cpachecker.cfa.ast.c;


public interface CDesignatorVisitor<R, X extends Exception> {

  R visit(CArrayDesignator pArrayDesignator) throws X;

  R visit(CArrayRangeDesignator pArrayRangeDesignator) throws X;

  R visit(CFieldDesignator pFieldDesignator) throws X;
}
