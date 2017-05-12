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
package org.sosy_lab.cpachecker.core.interfaces;


/**
 * This interface represents a property that is checked
 * during the analysis.
 *
 * A specification might consist of a set of properties.
 *
 * Instances of this interface...
 * MUST override the .toString() method to provide a description of the property!
 * MIGHT override the .equals(...) method! (and implicitly the hashCode() method)
 */
public interface Property {

  /**
   * @return The textual description of the property.
   */
  @Override
  public String toString();

}
