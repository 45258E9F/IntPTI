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
package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;


public abstract class AbstractAstNode implements AAstNode {

  private final FileLocation fileLocation;

  public AbstractAstNode(final FileLocation pFileLocation) {
    fileLocation = checkNotNull(pFileLocation);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toParenthesizedASTString() {
    return "(" + toASTString() + ")";
  }

  @Override
  public String toString() {
    return toASTString();
  }

  @Override
  public int hashCode() {
    return 2857;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof AbstractAstNode) {
      return true;
    }

    return false;
  }
}