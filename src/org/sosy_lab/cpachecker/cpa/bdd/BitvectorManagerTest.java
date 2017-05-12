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
package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

import java.math.BigInteger;

public class BitvectorManagerTest {

  private LogManager logger;

  private RegionManager rmgr;
  private BitvectorManager bvmgr;
  private Region[] zero;
  private Region[] one;
  private Region[] two;
  private Region[] n15;


  @Before
  public void init() throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();
    logger = TestLogManager.getInstance();

    rmgr = new BDDManagerFactory(config, logger).createRegionManager();
    bvmgr = new BitvectorManager(rmgr);

    zero = bvmgr.makeNumber(BigInteger.ZERO, 4);
    one = bvmgr.makeNumber(BigInteger.ONE, 4);
    two = bvmgr.makeNumber(BigInteger.valueOf(2), 4);
    n15 = bvmgr.makeNumber(BigInteger.valueOf(15), 4);
  }

  private void assertIsTrue(Region r) {
    Assert.assertTrue(r.isTrue());
  }

  private void assertIsFalse(Region r) {
    Assert.assertTrue(r.isFalse());

  }

  private void assertEqual(Region[] r1, Region[] r2) {
    Assert.assertTrue(r1.length == r2.length);
    Assert.assertArrayEquals(r1, r2);
  }

  private void assertDistinct(Region[] r1, Region[] r2) {
    Assert.assertTrue(r1.length == r2.length);
    boolean distinct = false;
    for (int i = 0; i < r1.length; i++) {
      distinct |= r1[i].equals(r2[i]);
    }
    Assert.assertTrue(distinct);
  }

  @Test
  public void selfTest() {
    assertDistinct(zero, one);
    assertDistinct(n15, one);

    for (int i = 0; i < 4; i++) {
      assertIsFalse(zero[i]);
    }
    assertIsTrue(one[0]);
    for (int i = 1; i < 4; i++) {
      assertIsFalse(one[i]);
    }
  }

  @Test(expected = AssertionError.class)
  public void differentLen() {
    bvmgr.makeAdd(bvmgr.makeNumber(BigInteger.ONE, 3), bvmgr.makeNumber(BigInteger.ONE, 4));
  }

  @Test
  public void addsub() {
    assertEqual(zero, bvmgr.makeAdd(zero, zero));
    assertEqual(one, bvmgr.makeAdd(zero, one));
    assertEqual(two, bvmgr.makeAdd(one, one));
    assertEqual(two, bvmgr.makeAdd(two, zero));

    assertEqual(one, bvmgr.makeSub(one, zero));
    assertEqual(two, bvmgr.makeSub(two, zero));
    assertEqual(n15, bvmgr.makeSub(n15, zero));

    assertEqual(zero, bvmgr.makeSub(zero, zero));
    assertEqual(zero, bvmgr.makeSub(one, one));
    assertEqual(zero, bvmgr.makeSub(n15, n15));

    assertEqual(two, bvmgr.makeSub(two, zero));
    assertEqual(n15, bvmgr.makeSub(n15, zero));

    for (Region[] n : Lists.newArrayList(zero, one, two, n15)) {
      assertEqual(zero, bvmgr.makeSub(bvmgr.makeAdd(zero, n), n));
      assertEqual(one, bvmgr.makeSub(bvmgr.makeAdd(one, n), n));
      assertEqual(n15, bvmgr.makeSub(bvmgr.makeAdd(n15, n), n));

      assertEqual(zero, bvmgr.makeSub(bvmgr.makeAdd(zero, n), n));
      assertEqual(one, bvmgr.makeSub(bvmgr.makeAdd(one, n), n));
      assertEqual(n15, bvmgr.makeSub(bvmgr.makeAdd(n15, n), n));

      assertEqual(zero, bvmgr.makeAdd(bvmgr.makeSub(zero, n), n));
      assertEqual(one, bvmgr.makeAdd(bvmgr.makeSub(one, n), n));
      assertEqual(n15, bvmgr.makeAdd(bvmgr.makeSub(n15, n), n));
    }
  }

  @Test
  public void bool() {
    assertIsFalse(bvmgr.makeNot(one));
    assertIsFalse(bvmgr.makeNot(two));
    assertIsTrue(bvmgr.makeNot(zero));

    assertEqual(zero, bvmgr.makeBinaryAnd(one, two));
    assertEqual(zero, bvmgr.makeBinaryAnd(one, zero));
    assertEqual(one, bvmgr.makeBinaryAnd(one, one));
    assertEqual(one, bvmgr.makeBinaryAnd(one, bvmgr.makeAdd(one, two)));

    assertIsTrue(bvmgr.makeLogicalEqual(one, one));
    assertIsTrue(bvmgr.makeLogicalEqual(two, two));
    assertEqual(bvmgr.makeBinaryEqual(one, one), bvmgr.makeSub(zero, one));
    assertEqual(bvmgr.makeBinaryEqual(zero, zero), bvmgr.makeSub(one, two));
    assertEqual(bvmgr.makeXor(one, two), bvmgr.makeAdd(one, two));
    assertEqual(bvmgr.makeXor(one, zero), one);
  }

  @Test
  public void signedLen4() {
    assertEqual(zero, bvmgr.makeAdd(n15, one));
    assertEqual(one, bvmgr.makeAdd(n15, two));

    assertEqual(n15, bvmgr.makeSub(zero, one));
    assertEqual(n15, bvmgr.makeSub(one, two));

    Region[] sum = one;
    for (int i = 0; i < 4; i++) {
      sum = bvmgr.makeAdd(sum, sum);
    }
    assertEqual(zero, sum); // 0 == 16 == 1*2*2*2*2
  }

  @Test
  public void lessSigned() {
    assertIsFalse(bvmgr.makeLess(one, zero, true));
    assertIsTrue(bvmgr.makeLess(zero, one, true));
    assertIsFalse(bvmgr.makeLess(zero, zero, true));
    assertIsFalse(bvmgr.makeLess(two, two, true));

    assertIsTrue(bvmgr.makeLess(n15, zero, true));
    assertIsFalse(bvmgr.makeLess(zero, n15, true));
    assertIsFalse(bvmgr.makeLess(n15, n15, true));
  }

  @Test
  public void lessUnsigned() {
    assertIsFalse(bvmgr.makeLess(one, zero, false));
    assertIsTrue(bvmgr.makeLess(zero, one, false));
    assertIsFalse(bvmgr.makeLess(zero, zero, false));
    assertIsFalse(bvmgr.makeLess(two, two, false));

    assertIsFalse(bvmgr.makeLess(n15, zero, false));
    assertIsTrue(bvmgr.makeLess(zero, n15, false));
    assertIsFalse(bvmgr.makeLess(n15, n15, false));
  }

  @Test
  public void lessOrEqualSigned() {
    assertIsFalse(bvmgr.makeLessOrEqual(one, zero, true));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, one, true));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, zero, true));
    assertIsTrue(bvmgr.makeLessOrEqual(two, two, true));

    assertIsTrue(bvmgr.makeLessOrEqual(n15, zero, true));
    assertIsFalse(bvmgr.makeLessOrEqual(zero, n15, true));
    assertIsTrue(bvmgr.makeLessOrEqual(n15, n15, true));
  }

  @Test
  public void lessOrEqualUnsigned() {
    assertIsFalse(bvmgr.makeLessOrEqual(one, zero, false));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, one, false));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, zero, false));
    assertIsTrue(bvmgr.makeLessOrEqual(two, two, false));

    assertIsFalse(bvmgr.makeLessOrEqual(n15, zero, false));
    assertIsTrue(bvmgr.makeLessOrEqual(zero, n15, false));
    assertIsTrue(bvmgr.makeLessOrEqual(n15, n15, false));
  }
}
