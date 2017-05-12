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
package org.sosy_lab.cpachecker.core.summary.instance.access;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.summary.apply.AbstractLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.apply.ApplicableExternalLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.apply.ApplicableInternalLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

public abstract class AccessLoopInstance extends AbstractLoopSummaryInstance
    implements ApplicableExternalLoopSummaryInstance<AccessResult>,
               ApplicableInternalLoopSummaryInstance<AccessResult> {

  PathCopyingPersistentTree<String, Presence> readTree = null;
  PathCopyingPersistentTree<String, Presence> writeTree = null;

  public AccessLoopInstance(
      Loop pLoop, PathCopyingPersistentTree<String, Presence> r,
      PathCopyingPersistentTree<String, Presence> w) {
    super(pLoop);
    readTree = r;
    writeTree = w;
  }

  @Override
  public AccessResult apply(CFAEdge pEntering, CFAEdge pLeaving) {
    return AccessInstanceUtil.getInstance().buildResult(readTree, writeTree);
  }

  @Override
  public AccessResult apply(CFAEdge entering) {
    return AccessInstanceUtil.getInstance().buildResult(readTree, writeTree);
  }


  @Override
  public boolean isEqualTo(SummaryInstance pThat) {
    if (this == pThat) {
      return true;
    }
    if (pThat == null || !(pThat instanceof AccessLoopInstance)) {
      return false;
    }
    AccessLoopInstance that = (AccessLoopInstance) pThat;
    if (!that.loop.equals(loop)) {
      return false;
    }
    return AccessInstanceUtil.getInstance().isEqual(this, that);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "AccessLoopInstance loop" + loop.toString() + "\n" + getSummaryName() + "\n" +
        "[readTree=" + readTree + ", writeTree=" +
        writeTree + "]";
  }

  public abstract SummaryName getSummaryName();

}
