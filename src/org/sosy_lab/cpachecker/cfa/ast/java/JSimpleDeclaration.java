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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * It is part of the declaration of types and variables (see {@link JDeclaration})
 * and methods (see {@link JMethodDeclaration}).
 * It is also used stand-alone for the declaration of function parameters.
 */
public interface JSimpleDeclaration extends ASimpleDeclaration, JAstNode {

  @Override
  public JType getType();

}
