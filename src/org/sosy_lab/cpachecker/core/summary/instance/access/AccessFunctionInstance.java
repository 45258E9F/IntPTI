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

import org.sosy_lab.cpachecker.core.summary.apply.AbstractFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

public class AccessFunctionInstance extends AbstractFunctionSummaryInstance<AccessResult> {
  PathCopyingPersistentTree<String, Presence> readTree = null;
  PathCopyingPersistentTree<String, Presence> writeTree = null;

  public AccessFunctionInstance(
      String pFunction, PathCopyingPersistentTree<String, Presence> r,
      PathCopyingPersistentTree<String, Presence> w) {
    super(pFunction);
    readTree = r;
    writeTree = w;
  }

  public AccessFunctionInstance(String pFunctionName) {
    super(pFunctionName);
    readTree = new PathCopyingPersistentTree<>();
    writeTree = new PathCopyingPersistentTree<>();
  }

  @Override
  public AccessResult apply() {
    return AccessInstanceUtil.getInstance().buildResult(readTree, writeTree);
  }

  @Override
  public boolean isEqualTo(SummaryInstance pThat) {
    if (!(pThat instanceof AccessFunctionInstance)) {
      return false;
    }
    AccessFunctionInstance that = (AccessFunctionInstance) pThat;
    return that.function.equals(function) &&
        AccessInstanceUtil.getInstance().isEqual(this, that);
  }


  /**
   * @return the readTree
   */
  public PathCopyingPersistentTree<String, Presence> getReadTree() {
    return readTree;
  }


  /**
   * @return the writeTree
   */
  public PathCopyingPersistentTree<String, Presence> getWriteTree() {
    return writeTree;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "AccessFunctionInstance [readTree=" + readTree + ", writeTree=" + writeTree + "]";
  }


}