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
package org.sosy_lab.cpachecker.cpa.smg.objects.generic;

import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;

public class SMGUnknownAbstractionCandidate implements SMGGenericAbstractionCandidate {

  private final static SMGUnknownAbstractionCandidate INSTANCE =
      new SMGUnknownAbstractionCandidate();

  private SMGUnknownAbstractionCandidate() {
  }

  @Override
  public int compareTo(SMGGenericAbstractionCandidate other) {
    return getScore() - other.getScore();
  }

  @Override
  public int getScore() {
    return 0;
  }

  @Override
  public SMG execute(SMG pSMG) {
    throw new UnsupportedOperationException("Unknown abstraction cannot be executed");
  }

  @Override
  public boolean isUnknown() {
    return true;
  }

  public static SMGUnknownAbstractionCandidate getInstance() {
    return INSTANCE;
  }

}
