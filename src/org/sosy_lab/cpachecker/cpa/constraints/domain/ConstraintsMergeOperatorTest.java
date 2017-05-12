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
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

import java.util.HashSet;
import java.util.Set;


/**
 * Unit tests for {@link ConstraintsMergeOperator}
 */
public class ConstraintsMergeOperatorTest {

  private final ConstraintsMergeOperator op = new ConstraintsMergeOperator();
  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
  private final Type defType = CNumericTypes.INT;

  private final SymbolicExpression idExp1 = factory.asConstant(factory.newIdentifier(), defType);
  private final SymbolicExpression numExp1 = factory.asConstant(new NumericValue(1), defType);

  private final Constraint posConst = factory.equal(idExp1, numExp1, defType, defType);
  private final Constraint negConst =
      (Constraint) factory.notEqual(idExp1, numExp1, defType, defType);

  @Test
  public void testMerge_mergePossible() throws Exception {
    Set<Constraint> constraints = getConstraints();

    ConstraintsState state1 = new ConstraintsState(constraints, new IdentifierAssignment());
    state1.add(posConst);

    constraints = getConstraints();

    ConstraintsState state2 = new ConstraintsState(constraints, new IdentifierAssignment());
    state2.add(negConst);

    ConstraintsState mergeResult =
        (ConstraintsState) op.merge(state1, state2, SingletonPrecision.getInstance());

    Assert.assertTrue(mergeResult.size() + 1 == state2.size());
    Assert.assertTrue(!mergeResult.contains(negConst));

    state2.remove(negConst);
    Assert.assertEquals(state2, mergeResult);
  }

  private Set<Constraint> getConstraints() {
    Set<Constraint> constraints = new HashSet<>();

    // this results in a new symbolic identifier at every method call
    SymbolicExpression idExp2 = factory.asConstant(factory.newIdentifier(), defType);

    Constraint currConstr = (Constraint) factory.greaterThan(idExp2, numExp1, defType, defType);
    constraints.add(currConstr);

    currConstr = (Constraint)
        factory.logicalNot(factory.lessThanOrEqual(idExp2, numExp1, defType, defType), defType);

    constraints.add(currConstr);

    return constraints;
  }
}