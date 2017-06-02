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
package org.sosy_lab.cpachecker.core.phase.fix.util;

import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;

public class SpecifierFixMetaInfo implements IntegerFixMetaInfo {

  private final String varName;

  private SpecifierFixMetaInfo(String pName) {
    varName = pName;
  }

  public static SpecifierFixMetaInfo of(String pName) {
    return new SpecifierFixMetaInfo(pName);
  }

  @Override
  public IntegerFixMode getMode() {
    return IntegerFixMode.SPECIFIER;
  }

  @Override
  public String toString() {
    // Note: the name for meta-field should start with "_"
    return "\"_var\":\"" + SourceStringInliner.inline(varName) + "\"";
  }
}
