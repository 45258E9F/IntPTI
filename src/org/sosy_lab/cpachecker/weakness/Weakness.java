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
package org.sosy_lab.cpachecker.weakness;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;

public enum Weakness {

  INTEGER_OVERFLOW("INTEGER_OVERFLOW", Lists.newArrayList(190, 191)),
  INTEGER_CONVERSION("INTEGER_CONVERSION_ERROR", Lists.newArrayList(194, 195, 196, 197)),
  DIVIDED_BY_ZERO("DIV_ZERO", Lists.newArrayList(369)),
  DEAD_CODE("DEAD_CODE", Lists.newArrayList(561)),
  ALWAYS_TRUE("ALWAYS_TRUE", Lists.newArrayList(571)),
  ALWAYS_FALSE("ALWAYS_FALSE", Lists.newArrayList(570)),
  UNUSED_VARIABLE("UNUSED_VAR", Lists.newArrayList(563)),
  INVALID_FREE("INVALID_FREE", Lists.newArrayList(590, 415, 761, 690)),
  INVALID_READ("INVALID_READ", Lists.newArrayList(476, 126, 127, 416)),
  INVALID_WRITE("INVALID_WRITE", Lists.newArrayList(121, 122, 124)),
  MEMORY_LEAK("MEMORY_LEAK", Lists.newArrayList(401, 775)),
  STACK_ADDRESS_RETURN("STACK_ADDRESS_RETURN", Lists.newArrayList(562));

  private final String weaknessName;
  private final Collection<Integer> CWENumber;

  Weakness(
      String pWeaknessName,
      Collection<Integer> pCWENumber) {
    weaknessName = pWeaknessName;
    CWENumber = pCWENumber;
  }

  public String getWeaknessName() {
    return weaknessName;
  }

  public Collection<Integer> getCWENumber() {
    return Collections.unmodifiableCollection(CWENumber);
  }

}
