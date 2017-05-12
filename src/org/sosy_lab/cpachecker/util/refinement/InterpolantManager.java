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
package org.sosy_lab.cpachecker.util.refinement;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Classes implementing this interface are able to create interpolants out of subtypes of
 * {@link AbstractState}.
 */
public interface InterpolantManager<S extends AbstractState, I extends Interpolant<S>> {

  I createInitialInterpolant();

  I createInterpolant(S state);

  I getTrueInterpolant();

  I getFalseInterpolant();

}
