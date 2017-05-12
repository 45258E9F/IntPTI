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
package org.sosy_lab.cpachecker.cfa.types;


import java.io.Serializable;

public interface Type extends Serializable {


  /**
   * Return a string representation of a variable declaration with a given name
   * and this type.
   *
   * Example:
   * If this type is array of int, and we call <code>toASTString("foo")</code>,
   * the result is <pre>int foo[]</pre>.
   *
   * @param declarator The name of the variable to declare.
   * @return A string representation of this type.
   */
  public String toASTString(String declarator);

}
