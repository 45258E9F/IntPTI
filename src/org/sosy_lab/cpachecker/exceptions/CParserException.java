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


public class CParserException extends ParserException {

  private static final long serialVersionUID = 2377475523222354924L;

  public CParserException(String pMsg) {
    super(pMsg, Language.C);
  }

  public CParserException(Throwable pCause) {
    super(pCause, Language.C);
  }


  public CParserException(String pMsg, Throwable pCause) {
    super(pMsg, pCause, Language.C);
  }

  public CParserException(String pMsg, CFAEdge pEdge) {
    super(pMsg, pEdge, Language.C);
  }

}
