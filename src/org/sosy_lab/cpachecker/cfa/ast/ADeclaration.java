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
package org.sosy_lab.cpachecker.cfa.ast;


/**
 * This interface represents all sorts of top-level declarations (i.e., declarations
 * not nested inside another type declaration).
 * This excludes for examples function parameter declarations and struct members.
 * It includes local and global variables and types, as well as functions.
 */
public interface ADeclaration extends ASimpleDeclaration {


  public boolean isGlobal();
}
