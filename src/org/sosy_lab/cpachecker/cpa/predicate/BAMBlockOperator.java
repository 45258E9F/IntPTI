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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

@Options
public class BAMBlockOperator extends BlockOperator {

  private BlockPartitioning partitioning = null;

  void setPartitioning(BlockPartitioning pPartitioning) {
    checkState(partitioning == null);
    partitioning = pPartitioning;
  }

  /**
   * @see{@link BlockOperator#isBlockEnd}
   */
  @Override
  public boolean isBlockEnd(CFANode loc, int thresholdValue) {
    return super.isBlockEnd(loc, thresholdValue)
        || partitioning.isCallNode(loc)
        || partitioning.isReturnNode(loc);
  }

  @Override
  public boolean alwaysReturnsFalse() {
    return super.alwaysReturnsFalse()
        && partitioning.getBlocks().isEmpty();
  }

  public BlockPartitioning getPartitioning() {
    checkState(partitioning != null);
    return partitioning;
  }
}
