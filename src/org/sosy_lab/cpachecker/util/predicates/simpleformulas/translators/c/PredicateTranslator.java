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
package org.sosy_lab.cpachecker.util.predicates.simpleformulas.translators.c;

import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Constant;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Predicate;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.TermVisitor;
import org.sosy_lab.cpachecker.util.predicates.simpleformulas.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PredicateTranslator {

  private static Map<Predicate, String> mCache = new HashMap<>();

  public static String translate(Predicate pPredicate) {
    if (mCache.containsKey(pPredicate)) {
      return mCache.get(pPredicate);
    }

    Set<String> lVariables = new HashSet<>();

    Visitor lVisitor = new Visitor();
    lVariables.addAll(pPredicate.getLeftTerm().accept(lVisitor));
    lVariables.addAll(pPredicate.getRightTerm().accept(lVisitor));

    StringBuilder lResult = new StringBuilder();

    lResult.append("void predicate(");

    boolean isFirst = true;

    for (String lVariable : lVariables) {
      if (isFirst) {
        isFirst = false;
      } else {
        lResult.append(", ");
      }

      lResult.append("int ");
      lResult.append(lVariable);
    }

    lResult.append(") { (");
    lResult.append(pPredicate.toString());
    lResult.append("); }");

    mCache.put(pPredicate, lResult.toString());

    return lResult.toString();
  }

  private static class Visitor implements TermVisitor<Set<String>> {

    @Override
    public Set<String> visit(Constant pConstant) {
      return Collections.emptySet();
    }

    @Override
    public Set<String> visit(Variable pVariable) {
      return Collections.singleton(pVariable.toString());
    }

  }

}
