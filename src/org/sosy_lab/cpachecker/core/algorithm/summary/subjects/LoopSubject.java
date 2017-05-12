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
package org.sosy_lab.cpachecker.core.algorithm.summary.subjects;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.core.algorithm.summary.SummarySubject;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Map;

// This is the default loopSubject as external summary
// We also need a loopSubject as internal summary
public class LoopSubject implements SummarySubject {

  /* *********** */
  /* global part */
  /* *********** */

  private static Map<Loop, LoopSubject> subjects = Maps.newHashMap();

  private static Map<Loop, LoopSubject> internalSubjects = Maps.newHashMap();

  public static LoopSubject of(Loop loop) {
    if (!subjects.containsKey(loop)) {
      subjects.put(loop, new LoopSubject(loop, true));
    }
    return subjects.get(loop);
  }

  public static LoopSubject ofInternal(Loop loop) {
    if (!internalSubjects.containsKey(loop)) {
      internalSubjects.put(loop, new LoopSubject(loop, false));
    }
    return internalSubjects.get(loop);
  }

  /* ********** */
  /* class part */
  /* ********** */

  private final Loop loop;

  private final boolean isExternal;

  private LoopSubject(Loop loop, boolean external) {
    this.loop = loop;
    isExternal = external;
  }

  public Loop getLoop() {
    return loop;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(loop, isExternal);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof LoopSubject)) {
      return false;
    }
    LoopSubject other = (LoopSubject) obj;
    return Objects.equal(loop, other.loop) && isExternal == other.isExternal;
  }

  @Override
  public String toString() {
    return "LoopSubject" + (isExternal ? "(external)" : "(internal)") + " [loop=" + loop.toString
        () + "]";
  }
}
