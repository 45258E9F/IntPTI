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
package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class BlankEdge extends AbstractCFAEdge {

  private final String description;

  public static final String REPLACEMENT_LABEL = "REPLACEMENT-NOOP";

  public BlankEdge(
      String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor,
      CFANode pSuccessor, String pDescription) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    description = pDescription;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getCode() {
    return "";
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.BlankEdge;
  }

  public static BlankEdge buildNoopEdge(final CFANode pPredecessor, final CFANode pSuccessor) {
    return new BlankEdge("",
        FileLocation.DUMMY,
        pPredecessor,
        pSuccessor,
        REPLACEMENT_LABEL);
  }
}
