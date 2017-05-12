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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import java.util.logging.Level;

interface AutomatonExpression {

  ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;


  static class StringExpression implements AutomatonExpression {
    private String toPrint;

    public StringExpression(String pString) {
      super();
      this.toPrint = pString;
    }

    @Override
    public ResultValue<?> eval(AutomatonExpressionArguments pArgs) {
      // replace $rawstatement
      String str =
          toPrint.replaceAll("\\$[rR]aw[Ss]tatement", pArgs.getCfaEdge().getRawStatement());
      // replace $line
      str = str.replaceAll("\\$[Ll]ine", String.valueOf(pArgs.getCfaEdge().getLineNumber()));
      // replace $location
      str = str.replaceAll("\\$[Ll]ocation", pArgs.getCfaEdge().getFileLocation().toString());
      // replace $file
      str = str.replaceAll("\\$[Ff]ile", pArgs.getCfaEdge().getFileLocation().getFileName());
      // replace $states
      str = str.replaceAll("\\$[Ss]tates", pArgs.getAbstractStates().toString());
      // replace Transition Variables and AutomatonVariables
      str = pArgs.replaceVariables(str);
      if (str == null) {
        return new ResultValue<>("Failure in Variable Replacement in String \"" + toPrint + "\"",
            "ActionExpr.Print");
      } else {
        return new ResultValue<>(str);
      }
    }
  }

  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the
   * query-Result.
   */
  static class CPAQuery implements AutomatonExpression {
    private final String cpaName;
    private final String queryString;

    public CPAQuery(String pCPAName, String pQuery) {
      cpaName = pCPAName;
      queryString = pQuery;
    }

    @Override
    public ResultValue<String> eval(AutomatonExpressionArguments pArgs) {
      // replace transition variables
      String modifiedQueryString = pArgs.replaceVariables(queryString);
      if (modifiedQueryString == null) {
        return new ResultValue<>("Failed to modify queryString \"" + queryString + "\"",
            "AutomatonBoolExpr.CPAQuery");
      }

      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              return new ResultValue<>(result.toString());
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e,
                  "Automaton encountered an Exception during Query of the "
                      + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription());
              return new ResultValue<>("Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription(),
                  "AutomatonExpression.CPAQuery");
            }
          }
        }
      }
      return new ResultValue<>("No State of CPA \"" + cpaName + "\" was found!",
          "AutomatonExpression.CPAQuery");
    }

    @Override
    public String toString() {
      return "EVAL(" + cpaName + "(\"" + queryString + "\"))";
    }
  }


  // TODO: lift CPA Query here

  public static class ResultValue<resultType> {
    private boolean canNotEvaluate = false;
    private String failureMessage = null; // only set if cannotEvaluate == true
    private String failureOrigin = null;  // only set if cannotEvaluate == true
    private resultType value = null;      // only set if cannotEvaluate == false

    public ResultValue(resultType value) {
      this.value = value;
    }

    public ResultValue(String failureMessage, String failureOrigin) {
      this.canNotEvaluate = true;
      this.failureMessage = failureMessage;
      this.failureOrigin = failureOrigin;
    }

    /**
     * Copies the failure messages from the passed result.
     * This Method assumes that the parameter fulfills canNotEvaluate() == true !
     */
    public ResultValue(ResultValue<?> pRes) {
      assert pRes.canNotEvaluate;
      this.canNotEvaluate = true;
      this.failureMessage = pRes.failureMessage;
      this.failureOrigin = pRes.failureOrigin;
    }

    boolean canNotEvaluate() {
      return this.canNotEvaluate;
    }

    /**
     * @returns null if cannotEvaluate() == false
     */
    String getFailureMessage() {
      return failureMessage;
    }

    /**
     * @returns null if cannotEvaluate() == false
     */
    String getFailureOrigin() {
      return failureOrigin;
    }

    /**
     * @returns null if cannotEvaluate() == true
     */
    resultType getValue() {
      return value;
    }
  }
}
