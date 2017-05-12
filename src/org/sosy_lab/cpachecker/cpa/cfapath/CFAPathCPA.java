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
package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class CFAPathCPA implements ConfigurableProgramAnalysis {

  private final CFAPathDomain mDomain;
  private final CFAPathTransferRelation mTransferRelation;
  private final PrecisionAdjustment mPrecisionAdjustment;
  private final Precision mPrecision;
  private final CFAPathStandardState mInitialState;
  private final StopOperator mStopOperator;
  private final MergeOperator mMergeOperator;

  private static final CFAPathCPA sInstance = new CFAPathCPA();

  public static CFAPathCPA getInstance() {
    return sInstance;
  }

  public CFAPathCPA() {
    mDomain = CFAPathDomain.getInstance();
    mTransferRelation = new CFAPathTransferRelation();
    mPrecisionAdjustment = StaticPrecisionAdjustment.getInstance();
    mPrecision = SingletonPrecision.getInstance();
    mInitialState = CFAPathStandardState.getEmptyPath();
    mStopOperator = StopNeverOperator.getInstance();
    mMergeOperator = MergeSepOperator.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return mInitialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return mPrecision;
  }

}
