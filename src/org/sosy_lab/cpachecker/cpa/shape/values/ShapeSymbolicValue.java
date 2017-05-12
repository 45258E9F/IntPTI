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
 * A symbolic value is not the concrete value. It is used to denote some values such as addresses
 * . In the contrary, explicit value is the concrete value. For example, an integer variable
 * assigned to 10 has the concrete value 10.
 */
public interface ShapeSymbolicValue extends ShapeValue {
}
