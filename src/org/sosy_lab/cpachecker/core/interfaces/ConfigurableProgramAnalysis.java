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

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Interface for classes representing a Configurable Program Analysis.
 *
 * All instances of this class have to have a public static method "factory()"
 * which takes no arguments, returns an instance of {@link CPAFactory} and never
 * fails that is, it never returns null or throws an exception).
 */
public interface ConfigurableProgramAnalysis {

  AbstractDomain getAbstractDomain();

  TransferRelation getTransferRelation();

  MergeOperator getMergeOperator();

  StopOperator getStopOperator();

  PrecisionAdjustment getPrecisionAdjustment();

  AbstractState getInitialState(CFANode node, StateSpacePartition partition);

  Precision getInitialPrecision(CFANode node, StateSpacePartition partition);

}
