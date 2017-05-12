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
package org.sosy_lab.cpachecker.util.predicates.ldd;


public enum TheoryType {

  /**
   * Two variables per inequality
   */
  TVPI,

  /**
   * Two variables per inequality over Z
   */
  TVPIZ,

  /**
   * Unit two variables over inequality over Z
   */
  UTVPIZ

}
