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
package org.sosy_lab.cpachecker.cpa.pointer2.summary;

import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;

/**
 * Created by landq on 11/25/16.
 */
public interface MergeSummary {

  /**
   * Merge summaries
   *
   * @param state       state need to be strengthened
   * @param otherStates current components of composite state
   * @return the strengthened state
   */
  Summary MergeSummary(PointerState state, Summary summary1, Summary summary2);

}
