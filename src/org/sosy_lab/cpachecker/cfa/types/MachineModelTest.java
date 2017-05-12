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
package org.sosy_lab.cpachecker.cfa.types;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class MachineModelTest {

  @Parameters(name = "{4}: {0}")
  public static Object[][] machineModels() {
    Object[][] types = new Object[][]{
        // type          // size in bits // min value // max value
        {CNumericTypes.BOOL, 8, 0L, 1L},
        {CNumericTypes.CHAR, 8, -128L, 127L},
        {CNumericTypes.SIGNED_CHAR, 8, -128L, 127L},
        {CNumericTypes.UNSIGNED_CHAR, 8, 0L, 255L},
        {CNumericTypes.SHORT_INT, 16, -32768L, 32767L},
        {CNumericTypes.INT, 32, -2147483648L, 2147483647L},
        {CNumericTypes.SIGNED_INT, 32, -2147483648L, 2147483647L},
        {CNumericTypes.UNSIGNED_INT, 32, 0L, 4294967295L},
    };

    // Create a copy of types for each MachineModel and append the MachineModel instance in each row
    MachineModel[] machineModels = MachineModel.values();
    Object[][] result = new Object[machineModels.length * types.length][];
    for (int m = 0; m < machineModels.length; m++) {
      int offset = m * types.length;
      for (int t = 0; t < types.length; t++) {
        result[offset + t] = Arrays.copyOf(types[t], types[t].length + 1);
        result[offset + t][types[t].length] = machineModels[m];
      }
    }

    return result;
  }

  @Parameter(0)
  public CSimpleType type;

  @Parameter(1)
  public int sizeInBits;

  @Parameter(2)
  public long minValue;

  @Parameter(3)
  public long maxValue;

  @Parameter(4)
  public MachineModel machineModel;

  @Test
  public void testSizeOfInBits() {
    assertEquals(sizeInBits, machineModel.getSizeofInBits(type));
  }

  @Test
  public void testMinimalValue() {
    assertEquals(minValue, machineModel.getMinimalIntegerValue(type).longValue());
  }

  @Test
  public void testMaximalValue() {
    assertEquals(maxValue, machineModel.getMaximalIntegerValue(type).longValue());
  }
}