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

import com.google.common.html.HtmlEscapers;

public final class SourceStringInliner {

  static String inline(String code) {
    String newCode;
    // STEP 1: replace "\ + newline" pattern with empty string
    newCode = code.replaceAll("\\\\[\r\n]+", "");
    // STEP 2: replace "newline" pattern with empty string
    newCode = newCode.replaceAll("\r\n|\n", "");
    // STEP 3: escape the source code for HTML
    newCode = HtmlEscapers.htmlEscaper().escape(newCode);
    return newCode;
  }

}
