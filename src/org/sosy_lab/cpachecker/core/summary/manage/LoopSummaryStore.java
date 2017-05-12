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

import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public interface LoopSummaryStore<S extends SummaryInstance> extends SummaryStore {
  public S query(Loop loop);
}
