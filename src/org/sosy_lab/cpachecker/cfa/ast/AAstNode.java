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

import com.google.common.base.Function;


public interface AAstNode {

  public static final Function<AAstNode, String> TO_AST_STRING = new Function<AAstNode, String>() {

    @Override
    public String apply(AAstNode pInput) {
      return pInput.toASTString();
    }
  };

  public FileLocation getFileLocation();

  public String toASTString();

  public String toParenthesizedASTString();

}
