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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;

/**
 * Interface for all Initializers that may occur in declarations.
 * E.g array initializer {@link JArrayInitializer},
 * initializer expressions of variable expressions {@link JVariableDeclaration}.
 */
public interface JInitializer extends AInitializer, JAstNode {

}
