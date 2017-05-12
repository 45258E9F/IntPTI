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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;


public class FunctionExitNode extends CFANode {

  private FunctionEntryNode entryNode;

  public FunctionExitNode(String pFunctionName) {
    super(pFunctionName);
  }

  public void setEntryNode(FunctionEntryNode pEntryNode) {
    checkState(entryNode == null);
    entryNode = checkNotNull(pEntryNode);
  }

  public FunctionEntryNode getEntryNode() {
    checkState(entryNode != null);
    return entryNode;
  }
}
