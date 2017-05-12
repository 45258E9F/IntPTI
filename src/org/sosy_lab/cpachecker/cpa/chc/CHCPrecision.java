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
package org.sosy_lab.cpachecker.cpa.chc;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.ArrayList;


public class CHCPrecision implements Precision {

  private ArrayList<String> Vars;

  public CHCPrecision() {
    Vars = null;
  }

  public boolean isDisabled() {
    if (Vars == null) {
      return true;
    }
    return false;
  }

}
