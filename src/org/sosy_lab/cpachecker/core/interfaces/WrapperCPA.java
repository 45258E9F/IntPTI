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
package org.sosy_lab.cpachecker.core.interfaces;

/**
 * Interface for classes that are wrapping CPAs
 * (like composite CPAs)
 */
public interface WrapperCPA {

  /**
   * Retrieve one of the wrapped CPAs by type. If the hierarchy of (wrapped)
   * CPAs has several levels, this method searches through them recursively.
   *
   * The type does not need to match exactly, the returned element has just to
   * be a sub-type of the type passed as argument.
   *
   * @param <T>  The type of the wrapped element.
   * @param type The class object of the type of the wrapped element.
   * @return An instance of an element with type T or null if there is none.
   */
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> type);

  /**
   * Retrieve all wrapped CPAs contained directly in this object (not recursively).
   *
   * @return A non-empty unmodifiable list of CPAs.
   */
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs();
}
