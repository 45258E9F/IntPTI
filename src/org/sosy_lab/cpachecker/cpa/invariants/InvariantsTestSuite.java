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
package org.sosy_lab.cpachecker.cpa.invariants;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical.IIIOperatorTest;
import org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical.ISIOperatorTest;

@RunWith(Suite.class)
@SuiteClasses({
    CompoundMathematicalIntervalTest.class,
    SimpleIntervalTest.class,
    IIIOperatorTest.class,
    ISIOperatorTest.class})
public class InvariantsTestSuite {

}
