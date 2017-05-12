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
package org.sosy_lab.cpachecker.core.interfaces.checker.util;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReportWithTrace;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;


/**
 * A utility class for manipulating error reports
 */
public final class ErrorReports {

  private ErrorReports() {
  }

  /**
   * Two error reports are regarded as equivalent if they have the same weakness type, the same
   * AST node and the same ARG path (error trace).
   *
   * @param error1 error report 1
   * @param error2 error report 2
   * @return whether two error reports are equivalent
   */
  public static boolean equals(ErrorReport error1, ErrorReport error2) {
    if (error1.getWeakness().equals(error2.getWeakness())) {
      FileLocation loc1 = error1.getErrorSpot().getFileLocation();
      FileLocation loc2 = error2.getErrorSpot().getFileLocation();
      if (loc1.equals(loc2)) {
        if ((error1 instanceof ErrorReportWithTrace) && (error2 instanceof ErrorReportWithTrace)) {
          ARGPath trace1 = ((ErrorReportWithTrace) error1).getErrorTrace();
          ARGPath trace2 = ((ErrorReportWithTrace) error2).getErrorTrace();
          return trace1.equals(trace2);
        } else if (!(error1 instanceof ErrorReportWithTrace) && !(error2 instanceof
            ErrorReportWithTrace)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Two errors are similar if they have the same type and the same faulty AST node.
   * They may have different error traces, but in general this means an error has more than one
   * means to be triggered
   *
   * @param error1 error report 1
   * @param error2 error report 2
   * @return whether two error reports are similar
   */
  public static boolean similarize(ErrorReport error1, ErrorReport error2) {
    if ((error1 instanceof ErrorReportWithTrace) != (error2 instanceof ErrorReportWithTrace)) {
      return false;
    }
    if (error1.getWeakness().equals(error2.getWeakness())) {
      if (error1.getErrorSpot().getFileLocation().equals(error2.getErrorSpot()
          .getFileLocation())) {
        return true;
      }
    }
    return false;
  }

}
