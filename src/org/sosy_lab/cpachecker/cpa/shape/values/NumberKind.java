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
package org.sosy_lab.cpachecker.cpa.shape.values;

/**
 * For now, we use builtin numeric values to simulate C program arithmetic. There are 3 kinds of
 * arithmetic mode: INT, FLOAT and DOUBLE. For now we do not support LONG DOUBLE.
 */
public enum NumberKind {

  INT,
  BIG_INT,
  FLOAT,
  DOUBLE

}
