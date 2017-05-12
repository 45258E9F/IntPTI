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

/**
 * This package defines a CPA that determines the level of (non-)determism of a program,
 * by inspecting how many assume edges can be evaluated to a concrete result, and how many not
 * This should be a good indicator for whether using refinement for the value analysis or not
 * (e.g. product lines are quite deterministic, and work very good without refinement,
 * while it is the other way round for most ldv* tasks)
 */
package org.sosy_lab.cpachecker.cpa.deterministic;