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
package org.sosy_lab.cpachecker.core.phase.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.List;
import java.util.Set;

/**
 * Created by Xi Cheng on 4/28/16.
 */
public class PhaseDependency {

  private final SetMultimap<String, String> dependency;

  public PhaseDependency() {
    dependency = HashMultimap.create();
  }

  public void insertDependency(String phaseID, List<String> relates) {
    dependency.putAll(phaseID, relates);
  }

  public Set<String> keySet() {
    return dependency.keySet();
  }

  public SetMultimap<String, String> copyOfMap() {
    return HashMultimap.create(dependency);
  }

  /**
   * Obtain the iterator of depending target list for the specified phase
   *
   * @param phaseID the identifier of phase for querying
   * @return set of targets, {@code null} for failed query
   */
  public Set<String> queryFor(String phaseID) {
    return dependency.containsKey(phaseID) ? dependency.get(phaseID) : null;
  }

}
