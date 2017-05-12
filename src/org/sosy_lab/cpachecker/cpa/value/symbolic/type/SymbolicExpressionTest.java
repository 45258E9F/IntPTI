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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

/**
 * Unit tests for {@link SymbolicExpression} sub types.
 */
public class SymbolicExpressionTest {

  private static final CType OP_TYPE = CNumericTypes.INT;
  private static final CType POINTER_TYPE = CNumericTypes.UNSIGNED_INT;
  private static final CType PROMOTED_OP_TYPE = CNumericTypes.INT;

  private static final ConstantSymbolicExpression CONSTANT_OP1
      = new ConstantSymbolicExpression(new NumericValue(1), OP_TYPE);

  private static final ConstantSymbolicExpression CONSTANT_OP2
      = new ConstantSymbolicExpression(new NumericValue(5), OP_TYPE);

  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  @Test
  public void testEquals_BinarySymbolicExpression() {
    AdditionExpression add1 = new AdditionExpression(CONSTANT_OP1,
        CONSTANT_OP2,
        PROMOTED_OP_TYPE,
        PROMOTED_OP_TYPE);
    AdditionExpression add2 = new AdditionExpression(CONSTANT_OP1,
        CONSTANT_OP2,
        PROMOTED_OP_TYPE,
        PROMOTED_OP_TYPE);
    SubtractionExpression sub1 = new SubtractionExpression(CONSTANT_OP1,
        CONSTANT_OP2,
        PROMOTED_OP_TYPE,
        PROMOTED_OP_TYPE);

    Assert.assertTrue(add1.equals(add2));
    Assert.assertFalse(add1.equals(sub1));
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  @Test
  public void testEquals_UnarySymbolicExpression() {
    NegationExpression neg1 = new NegationExpression(CONSTANT_OP1, POINTER_TYPE);
    NegationExpression neg2 = new NegationExpression(CONSTANT_OP1, POINTER_TYPE);
    PointerExpression ptr = new PointerExpression(CONSTANT_OP1, POINTER_TYPE);

    Assert.assertTrue(neg1.equals(neg2));
    Assert.assertFalse(neg1.equals(ptr));
  }
}
