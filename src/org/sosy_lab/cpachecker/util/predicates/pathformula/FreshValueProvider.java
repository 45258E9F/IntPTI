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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;

/**
 * Class for retrieving fresh indices for an old value from an SSAMap, based on some increment.
 */
public interface FreshValueProvider {

  /**
   * get a new unused value based on the given one.
   */
  public int getFreshValue(String variable, int value);

  /**
   * get a new provider, that is based on the current one and the given one.
   */
  public FreshValueProvider merge(FreshValueProvider other);


  /**
   * The default implementation returns always the old index plus default increment.
   */
  public static class DefaultFreshValueProvider implements FreshValueProvider, Serializable {

    private static final long serialVersionUID = 8349867460915893626L;
    // Default difference for two SSA-indizes of the same name.
    @VisibleForTesting
    public static final int DEFAULT_INCREMENT = 1;

    @Override
    public int getFreshValue(String variable, int value) {
      return value + DEFAULT_INCREMENT;
    }

    /**
     * returns the current FreshValueProvider without a change.
     */
    @Override
    public FreshValueProvider merge(FreshValueProvider other) {
      if (other instanceof DefaultFreshValueProvider) {
        return this;
      } else {
        return other.merge(this); // endless recursion!!
      }
    }

  }
}
