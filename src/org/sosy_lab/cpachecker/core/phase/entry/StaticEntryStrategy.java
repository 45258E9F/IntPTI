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

import java.util.Collection;

/**
 * Static entry selection strategy derives initial analysis point based on syntactic information
 * of program (i.e. CFA).
 */
public interface StaticEntryStrategy {

  Collection<CFANode> getInitialEntry(CFA pCFA);

}
