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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.io.Serializable;

/**
 * Marker interface for symbolic values.
 * <p/>
 * Each class implementing this interface should provide an <code>equals(Object)</code> method
 * that allows checks for equality of symbolic values.
 */
public interface SymbolicValue extends Value, Serializable {

  <T> T accept(SymbolicValueVisitor<T> pVisitor);

  /**
   * Returns the memory location this symbolic value represents.
   */
  Optional<MemoryLocation> getRepresentedLocation();

  SymbolicValue copyForLocation(MemoryLocation pLocation);

  /**
   * Returns a string representation of this symbolic value with symbolic expressions representing
   * a certain memory locations replaced with these locations.
   */
  String getRepresentation();
}
