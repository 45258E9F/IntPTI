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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.greaterOrEqual;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.lessOrEqual;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.of;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.singleton;

import org.junit.Test;

import java.math.BigInteger;

public class SimpleIntervalTest {

  @Test
  public void testConstruction() {
    assertNotNull(singleton(BigInteger.ZERO));
    assertNotNull(singleton(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(singleton(BigInteger.valueOf(Long.MIN_VALUE)));

    assertNotNull(lessOrEqual(BigInteger.ZERO));
    assertNotNull(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)));

    assertNotNull(greaterOrEqual(BigInteger.ZERO));
    assertNotNull(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)));

    assertNotNull(of(BigInteger.ZERO, BigInteger.ZERO));
    assertNotNull(of(BigInteger.ZERO, BigInteger.ONE));
    assertNotNull(of(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConstruction1() {
    of(BigInteger.ONE, BigInteger.ZERO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConstruction2() {
    of(BigInteger.valueOf(Long.MAX_VALUE), BigInteger.valueOf(Long.MIN_VALUE));
  }

  public void testContains() {
    assertTrue(singleton(BigInteger.ZERO).contains(BigInteger.ZERO));
    assertTrue(singleton(BigInteger.TEN).contains(BigInteger.TEN));
    assertFalse(singleton(BigInteger.ZERO).contains(BigInteger.TEN));
    assertFalse(singleton(BigInteger.TEN).contains(BigInteger.ZERO));
    assertTrue(of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.ONE));
    assertTrue(of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.TEN));
    assertTrue(of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.valueOf(5)));
    assertFalse(of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.ZERO));
    assertFalse(of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.valueOf(-5)));
    assertFalse(of(BigInteger.ONE, BigInteger.TEN).contains(BigInteger.valueOf(-10)));
  }

  @Test
  public void testIsSingleton() {
    assertTrue(singleton(BigInteger.ZERO).isSingleton());
    assertTrue(singleton(BigInteger.valueOf(Long.MAX_VALUE)).isSingleton());
    assertTrue(singleton(BigInteger.valueOf(Long.MIN_VALUE)).isSingleton());

    assertFalse(lessOrEqual(BigInteger.ZERO).isSingleton());
    assertFalse(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).isSingleton());
    assertFalse(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).isSingleton());

    assertFalse(greaterOrEqual(BigInteger.ZERO).isSingleton());
    assertFalse(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).isSingleton());
    assertFalse(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).isSingleton());

    assertTrue(of(BigInteger.ZERO, BigInteger.ZERO).isSingleton());
    assertFalse(of(BigInteger.ZERO, BigInteger.ONE).isSingleton());
    assertFalse(
        of(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)).isSingleton());
  }

  @Test
  public void testSize() {
    assertEquals(BigInteger.ONE, singleton(BigInteger.ZERO).size());
    assertEquals(BigInteger.ONE, singleton(BigInteger.valueOf(Long.MAX_VALUE)).size());
    assertEquals(BigInteger.ONE, singleton(BigInteger.valueOf(Long.MIN_VALUE)).size());

    assertNull(lessOrEqual(BigInteger.ZERO).size());
    assertNull(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).size());
    assertNull(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).size());

    assertNull(greaterOrEqual(BigInteger.ZERO).size());
    assertNull(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)).size());
    assertNull(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)).size());

    assertEquals(BigInteger.ONE, of(BigInteger.ZERO, BigInteger.ZERO).size());
    assertEquals(BigInteger.valueOf(2L), of(BigInteger.ZERO, BigInteger.ONE).size());
    assertEquals(BigInteger.TEN, of(BigInteger.ONE, BigInteger.TEN).size());

    assertEquals(BigInteger.valueOf(201L),
        of(BigInteger.valueOf(-100L), BigInteger.valueOf(100L)).size());

    assertEquals(BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.valueOf(Long.MIN_VALUE))
            .add(BigInteger.ONE),
        of(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)).size());

  }

  @Test
  public void testIntersectsWith() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval two = SimpleInterval.singleton(BigInteger.valueOf(2));
    SimpleInterval negFiveToTen = SimpleInterval.of(BigInteger.valueOf(-5), BigInteger.TEN);
    SimpleInterval fiveToFifteen = SimpleInterval.of(BigInteger.valueOf(5), BigInteger.valueOf(15));
    SimpleInterval twentyToFifty =
        SimpleInterval.of(BigInteger.valueOf(20), BigInteger.valueOf(50));
    SimpleInterval oneToThousand = SimpleInterval.of(BigInteger.ONE, BigInteger.valueOf(1000));
    assertFalse(zero.intersectsWith(one));
    assertFalse(one.intersectsWith(zero));
    assertTrue(zero.intersectsWith(zero));
    assertTrue(one.intersectsWith(one));
    assertTrue(zero.extendToNegativeInfinity().intersectsWith(zero.extendToPositiveInfinity()));
    assertTrue(one.extendToNegativeInfinity().intersectsWith(one.extendToPositiveInfinity()));
    assertFalse(zero.extendToNegativeInfinity().intersectsWith(one.extendToPositiveInfinity()));
    assertFalse(one.extendToPositiveInfinity().intersectsWith(zero.extendToNegativeInfinity()));
    assertTrue(one.extendToNegativeInfinity().intersectsWith(zero.extendToPositiveInfinity()));
    assertTrue(zero.extendToPositiveInfinity().intersectsWith(one.extendToNegativeInfinity()));
    assertTrue(negFiveToTen.intersectsWith(fiveToFifteen));
    assertFalse(negFiveToTen.intersectsWith(twentyToFifty));
    assertFalse(fiveToFifteen.intersectsWith(twentyToFifty));
    assertTrue(oneToThousand.intersectsWith(two));
  }
}
