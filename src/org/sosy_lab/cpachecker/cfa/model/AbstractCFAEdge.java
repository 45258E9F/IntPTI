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


import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract class AbstractCFAEdge implements CFAEdge {

  private final CFANode predecessor;
  private final CFANode successor;
  private final String rawStatement;
  private final FileLocation fileLocation;

  public AbstractCFAEdge(
      String pRawStatement, FileLocation pFileLocation,
      CFANode pPredecessor, CFANode pSuccessor) {

    Preconditions.checkNotNull(pRawStatement);
    Preconditions.checkNotNull(pPredecessor);
    Preconditions.checkNotNull(pSuccessor);

    predecessor = pPredecessor;
    successor = pSuccessor;
    rawStatement = pRawStatement;
    fileLocation = checkNotNull(pFileLocation);
  }

  @Override
  public CFANode getPredecessor() {
    return predecessor;
  }

  @Override
  public CFANode getSuccessor() {
    return successor;
  }

  @Override
  public String getRawStatement() {
    return rawStatement;
  }

  @Override
  public Optional<? extends AAstNode> getRawAST() {
    return Optional.absent();
  }

  @Override
  public String getDescription() {
    return getCode();
  }

  @Override
  public int getLineNumber() {
    return fileLocation.getStartingLineNumber();
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public int hashCode() {
    return 31 * predecessor.hashCode() + successor.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof AbstractCFAEdge)) {
      return false;
    }

    AbstractCFAEdge otherEdge = (AbstractCFAEdge) pOther;

    if ((otherEdge.predecessor != predecessor)
        || (otherEdge.successor != successor)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return getFileLocation() + ":\t" + getPredecessor() + " -{" +
        getDescription().replaceAll("\n", " ") +
        "}-> " + getSuccessor();
  }
}
