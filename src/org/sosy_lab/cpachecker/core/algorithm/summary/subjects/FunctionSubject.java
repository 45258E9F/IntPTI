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
package org.sosy_lab.cpachecker.core.algorithm.summary.subjects;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummarySubject;

import java.util.Map;

/**
 * @author tomgu function subject For each function with definition, we store it in subjects For
 *         each function without definition, we store it in subjectsByName
 */
public class FunctionSubject implements SummarySubject {

  /* *********** */
  /* global part */
  /* *********** */

  // Note: FunctionSubject contains all the functions with a definition
  private static Map<FunctionEntryNode, FunctionSubject> subjects = Maps.newHashMap();
  // for those without definition
  private static Map<String, FunctionSubject> subjectsByName = Maps.newHashMap();

  public static FunctionSubject of(FunctionEntryNode fun) {
    if (!subjects.containsKey(fun)) {
      subjects.put(fun, new FunctionSubject(fun));
    }
    return subjects.get(fun);
  }

  // this method is used for functions with no definition
  public static FunctionSubject of(String fName) {
    if (!subjectsByName.containsKey(fName)) {
      subjectsByName.put(fName, new FunctionSubject(fName, null));
    }
    return subjectsByName.get(fName);
  }

  /* ********** */
  /* class part */
  /* ********** */

  private final FunctionEntryNode fun;
  // in general, it is sufficient to use function name as the unique identifier
  private final String fName;

  private FunctionSubject(FunctionEntryNode fun) {
    this(fun.getFunctionName(), fun);
  }

  private FunctionSubject(String pFName, FunctionEntryNode fun) {
    fName = pFName;
    this.fun = fun;
  }

  public FunctionEntryNode getFunctionEntry() {
    return fun;
  }

  public String getFunctionName() {
    return fName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "FunctionSubject [fun=" + fName + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof FunctionSubject)) {
      return false;
    }
    FunctionSubject that = (FunctionSubject) obj;
    return Objects.equal(fName, that.fName);
  }
}
