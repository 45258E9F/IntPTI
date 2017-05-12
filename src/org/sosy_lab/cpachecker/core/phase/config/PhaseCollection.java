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
package org.sosy_lab.cpachecker.core.phase.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PhaseCollection implements Iterable<PhaseInfo> {

  private final Set<PhaseInfo> phaseInfoSet;

  public PhaseCollection() {
    phaseInfoSet = new HashSet<>();
  }

  public void addPhaseInfo(PhaseInfo pInfo) {
    // overwrite mode
    if (!phaseInfoSet.add(pInfo)) {
      phaseInfoSet.remove(pInfo);
      phaseInfoSet.add(pInfo);
    }
  }

  public boolean contains(String phaseId) {
    PhaseInfo forCompare = new PhaseInfo(phaseId);
    if (phaseInfoSet.contains(forCompare)) {
      return true;
    }
    return false;
  }

  @Override
  public Iterator<PhaseInfo> iterator() {
    return phaseInfoSet.iterator();
  }
}
