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

/**
 * This CPA tracks the bounds of loop and callstack unrollings and stops
 * exploration after a given (modifiable) bound is reached.
 *
 * Applications of this CPA are bounded model checking and k-induction.
 */
package org.sosy_lab.cpachecker.cpa.bounds;