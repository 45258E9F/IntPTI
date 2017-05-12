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
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

import java.util.Iterator;

/**
 * Unit tests for {@link ConstraintsState}
 */
public class ConstraintsStateTest {

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  private final Type defType = CNumericTypes.INT;

  private final SymbolicIdentifier id1 = factory.newIdentifier();
  private final SymbolicExpression idExp1 = factory.asConstant(id1, defType);
  private final SymbolicExpression numExp = factory.asConstant(new NumericValue(5), defType);

  private final Constraint constr1 = factory.equal(idExp1, numExp, defType, defType);
  private final Constraint constr2 =
      (Constraint) factory.lessThan(idExp1, numExp, defType, defType);
  private final Constraint constr3 = (Constraint) factory.lessThanOrEqual(idExp1, numExp, defType,
      defType);

  private ConstraintsState state;

  @Before
  public void setUp() {
    state = new ConstraintsState();

    state.add(constr1);
    state.add(constr2);
    state.add(constr3);
  }

  @Test
  public void testIterator() {
    Iterator<Constraint> it = state.iterator();

    it.next();
    it.remove();
    Assert.assertTrue(state.size() == 2);
    Assert.assertTrue(it.hasNext());
    Assert.assertTrue(state.contains(constr2));
    Assert.assertTrue(state.contains(constr3));

    it.next();
    it.remove();
    Assert.assertTrue(it.hasNext());
    Assert.assertTrue(state.size() == 1);
    Assert.assertTrue(state.contains(constr3));

    it.next();
    it.remove();
    Assert.assertFalse(it.hasNext());
    Assert.assertTrue(state.isEmpty());
  }
}
