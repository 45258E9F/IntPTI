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
package org.sosy_lab.cpachecker.core.interfaces;

import static org.mockito.Mockito.mock;

import com.google.common.testing.ClassSanityTester;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;

public class PrecisionAdjustmentResultTest {

  @Test
  public void testCreation() {
    AbstractState state = mock(AbstractState.class);
    Precision precision = mock(Precision.class);
    Action action = Action.BREAK;

    PrecisionAdjustmentResult r = PrecisionAdjustmentResult.create(state, precision, action);
    Assert.assertSame(state, r.abstractState());
    Assert.assertSame(precision, r.precision());
    Assert.assertSame(action, r.action());
  }

  @Test
  public void testEquals() {
    new ClassSanityTester().testEquals(PrecisionAdjustmentResult.class);
  }

  @Test
  public void testNulls() {
    new ClassSanityTester().testNulls(PrecisionAdjustmentResult.class);
  }

  @Test
  public void testWithAbstractState() {
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
        mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    AbstractState newState = mock(AbstractState.class);
    PrecisionAdjustmentResult r2 = r1.withAbstractState(newState);

    Assert.assertSame(newState, r2.abstractState());
    Assert.assertSame(r1.precision(), r2.precision());
    Assert.assertSame(r1.action(), r2.action());

    Assert.assertNotEquals(r1.abstractState(), r2.abstractState());
    Assert.assertNotEquals(r1, r2);
  }

  @Test
  public void testWithPrecision() {
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
        mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    Precision newPrecision = mock(Precision.class);
    PrecisionAdjustmentResult r2 = r1.withPrecision(newPrecision);

    Assert.assertSame(r1.abstractState(), r2.abstractState());
    Assert.assertSame(newPrecision, r2.precision());
    Assert.assertSame(r1.action(), r2.action());

    Assert.assertNotEquals(r1.precision(), r2.precision());
    Assert.assertNotEquals(r1, r2);
  }

  @Test
  public void testWithAction() {
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
        mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    Action newAction = Action.BREAK;
    PrecisionAdjustmentResult r2 = r1.withAction(newAction);

    Assert.assertSame(r1.abstractState(), r2.abstractState());
    Assert.assertSame(r1.precision(), r2.precision());
    Assert.assertSame(newAction, r2.action());

    Assert.assertNotEquals(r1.action(), r2.action());
    Assert.assertNotEquals(r1, r2);
  }
}
