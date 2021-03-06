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
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.AliasCreator.Environment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Set;

/**
 * Less-or-equal operator of the semi-lattice of the {@link ConstraintsCPA}.
 * Allows to check whether one {@link ConstraintsState} is less or equal another one
 * and whether two constraints represent the same meaning.
 *
 * <p>Constraints state <code>c</code> is less or equal <code>c'</code> if a bijective mapping
 * <code>d: SymbolicIdentifier -> SymbolicIdentifier</code> exists so that for each constraint
 * <code>e</code> in <code>c</code> a constraint <code>e'</code> in <code>c'</code> exists that
 * equals <code>e</code> after replacing each symbolic identifier <code>s</code> occurring in it
 * with <code>d(s)</code>.
 * </p>
 */
public class AliasedSubsetLessOrEqualOperator implements AbstractDomain {

  private static final AliasedSubsetLessOrEqualOperator SINGLETON =
      new AliasedSubsetLessOrEqualOperator();

  private final SubsetLessOrEqualOperator simpleSubsetOperator;

  private AliasedSubsetLessOrEqualOperator() {
    simpleSubsetOperator = SubsetLessOrEqualOperator.getInstance();
  }

  public static AliasedSubsetLessOrEqualOperator getInstance() {
    return SINGLETON;
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("ConstraintsCPA's domain does not support join");
  }

  /**
   * Returns whether the first state is less or equal the second state.
   *
   * <p>Constraints state <code>c</code> is less or equal <code>c'</code> if a bijective mapping
   * <code>d: SymbolicIdentifier -> SymbolicIdentifier</code> exists so that for each constraint
   * <code>e</code> in <code>c</code> a constraint <code>e'</code> in <code>c'</code> exists that
   * equals <code>e</code> after replacing each symbolic identifier <code>s</code> occurring in it
   * with <code>d(s)</code>.
   * </p>
   *
   * @param pLesserState the state that should be less or equal the second state
   * @param pBiggerState the state that should be equal or bigger the first state
   * @return <code>true</code> if the first given state is less or equal the given second state
   */
  @Override
  public boolean isLessOrEqual(
      final AbstractState pLesserState,
      final AbstractState pBiggerState
  ) {
    assert pLesserState instanceof ConstraintsState;
    assert pBiggerState instanceof ConstraintsState;

    if (simpleSubsetOperator.isLessOrEqual(pLesserState, pBiggerState)) {
      return true;
    }

    ConstraintsState lesserState = (ConstraintsState) pLesserState;
    ConstraintsState biggerState = (ConstraintsState) pBiggerState;

    if (biggerState.size() > lesserState.size()) {
      return false;
    }

    // we already know that the second state has less constraints or the same amount of constraints
    // as the first state. So if it is empty, the first one has to be empty, too, and they are equal
    if (biggerState.isEmpty()) {
      return true;
    }

    final Set<Environment> possibleScenarios =
        SymbolicValues.getPossibleAliases(lesserState, biggerState);

    return !possibleScenarios.isEmpty();
  }
}
