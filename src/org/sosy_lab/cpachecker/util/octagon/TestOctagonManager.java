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
package org.sosy_lab.cpachecker.util.octagon;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestOctagonManager {

  static OctagonManager manager;

  @BeforeClass
  public static void setUpBeforeClass() {
    manager = new OctagonFloatManager();
  }

  @Test
  public void testNum_Int() {
    NumArray num = manager.init_num_t(1);
    manager.num_set_int(num, 0, 3);
    Assert.assertFalse(manager.num_infty(num, 0));
    Assert.assertEquals(3, manager.num_get_int(num, 0));
    Assert.assertEquals(3, manager.num_get_float(num, 0), 0);
  }

  @Test
  public void testNum_Float() {
    NumArray num = manager.init_num_t(1);
    manager.num_set_float(num, 0, 3.3);
    Assert.assertFalse(manager.num_infty(num, 0));
    Assert.assertEquals(3, manager.num_get_int(num, 0));
    Assert.assertEquals(3.3, manager.num_get_float(num, 0), 0);
  }

}
