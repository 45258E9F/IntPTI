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
package org.sosy_lab.cpachecker.core.summary.instance.range;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.summary.apply.AbstractLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.apply.ApplicableExternalLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Map;

public class RangeExternalLoopInstance extends AbstractLoopSummaryInstance
    implements ApplicableExternalLoopSummaryInstance<RangeState> {

  private Map<CFAEdge, RangeState> loopSummary;

  public RangeExternalLoopInstance(Loop pLoop, Map<CFAEdge, RangeState> pSummary) {
    super(pLoop);
    loopSummary = pSummary;
  }

  @Override
  public RangeState apply(CFAEdge entering, CFAEdge leaving) {
    return loopSummary.get(leaving);
  }

  @Override
  public boolean isEqualTo(SummaryInstance that) {
    if (that == this) {
      return true;
    }
    if (!(that instanceof RangeExternalLoopInstance)) {
      return false;
    }
    RangeExternalLoopInstance other = (RangeExternalLoopInstance) that;
    return Objects.equal(this.loop, other.loop) &&
        Objects.equal(this.loopSummary, other.loopSummary);
  }

  public Map<CFAEdge, RangeState> getLoopSummary() {
    return ImmutableMap.copyOf(loopSummary);
  }

  @Override
  public String toString() {
    return loopSummary.toString();
  }
}
