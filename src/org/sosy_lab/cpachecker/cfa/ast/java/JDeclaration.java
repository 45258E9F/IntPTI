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

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;

/**
 * This interface represents all sorts of top-level declarations (i.e., declarations
 * not nested inside another type declaration).
 * This excludes for examples methods parameter declarations.
 * It includes variables, as well as methods.
 */
public interface JDeclaration extends ADeclaration, JSimpleDeclaration {

}
