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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class BAMARGBlockStartState extends ARGState {

  private static final long serialVersionUID = -5143941913753150639L;

  private ARGState analyzedBlock = null;

  public BAMARGBlockStartState(AbstractState pWrappedState, ARGState pParentElement) {
    super(pWrappedState, pParentElement);
  }

  public void setAnalyzedBlock(ARGState pRootOfBlock) {
    analyzedBlock = pRootOfBlock;
  }


  public ARGState getAnalyzedBlock() {
    return analyzedBlock;
  }

  @Override
  public String toString() {
    return "BAMARGBlockStartState " + super.toString();
  }
}
