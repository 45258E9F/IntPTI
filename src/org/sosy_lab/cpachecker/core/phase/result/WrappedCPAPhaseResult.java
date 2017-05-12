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
package org.sosy_lab.cpachecker.core.phase.result;

public interface WrappedCPAPhaseResult extends CPAPhaseResult {

  public <T extends CPAPhaseResult> T retrieveWrappedCPAPhaseResult(Class<T> type);

  public Iterable<CPAPhaseResult> getWrappedCPAPhaseResults();

}
