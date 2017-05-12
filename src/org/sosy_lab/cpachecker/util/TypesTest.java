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
package org.sosy_lab.cpachecker.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

/**
 * Unit tests for {@link Types}.
 */
public class TypesTest {

  MachineModel machineModel = MachineModel.LINUX32;

  @Test
  public void testCanHoldAllValues_CTypes() {
    assertTrue(shortHoldsChar());
    assertTrue(intHoldsShort());
    assertTrue(doubleHoldsInt());
    assertTrue(doubleHoldsFloat());
    assertFalse(intHoldsFloat());
    assertFalse(charHoldsLongLong());

    assertTrue(signedIntHoldsUnsignedShort());
    assertFalse(unsignedHoldsSignedInt());
    assertFalse(unsignedCharHoldsInt());
  }

  private boolean shortHoldsChar() {
    return Types.canHoldAllValues(CNumericTypes.SHORT_INT, CNumericTypes.CHAR, machineModel);
  }

  private boolean intHoldsShort() {
    return Types.canHoldAllValues(CNumericTypes.INT, CNumericTypes.SHORT_INT, machineModel);
  }

  private boolean doubleHoldsInt() {
    return Types.canHoldAllValues(CNumericTypes.DOUBLE, CNumericTypes.INT, machineModel);
  }

  private boolean doubleHoldsFloat() {
    return Types.canHoldAllValues(CNumericTypes.DOUBLE, CNumericTypes.FLOAT, machineModel);
  }

  private boolean intHoldsFloat() {
    return Types.canHoldAllValues(CNumericTypes.SIGNED_INT, CNumericTypes.FLOAT, machineModel);
  }

  private boolean charHoldsLongLong() {
    return Types.canHoldAllValues(CNumericTypes.CHAR, CNumericTypes.LONG_LONG_INT, machineModel);
  }

  private boolean signedIntHoldsUnsignedShort() {
    return Types
        .canHoldAllValues(CNumericTypes.SIGNED_INT, CNumericTypes.UNSIGNED_SHORT_INT, machineModel);
  }

  private boolean unsignedHoldsSignedInt() {
    return Types
        .canHoldAllValues(CNumericTypes.UNSIGNED_INT, CNumericTypes.SIGNED_INT, machineModel);
  }

  private boolean unsignedCharHoldsInt() {
    return Types.canHoldAllValues(CNumericTypes.UNSIGNED_CHAR, CNumericTypes.INT, machineModel);
  }
}
