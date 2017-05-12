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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;

/**
 * This Cpa can generate a concrete counterexample from an {@link ARGPath} path.
 */
public interface ConfigurableProgramAnalysisWithConcreteCex {

  /**
   * Creates a {@link ConcreteStatePath} path, that contain the concrete values of the given
   * variables along the given {@link ARGPath}. The {@link ConcreteStatePath} path is used to
   * calculate the concrete values of the variables along the generated counterexample path.
   *
   * @param path An {@link ARGPath} counterexample path, generated from the {@link ARGCPA}. The
   *             concrete values of variables along this path should be calculated.
   * @return A {@link ConcreteStatePath} path along the {@link CFAEdge} edges of the {@link ARGPath}
   * path that contain concrete values for the variables along the path.
   */
  public ConcreteStatePath createConcreteStatePath(ARGPath path);

}
