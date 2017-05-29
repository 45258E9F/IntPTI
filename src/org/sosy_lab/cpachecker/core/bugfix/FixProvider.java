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
package org.sosy_lab.cpachecker.core.bugfix;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.core.phase.CPAPhase;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Provide global access to bug fix information.
 */
public final class FixProvider {

  /**
   * The category of bug.
   * It is worth mentioning that a bug category could match one or multiple weaknesses.
   */
  public enum BugCategory {
    INTEGER,    // integer error, including integer overflow/underflow, unexpected cast, etc.
  }

  private static Map<BugCategory, FixInformation> fixInfo = Maps.newHashMap();

  private static Map<BugCategory, Class<? extends CPAPhase>> fixGen = Maps.newHashMap();
  private static Map<BugCategory, Class<? extends CPAPhase>> fixApp = Maps.newHashMap();
  private static Map<BugCategory, Class<? extends CPAPhase>> interactApp = Maps.newHashMap();

  public static void register(
      BugCategory pCategory, FixInformation pInfo, Class<? extends
      CPAPhase> pFixGenClass, Class<? extends CPAPhase> pFixAppClass, @Nullable Class<? extends
      CPAPhase> pIntAppClass) {
    // sanity check: fix information and bug category have strict matching relation
    if (pInfo.getCategory() == pCategory) {
      fixInfo.put(pCategory, pInfo);
      fixGen.put(pCategory, pFixGenClass);
      fixApp.put(pCategory, pFixAppClass);
      // not all bug categories have interactive fix application mode
      if (pIntAppClass != null) {
        interactApp.put(pCategory, pIntAppClass);
      }
    }
  }

  @Nullable
  public static FixInformation getFixInfo(BugCategory pCategory) {
    if (pCategory != null) {
      return fixInfo.get(pCategory);
    }
    return null;
  }

  @Nullable
  public static Class<? extends CPAPhase> getFixGenClass(BugCategory pCategory) {
    if (pCategory != null) {
      return fixGen.get(pCategory);
    }
    return null;
  }

  @Nullable
  public static Class<? extends CPAPhase> getFixAppClass(BugCategory pCategory) {
    if (pCategory != null) {
      return fixApp.get(pCategory);
    }
    return null;
  }

  @Nullable
  public static Class<? extends CPAPhase> getInteractiveAppPhase(BugCategory pCategory) {
    if (pCategory != null) {
      return interactApp.get(pCategory);
    }
    return null;
  }

  public static void clearStatus(BugCategory pCategory) {
    FixInformation info = fixInfo.get(pCategory);
    if (info != null) {
      info.reset();
    }
  }

}
