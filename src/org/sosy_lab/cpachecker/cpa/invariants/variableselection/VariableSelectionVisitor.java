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
package org.sosy_lab.cpachecker.cpa.invariants.variableselection;


public interface VariableSelectionVisitor<ConstantType, T> {

  T visit(AcceptAllVariableSelection<ConstantType> pAcceptAllVariableSelection);

  T visit(AcceptSpecifiedVariableSelection<ConstantType> pAcceptSpecifiedVariableSelection);

}
