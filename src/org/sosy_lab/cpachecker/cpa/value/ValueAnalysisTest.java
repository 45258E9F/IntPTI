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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.util.Map;

public class ValueAnalysisTest {
  // Specification Tests
  @Test
  public void ignoreVariablesTest1() throws Exception {
    // check whether a variable can be ignored (this will lead to a spurious counterexample be found)

    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",
        "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
        "specification", "config/specification/default.spc",
        "ValueAnalysisCPA.precision.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.composite.precAdjust", "COMPONENT",
        "log.consoleLevel", "FINER"
    );

    TestResults results = CPATestRunner.run(
        prop,
        "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
    results.assertIsUnsafe();
  }

  @Test
  public void ignoreVariablesTest2() throws Exception {
    // check whether the counterexample is indeed not found if the variable is not ignored

    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",
        "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA",
        "specification", "config/specification/default.spc",
        "ValueAnalysisCPA.precision.variableBlacklist", "somethingElse"
    );

    TestResults results = CPATestRunner.run(
        prop,
        "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
    results.assertIsSafe();
  }
}
