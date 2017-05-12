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
package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import static com.google.common.truth.Truth.assert_;

import org.junit.Test;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.log.TestLogManager;


public class ConverterTest {

  /**
   * just some useless formulas
   */
  private final String[] LINES = {
      "(declare-fun |__VERIFIER_assert::cond@1| () Int)",
      "(define-fun .def_110 () Bool (= |__VERIFIER_assert::cond@1| 0))",
      "(define-fun .def_113 () Bool (<= 1 |main::p@4|))",
      "(define-fun .def_176 () Int (+ .def_75 .def_65))",
      "(define-fun .def_285 () Int (* (- 2) |main::i@3|))",
      "(assert .def_110)",
      "(assert false)",
      "(assert (((a))))",
      "(assert (t t t t t))",
      "(assert (t (t t t) (t t)))",
      "(assert ((t (t t t)) (t t) (t t) t))",
      "(assert (3))",
      "(define-fun .def_178 () Bool (4))",
      "(define-fun .def_178 (t t t) Bool (4))",
  };

  private final LogManagerWithoutDuplicates logger =
      new LogManagerWithoutDuplicates(TestLogManager.getInstance());

  @Test
  public void test1() throws Exception {
    Converter converter = new Converter();
    for (String line : LINES) {
      String converted = FormulaParser.convertFormula(converter, line, logger);
      assert_().that(converted).isEqualTo(line);
    }
  }
}
