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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

/**
 * This interface declares strongest post-operators for symbolic value analysis.
 */
public interface SymbolicStrongestPostOperator
    extends StrongestPostOperator<ForgettingCompositeState> {
}
