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
package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;


public class JParserException extends ParserException {

  private static final long serialVersionUID = 2377445523222164635L;

  public JParserException(String pMsg) {
    super(pMsg, Language.JAVA);
  }

  public JParserException(Throwable pCause) {
    super(pCause, Language.JAVA);
  }

  public JParserException(String pMsg, CFAEdge pEdge) {
    super(pMsg, pEdge, Language.JAVA);
  }

}
