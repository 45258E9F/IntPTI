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

/**
 * The classes that are used to represent single program statements, declarations,
 * and expressions in form of an abstract syntax tree (AST).
 * Sub-packages contain language-specific sub-classes
 * for representation of features of specific languages.
 *
 * The classes in this package have an "A" as prefix to show that they are
 * language-independent, in contrast to the language-specific classes
 * with prefixes like "C" and "J".
 * All classes in this package named "Abstract*" are only relevant
 * for implementing the language-specific sub-classes
 * and should not be used by other code.
 */
package org.sosy_lab.cpachecker.cfa.ast;