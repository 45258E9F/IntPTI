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
package org.sosy_lab.cpachecker.core;

/**
 * Enum that defines the analysis direction.
 * Using a boolean flag for this purpose is considered as a bad practice,
 * since it reduces the readability of code, and increases the risk of bugs.
 * (see http://blog.codinghorror.com/avoiding-booleans/)
 */
public enum AnalysisDirection {
  FORWARD,
  BACKWARD
}
