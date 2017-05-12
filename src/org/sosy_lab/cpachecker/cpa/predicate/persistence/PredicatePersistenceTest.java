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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.CharMatcher;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverContextFactory.Solvers;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.test.SolverBasedTest0;

import java.util.List;

@RunWith(Parameterized.class)
public class PredicatePersistenceTest extends SolverBasedTest0 {

  @Parameters(name = "{0}")
  public static Solvers[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  private FormulaManagerView mgrv;

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() {
    return super.createTestConfigBuilder().setOption("cpa.predicate.encodeFloatAs", "integer");
  }

  @Before
  public void init() throws InvalidConfigurationException {
    mgrv = new FormulaManagerView(mgr, config, logger);
  }

  @Test
  public void testSplitFormula_Syntactically() {

    BooleanFormula f1 =
        imgr.equal(imgr.makeVariable("variable_with_long_name"), imgr.makeNumber(1));
    BooleanFormula f2 =
        imgr.equal(
            imgr.makeVariable("variable_with_long_name2"),
            imgr.makeVariable("variable_with_long_name"));
    BooleanFormula f = bmgr.and(f1, f2);

    Pair<String, List<String>> result = PredicatePersistenceUtils.splitFormula(mgrv, f);
    String assertFormula = result.getFirst();
    List<String> declarationFormulas = result.getSecond();

    assertThat(assertFormula).startsWith("(assert ");
    assertThat(assertFormula).endsWith(")");
    assertThat(assertFormula).doesNotContain("\n");

    assertThatAllParenthesesAreClosed(assertFormula);

    for (String declaration : declarationFormulas) {
      if (!(declaration.startsWith("(define-fun ")
          || declaration.startsWith("(declare-fun ")
          || declaration.startsWith("(set-info ")
          || declaration.startsWith("(set-logic "))) {
        Truth.assert_().fail("Unexpected statement in <%s>", declaration);
      }
      assertThat(declaration).endsWith(")");
      assertThatAllParenthesesAreClosed(declaration);
    }
  }

  private void assertThatAllParenthesesAreClosed(String formula) {
    assertThat(CharMatcher.anyOf(")").countIn(formula))
        .named("number of closing parentheses")
        .isEqualTo(CharMatcher.anyOf("(").countIn(formula));
  }
}
