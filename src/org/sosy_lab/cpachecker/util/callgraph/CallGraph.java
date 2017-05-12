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
package org.sosy_lab.cpachecker.util.callgraph;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import java.util.Collection;

/**
 * Call Graph
 *
 * Note that undefined function are stil maintained in the call graph
 * -> FIXME tomgu, undefined function does not have functionCall edge, so it will not be maintained.
 * Therefore, the multimap uses String instead of FunctionEntryNode
 */
public class CallGraph {
  private CFA cfa;
  private final Multimap<String, String> callers;
  private final Multimap<String, String> callees;

  CallGraph(CFA cfa) {
    this.cfa = cfa;
    callers = HashMultimap.create();
    callees = HashMultimap.create();
  }

  void attach(FunctionEntryNode caller, FunctionEntryNode callee) {
    callees.put(caller.getFunctionName(), callee.getFunctionName());
    callers.put(callee.getFunctionName(), caller.getFunctionName());
  }

  /**
   * Return the set of functions that calls f
   */
  public Collection<String> getCaller(String f) {
    return callers.get(f);
  }

  /**
   * Return the set of functions that calls f
   * Remove those without definitions
   */
  public Collection<FunctionEntryNode> getCaller(FunctionEntryNode f) {
    return FluentIterable.from(getCaller(f.getFunctionName())).transform(
        new Function<String, FunctionEntryNode>() {
          @Override
          public FunctionEntryNode apply(String name) {
            return cfa.getFunctionHead(name);
          }
        }).filter(Predicates.notNull()).toList();
  }

  /**
   * Return the set of functions that are called by f
   */
  public Collection<String> getCallee(String f) {
    return callees.get(f);
  }

  /**
   * Return the set of functions that are called by f
   * Remove those without definitions
   */
  public Collection<FunctionEntryNode> getCallee(FunctionEntryNode f) {
    return FluentIterable.from(getCallee(f.getFunctionName())).transform(
        new Function<String, FunctionEntryNode>() {
          @Override
          public FunctionEntryNode apply(String name) {
            return cfa.getFunctionHead(name);
          }
        }).filter(Predicates.notNull()).toList();
  }

  /**
   * @return nonnull <=> f is defined
   */
  public Optional<FunctionEntryNode> getFunctionEntryNode(String f) {
    return Optional.of(cfa.getFunctionHead(f));
  }

  public int getNumCaller(FunctionEntryNode f) {
    return getCaller(f).size();
  }

  public int getNumCallee(FunctionEntryNode f) {
    return getCallee(f).size();
  }
}
