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

import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * An AbstractState that evaluates Properties (String-encoded) and
 * returns whether they are satisfied in concrete states represented by the AbstractState.
 */
public interface AbstractQueryableState extends AbstractState {

  String getCPAName();

  /**
   * Checks whether this AbstractState satisfies the property.
   * Each CPA defines which properties can be evaluated.
   *
   * This method is never called from outside, but it should be used
   * as "type-safe" version of {@link #evaluateProperty(String)} internally
   * and {@link #evaluateProperty(String)} can forward to this method.
   *
   * @param property the property to be checked
   * @return if the property is satisfied
   * @throws InvalidQueryException if the property is not given in the (CPA-specific) syntax
   */
  boolean checkProperty(String property) throws InvalidQueryException;

  Object evaluateProperty(String property) throws InvalidQueryException;

  /**
   * Modifies the internal state of this AbstractState.
   * Each CPA defines a separate language for definition of modifications.
   *
   * @param modification how the state should be modified
   * @throws InvalidQueryException if the modification is not given in the (CPA-specific) syntax
   */
  void modifyProperty(String modification) throws InvalidQueryException;

}
