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
package org.sosy_lab.cpachecker.util.predicates;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model.ValueAssignment;

import java.util.List;

public class AssignmentToPathAllocatorTest {

  private AssignmentToPathAllocator allocator;

  @Before
  public void setUp() throws InvalidConfigurationException {
    this.allocator =
        new AssignmentToPathAllocator(
            Configuration.defaultConfiguration(),
            ShutdownNotifier.createDummy(),
            TestLogManager.getInstance(),
            MachineModel.LINUX32);
  }

  @Test
  public void testFindFirstOccurrenceOfVariable() {
    ValueAssignment varX = new ValueAssignment(mock(Formula.class), "x@4", 1, ImmutableList.of());
    ValueAssignment varY = new ValueAssignment(mock(Formula.class), "y@5", 1, ImmutableList.of());
    ValueAssignment varZ = new ValueAssignment(mock(Formula.class), "z@6", 1, ImmutableList.of());

    SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
    List<SSAMap> ssaMaps = Lists.newArrayList();

    ssaMaps.add(SSAMap.emptySSAMap());

    ssaMapBuilder.setIndex("x", CNumericTypes.INT, 4);
    ssaMaps.add(ssaMapBuilder.build());

    ssaMapBuilder.setIndex("y", CNumericTypes.INT, 5);
    ssaMapBuilder.setIndex("z", CNumericTypes.INT, 6);
    ssaMaps.add(ssaMapBuilder.build());

    ssaMapBuilder.deleteVariable("z");
    ssaMaps.add(ssaMapBuilder.build());

    assertEquals(1, allocator.findFirstOccurrenceOfVariable(varX, ssaMaps));
    assertEquals(2, allocator.findFirstOccurrenceOfVariable(varY, ssaMaps));
    assertEquals(2, allocator.findFirstOccurrenceOfVariable(varZ, ssaMaps));
  }

}
