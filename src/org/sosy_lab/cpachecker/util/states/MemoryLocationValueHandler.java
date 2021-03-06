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
package org.sosy_lab.cpachecker.util.states;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * Classes implementing this interface provide means for altering the value of
 * {@link MemoryLocation}s in a specific way.
 */
public interface MemoryLocationValueHandler {

  /**
   * Handles the value of the given memory location in the given state in an
   * implementation-dependent way. A call to this method could assign a new value to the given
   * memory location or even remove it from the state.
   *
   * @param pMemLocation  the memory location to alter
   * @param pType         the type of the variable at the given memory location
   * @param pState        the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use.
   *                      Value assignments will happen in this state
   * @param pValueVisitor a value visitor for possibly needed evaluations or computations
   * @throws UnrecognizedCCodeException thrown if the given parameters do not fit. Other causes for
   *                                    this exception may be implementation-dependent
   */
  void handle(
      MemoryLocation pMemLocation, Type pType,
      ValueAnalysisState pState, ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCCodeException;
}
