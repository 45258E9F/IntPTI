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
 * This is a marker interface that tells other CPAs that this state
 * should not be merged with other states. Other CPAs may or may not reflect
 * this.
 *
 * It is primarily used to tell the merge operator of CompositeCPA to not merge
 * an abstract state.
 */
public interface NonMergeableAbstractState extends AbstractState {

}
