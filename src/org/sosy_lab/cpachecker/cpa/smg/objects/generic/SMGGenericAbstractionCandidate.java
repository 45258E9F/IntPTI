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

public interface SMGGenericAbstractionCandidate extends Comparable<SMGGenericAbstractionCandidate> {

  public int getScore();

  public SMG execute(SMG pSMG);

  public boolean isUnknown();
}