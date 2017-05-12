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
package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import javax.annotation.Nullable;

/**
 * Exception thrown when a CPA cannot handle some code attached to a CFAEdge.
 */
public class UnrecognizedCodeException extends CPATransferException {

  private static final long serialVersionUID = 6425746398197035741L;
  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  protected UnrecognizedCodeException(
      String msg1, @Nullable String msg2,
      @Nullable CFAEdge edge, @Nullable AAstNode astNode) {
    super(createMessage(msg1, msg2, edge, astNode));
  }

  public UnrecognizedCodeException(String msg2, CFAEdge edge, AAstNode astNode) {
    super(createMessage(getPrimaryMessage(edge, astNode), msg2, edge, astNode));
  }

  public UnrecognizedCodeException(String msg2, CFAEdge edge) {
    super(createMessage(getPrimaryMessage(edge, null), msg2, edge, null));
  }

  public UnrecognizedCodeException(String msg2, AAstNode astNode) {
    super(createMessage(getPrimaryMessage(null, astNode), msg2, null, astNode));
  }

  private static String getPrimaryMessage(@Nullable CFAEdge edge, @Nullable AAstNode astNode) {
    Language lang = null;

    if (astNode != null) {
      lang = getLanguage(astNode);
    } else if (edge != null && edge.getRawAST().isPresent()) {
      lang = getLanguage(edge.getRawAST().get());
    }

    if (lang == null) {
      return "Unrecognized code";
    }
    switch (lang) {
      case C:
        return "Unrecognized C code";
      case JAVA:
        return "Unrecognized Java code";
      default:
        throw new AssertionError();
    }
  }

  private static Language getLanguage(AAstNode astNode) {
    if (astNode instanceof CAstNode) {
      return Language.C;
    } else if (astNode instanceof JAstNode) {
      return Language.JAVA;
    }
    throw new AssertionError();
  }

  static String createMessage(
      String msg1, @Nullable String msg2,
      @Nullable CFAEdge edge, @Nullable AAstNode astNode) {
    checkNotNull(msg1);
    if (astNode == null && edge != null && edge.getRawAST().isPresent()) {
      astNode = edge.getRawAST().get();
    }

    StringBuilder sb = new StringBuilder();
    FileLocation fileLocation = null;
    if (astNode != null) {
      fileLocation = astNode.getFileLocation();
    } else if (edge != null) {
      fileLocation = edge.getFileLocation();
    }
    if (fileLocation != null) {
      sb.append(fileLocation);
      sb.append(": ");
    }

    sb.append(msg1);
    if (msg2 != null) {
      sb.append(" (");
      sb.append(msg2);
      sb.append(")");
    }

    if (astNode != null || edge != null) {
      String code;
      if (astNode != null) {
        code = astNode.toASTString();
      } else {
        code = edge.getCode();
      }

      if (!code.isEmpty()) {
        sb.append(": ");
        sb.append(code);

        String rawCode = edge != null ? edge.getRawStatement() : "";

        // remove all whitespaces and trailing semicolons for comparison
        String codeWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(code);
        String rawCodeWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(rawCode);

        codeWithoutWhitespace = SEMICOLON.trimFrom(codeWithoutWhitespace);
        rawCodeWithoutWhitespace = SEMICOLON.trimFrom(rawCodeWithoutWhitespace);

        if (!codeWithoutWhitespace.equals(rawCodeWithoutWhitespace)
            && !rawCodeWithoutWhitespace.isEmpty()) {
          sb.append(" (line was originally ");
          sb.append(rawCode);
          sb.append(")");
        }
      }
    }

    return sb.toString();
  }
}