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
package org.sosy_lab.cpachecker.cpa.partitioning;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.CPAchecker.InitialStatesFor;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class PartitioningCPATest {

  private PartitioningCPA cpa;
  private AbstractDomain domain;

  @Before
  public void setUp() {
    cpa = new PartitioningCPA();
    domain = cpa.getAbstractDomain();
  }

  @Test
  public void testIsLessOrEqual_EqualPartition() throws CPAException, InterruptedException {
    AbstractState p1 = cpa.getInitialState(
        TestDataTools.DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 = cpa.getInitialState(
        TestDataTools.DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    assertThat(p1).isEqualTo(p2);

    assertThat(domain.isLessOrEqual(p1, p2)).isTrue();
  }

  @Test
  public void testMerge_EqualPartition() throws CPAException, InterruptedException {
    AbstractState p1 = cpa.getInitialState(
        TestDataTools.DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 = cpa.getInitialState(
        TestDataTools.DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState mergeResult =
        cpa.getMergeOperator().merge(p1, p2, SingletonPrecision.getInstance());

    assertThat(mergeResult).isEqualTo(p2); // MERGE-SEP
  }

  @Test
  public void testIsLessOrEqual_DifferentPartitions() throws CPAException, InterruptedException {
    AbstractState p1 = cpa.getInitialState(
        TestDataTools.DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 = cpa.getInitialState(
        TestDataTools.DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.EXIT));

    assertThat(p1).isNotEqualTo(p2);

    assertThat(domain.isLessOrEqual(p1, p2)).isFalse();
  }

}
