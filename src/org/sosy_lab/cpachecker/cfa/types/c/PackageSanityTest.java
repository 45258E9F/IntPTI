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
package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.testing.AbstractPackageSanityTests;

import org.junit.Ignore;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(ComplexTypeKind.class, ComplexTypeKind.STRUCT);
  }

  @Ignore
  @Override
  public void testEquals() throws Exception {
    // equals methods of CTypes are not testable with PackageSanityTest
    // because of field origName
  }
}
