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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Strings;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemHolder;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;

import javax.annotation.Nullable;

/**
 * Handles problems during CFA generation
 */
class CFAGenerationRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6850681425709171716L;

  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  public CFAGenerationRuntimeException(String msg) {
    super(msg);
  }

  public CFAGenerationRuntimeException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  public CFAGenerationRuntimeException(
      String msg, IASTNode astNode,
      Function<String, String> niceFileNameFunction) {
    this(astNode == null ? msg : createMessage(msg, astNode, niceFileNameFunction));
  }

  public CFAGenerationRuntimeException(String msg, CAstNode astNode) {
    this(astNode == null ? msg :
         (astNode.getFileLocation() + ": " + msg + ": " + astNode.toASTString()));
  }

  public CFAGenerationRuntimeException(
      IASTProblem problem,
      Function<String, String> niceFileNameFunction) {
    this(createMessage(problem.getMessage(), problem, niceFileNameFunction));
  }

  public <P extends IASTProblemHolder & IASTNode> CFAGenerationRuntimeException(
      P problem, Function<String, String> niceFileNameFunction) {
    this(createMessage(problem.getProblem().getMessage(), problem, niceFileNameFunction));
  }

  private static String createMessage(
      String msg, IASTNode node,
      Function<String, String> niceFileNameFunction) {
    StringBuilder sb = new StringBuilder();

    @Nullable
    IASTFileLocation fileLocation = node.getFileLocation();
    if (fileLocation != null) {
      String fileName = niceFileNameFunction.apply(fileLocation.getFileName());
      if (!fileName.isEmpty()) {
        sb.append(fileName);
        sb.append(", ");
      }
      if (fileLocation.getEndingLineNumber() != fileLocation.getStartingLineNumber()) {
        sb.append("lines ");
        sb.append(fileLocation.getStartingLineNumber());
        sb.append("-");
        sb.append(fileLocation.getEndingLineNumber());
      } else {
        sb.append("line ");
        sb.append(fileLocation.getStartingLineNumber());
      }
      sb.append(": ");
    }

    if (Strings.isNullOrEmpty(msg)) {
      sb.append("Problem");
    } else {
      sb.append(msg);
    }
    sb.append(": ");

    String rawSignature = node.getRawSignature();
    sb.append(rawSignature);

    // search the ast node for the whole statement / declaration / line
    IASTNode fullLine = node;
    while ((fullLine != null)
        && !(fullLine instanceof IASTStatement)
        && !(fullLine instanceof IASTDeclaration)) {

      fullLine = fullLine.getParent();
    }

    if (fullLine != null && fullLine != node) {
      String lineRawSignature = fullLine.getRawSignature();

      String codeWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(rawSignature);
      String lineWithoutWhitespace = CharMatcher.WHITESPACE.removeFrom(lineRawSignature);

      // remove all whitespaces and trailing semicolons for comparison
      codeWithoutWhitespace = SEMICOLON.trimFrom(codeWithoutWhitespace);
      lineWithoutWhitespace = SEMICOLON.trimFrom(lineWithoutWhitespace);

      if (!codeWithoutWhitespace.equals(lineWithoutWhitespace)) {
        sb.append(" (full line is ");
        sb.append(lineRawSignature);
        sb.append(")");
      }
    }

    return sb.toString();
  }
}
