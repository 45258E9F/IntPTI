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

/**
 * Exception thrown if an error occurs during parsing step (e.g. because the
 * parser library throws an exception).
 */
public class ParserException extends Exception {

  private static final long serialVersionUID = 2377475523222364935L;

  private final Language language;

  public ParserException(String msg, Language pLanguage) {
    super(msg);
    language = pLanguage;
  }

  protected ParserException(Throwable cause, Language pLanguage) {
    super(cause.getMessage(), cause);
    language = pLanguage;
  }

  protected ParserException(String msg, Throwable cause, Language pLanguage) {
    super(msg + ": " + cause.getMessage(), cause);
    language = pLanguage;
  }

  protected ParserException(String msg, CFAEdge edge, Language pLanguage) {
    super(UnrecognizedCodeException.createMessage(msg, null, edge, null));
    language = pLanguage;
  }

  public Language getLanguage() {
    return language;
  }
}
