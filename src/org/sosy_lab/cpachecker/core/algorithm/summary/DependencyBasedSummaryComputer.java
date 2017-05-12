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
package org.sosy_lab.cpachecker.core.algorithm.summary;

import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.summary.subjects.FunctionSubject;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.util.callgraph.CallGraph;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compute summary use a CPA
 * It is required that each derived class of DependencyBasedSummaryComputer should have
 * a *PUBLIC* constructor in the form of
 *
 * XyzComputer(Configuration, LogManager, ShutdownNotifier)
 */
abstract public class DependencyBasedSummaryComputer implements SummaryComputer {

  // use set to eliminate duplicated elements
  private Map<SummarySubject, Set<SummarySubject>> dependers;   // those depend on 'this'
  // use set to eliminate duplicated elements
  private Map<SummarySubject, Set<SummarySubject>> dependees;   // those 'this' depend on
  // summary
  protected Map<SummarySubject, SummaryInstance> summary;

  // Configuration
  protected final Configuration config;
  // Logger
  protected final LogManager logger;
  // Shutdown notifier
  protected final ShutdownNotifier shutdownNotifier;

  protected final CFAInfo cfaInfo;

  protected DependencyBasedSummaryComputer(
      Configuration config, LogManager logger,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.shutdownNotifier = shutdownNotifier;
    // initial local variables
    dependers = Maps.newHashMap();
    dependees = Maps.newHashMap();
    summary = Maps.newHashMap();
    // initialize CFA
    CFAInfo info = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (info == null) {
      throw new InvalidConfigurationException("CFA required for summary computation");
    }
    cfaInfo = info;

    // compute dependency in function subjects
    initDependency();
  }

  /**
   * Defines that 'depender' depends 'dependee'
   * Summary of 'dependee' should be re-computed when summary of 'dependee' is changed
   *
   * @param depender depender -(depends)-> dependee
   * @param dependee depender -(depends)-> dependee
   */
  protected void setDependency(SummarySubject depender, SummarySubject dependee) {
    if (!dependers.containsKey(dependee)) {
      dependers.put(dependee, new HashSet<SummarySubject>());
    }
    dependers.get(dependee).add(depender);
    if (!dependees.containsKey(depender)) {
      dependees.put(depender, new HashSet<SummarySubject>());
    }
    dependees.get(depender).add(dependee);
  }

  /**
   * Set all dependencies
   */
  private void initDependency() {
    CallGraph callGraph = cfaInfo.getCallGraph();
    CFA cfa = cfaInfo.getCFA();
    for (FunctionEntryNode caller : cfa.getAllFunctionHeads()) {
      // depender = caller
      FunctionSubject depender = FunctionSubject.of(caller);
      for (FunctionEntryNode callee : callGraph.getCallee(caller)) {
        // dependee = callee
        FunctionSubject dependee = FunctionSubject.of(callee);
        setDependency(depender, dependee);
      }
    }
  }

  /**
   * Summarize from the reached set
   * It can return null when nothing needs to be updated
   */
  abstract protected Map<? extends SummarySubject, ? extends SummaryInstance>
  summarize(SummarySubject subject, ReachedSet reachedSet, SummaryInstance old);

  /**
   * Create a map which contains a single entry (key -> value)
   */
  protected static <K, V> Map<K, V> createSingleEntryMap(K key, V value) {
    Map<K, V> m = Maps.newHashMap();
    m.put(key, value);
    return m;
  }

  @Override
  public Set<SummarySubject> getDepender(SummarySubject subject) {
    return dependers.containsKey(subject)
           ? Collections.unmodifiableSet(dependers.get(subject))
           : Collections.<SummarySubject>emptySet();
  }


  @Override
  public Set<SummarySubject> getDependee(SummarySubject subject) {
    return dependees.containsKey(subject)
           ? Collections.unmodifiableSet(dependees.get(subject))
           : Collections.<SummarySubject>emptySet();
  }

  @Override
  public Set<SummarySubject> getSubjects() {
    return summary.keySet();
  }

  @Override
  public boolean update(SummarySubject subject, SummaryInstance inst) {
    SummaryInstance old = summary.get(subject);
    summary.put(subject, inst);
    // return true if summary mapping changes
    return old == null || !old.isEqualTo(inst);
  }

  @Override
  public void preAction() {
    // do nothing
  }

  @Override
  public void postAction() {
    // do nothing
  }

}
