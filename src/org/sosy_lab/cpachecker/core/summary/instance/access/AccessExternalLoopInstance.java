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
package org.sosy_lab.cpachecker.core.summary.instance.access;

import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

public class AccessExternalLoopInstance extends AccessLoopInstance {
  public final SummaryName SUMMARY_NAME = SummaryName.ACCESS_SUMMARY;

  public AccessExternalLoopInstance(
      Loop pLoop,
      PathCopyingPersistentTree<String, Presence> r,
      PathCopyingPersistentTree<String, Presence> w) {
    super(pLoop, r, w);
  }

  @Override
  public SummaryName getSummaryName() {
    return SUMMARY_NAME;
  }

}
