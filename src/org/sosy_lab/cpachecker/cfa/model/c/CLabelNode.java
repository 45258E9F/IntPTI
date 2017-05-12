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
package org.sosy_lab.cpachecker.cfa.model.c;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CLabelNode extends CFANode {

  private final String label;

  public CLabelNode(String pFunctionName, String pLabel) {
    super(pFunctionName);
    checkArgument(!pLabel.isEmpty());
    label = pLabel;
  }

  public String getLabel() {
    return label;
  }
}
