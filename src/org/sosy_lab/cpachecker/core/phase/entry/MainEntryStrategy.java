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
package org.sosy_lab.cpachecker.core.phase.entry;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MainEntryStrategy implements StaticEntryStrategy {

  @Override
  public Collection<CFANode> getInitialEntry(CFA pCFA) {
    List<CFANode> entry = new ArrayList<>();
    entry.add(pCFA.getMainFunction());
    return Collections.unmodifiableCollection(entry);
  }
}
