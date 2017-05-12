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
package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

import java.math.BigInteger;


public class ISIOperatorTest {

  @Test
  public void testModulo() {
    BigInteger scalarFour = BigInteger.valueOf(4);
    BigInteger scalarFive = BigInteger.valueOf(5);
    SimpleInterval zeroToFour = SimpleInterval.of(BigInteger.ZERO, scalarFour);
    SimpleInterval zeroToInf = SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();
    assertEquals(zeroToFour.negate(), ISIOperator.MODULO.apply(zeroToInf.negate(), scalarFive));
    assertEquals(zeroToFour.negate(),
        ISIOperator.MODULO.apply(zeroToInf.negate(), scalarFive.negate()));
  }

  @Test
  public void testShiftLeft() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval ten = SimpleInterval.singleton(BigInteger.TEN);
    SimpleInterval zeroToTen = SimpleInterval.span(zero, ten);
    SimpleInterval zeroToFive = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(5));
    SimpleInterval two = SimpleInterval.singleton(BigInteger.valueOf(2));
    SimpleInterval oneThousandTwentyFour = SimpleInterval.singleton(BigInteger.valueOf(1024));
    assertEquals(zero, ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.ZERO));
    assertEquals(zero, ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.ONE));
    assertEquals(zero, ISIOperator.SHIFT_LEFT.apply(zero, BigInteger.TEN));
    assertEquals(one, ISIOperator.SHIFT_LEFT.apply(one, BigInteger.ZERO));
    assertEquals(two, ISIOperator.SHIFT_LEFT.apply(one, BigInteger.ONE));
    assertEquals(oneThousandTwentyFour, ISIOperator.SHIFT_LEFT.apply(one, BigInteger.TEN));
    assertEquals(ten, ISIOperator.SHIFT_LEFT.apply(ten, BigInteger.ZERO));
    assertEquals(zeroToTen, ISIOperator.SHIFT_LEFT.apply(zeroToFive, BigInteger.ONE));
  }

  @Test
  public void testShiftRight() {
    SimpleInterval zero = SimpleInterval.singleton(BigInteger.ZERO);
    SimpleInterval one = SimpleInterval.singleton(BigInteger.ONE);
    SimpleInterval ten = SimpleInterval.singleton(BigInteger.TEN);
    SimpleInterval oneToTen = SimpleInterval.span(one, ten);
    SimpleInterval zeroToFive = SimpleInterval.of(BigInteger.ZERO, BigInteger.valueOf(5));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.ZERO));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.ONE));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(zero, BigInteger.TEN));
    assertEquals(one, ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.ZERO));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.ONE));
    assertEquals(zero, ISIOperator.SHIFT_RIGHT.apply(one, BigInteger.TEN));
    assertEquals(ten, ISIOperator.SHIFT_RIGHT.apply(ten, BigInteger.ZERO));
    assertEquals(zeroToFive, ISIOperator.SHIFT_RIGHT.apply(oneToTen, BigInteger.ONE));
  }

}
