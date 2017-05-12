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
package org.sosy_lab.cpachecker.core.summary.manage;

/**
 * Summary Instance can have its own structure, for instance:
 * 1. AccessSummary: It is a Tree
 * 2. Linear: It is a map from guard condition to formula
 * 3. Pointer: It is a map from pointer to the set of possible memory locations.
 * 4. set of explicit values, etc.
 *
 * Notice that, we require that subclass of SummaryInstance implement
 * a well-defined isEqualTo method, which will be used to determine whether
 * the dependers should be recalculated when a summary instance is updated
 *
 * It should always hold that: equals ==> isEqualTo
 */
public interface SummaryInstance {

  boolean isEqualTo(SummaryInstance that);

}
