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
package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CFAPathStandardState implements CFAPathState, Iterable<CFAEdge> {

  private static final CFAPathStandardState sEmptyPath = new CFAPathStandardState();

  public static CFAPathStandardState getEmptyPath() {
    return sEmptyPath;
  }

  private final CFAPathStandardState mPredecessor;
  private final CFAEdge mCFAEdge;
  private final int mLength;

  private static class CFAEdgeIterator implements Iterator<CFAEdge> {

    private CFAPathStandardState crrentState;

    public CFAEdgeIterator(CFAPathStandardState pLastElement) {
      crrentState = pLastElement;
    }

    @Override
    public boolean hasNext() {
      return (crrentState != sEmptyPath);
    }

    @Override
    public CFAEdge next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      CFAEdge lNextCFAEdge = crrentState.mCFAEdge;

      crrentState = crrentState.mPredecessor;

      return lNextCFAEdge;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private CFAPathStandardState() {
    mPredecessor = null;
    mCFAEdge = null;
    mLength = 0;
  }

  public CFAPathStandardState(CFAPathStandardState pPredecessor, CFAEdge pCFAEdge) {
    if (pPredecessor == null) {
      throw new IllegalArgumentException();
    }

    if (pCFAEdge == null) {
      throw new IllegalArgumentException();
    }

    mPredecessor = pPredecessor;
    mCFAEdge = pCFAEdge;
    mLength = pPredecessor.getLength() + 1;
  }

  public int getLength() {
    return mLength;
  }

  public CFAEdge get(int lIndex) {
    if (lIndex >= mLength || lIndex < 0) {
      throw new IllegalArgumentException();
    }

    if (lIndex + 1 == mLength) {
      return mCFAEdge;
    } else {
      return mPredecessor.get(lIndex);
    }
  }

  @Override
  /*
   * Traverses the cfa path backwards.
   */
  public Iterator<CFAEdge> iterator() {
    return new CFAEdgeIterator(this);
  }

  public CFAEdge[] toArray() {
    CFAEdge[] lPath = new CFAEdge[mLength];

    CFAPathStandardState lElement = this;

    for (int lIndex = mLength - 1; lIndex >= 0; lIndex--) {
      lPath[lIndex] = lElement.mCFAEdge;
      lElement = lElement.mPredecessor;
    }

    return lPath;
  }

  @Override
  public String toString() {
    if (getLength() == 0) {
      return "<>";
    } else {
      if (getLength() == 1) {
        return "< " + mCFAEdge.toString() + " >";
      } else {
        return "< ... " + mCFAEdge.toString() + " >";
      }
    }
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
