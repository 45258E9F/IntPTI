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
package org.sosy_lab.cpachecker.core.phase.entry;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.callgraph.CallGraph;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * First, sort the functions by the number of callers
 * Then, select as few as possible functions as entry so that
 * the entire call graph is covered assuming that
 * each analysis from the entry covers all nodes within the certain depth
 */
public class CheapCoverStrategy implements StaticEntryStrategy {

  private int depth;
  private List<CFANode> entries;

  private CallGraph callGraph;

  private Map<FunctionEntryNode, Integer> inDegreeMap = Maps.newHashMap();
  private Map<FunctionEntryNode, FunctionEntryWrapper> wrappers = Maps.newHashMap();

  public CheapCoverStrategy(int pDepth) {
    depth = pDepth;
    Optional<CFAInfo> cfaInfo = GlobalInfo.getInstance().getCFAInfo();
    assert cfaInfo.isPresent();
    CFAInfo info = cfaInfo.get();
    entries = null;
    // initialize in-degree map
    CFA cfa = info.getCFA();
    callGraph = info.getCallGraph();
    for (FunctionEntryNode entry : cfa.getAllFunctionHeads()) {
      inDegreeMap.put(entry, callGraph.getNumCaller(entry));
      wrappers.put(entry, new FunctionEntryWrapper(entry));
    }
  }

  @Override
  public Collection<CFANode> getInitialEntry(CFA pCFA) {
    if (entries == null) {
      computeEntry();
      assert (entries != null);
    }
    return entries;
  }

  private void computeEntry() {
    PriorityQueue<FunctionEntryWrapper> remainEntrySet = new PriorityQueue<>();
    remainEntrySet.addAll(wrappers.values());
    PriorityQueue<FunctionEntryWrapper> waitList = new PriorityQueue<>();
    waitList.add(remainEntrySet.poll());
    entries = Lists.newArrayList();
    while (!remainEntrySet.isEmpty() || !waitList.isEmpty()) {
      while (!waitList.isEmpty()) {
        FunctionEntryNode nextEntry = waitList.poll().entry;
        entries.add(nextEntry);
        Set<FunctionEntryNode> reachable = Sets.newHashSet();
        Set<FunctionEntryNode> almostReachable = Sets.newHashSet();
        collectReachable(nextEntry, depth, reachable, almostReachable);
        for (FunctionEntryNode r : reachable) {
          remainEntrySet.remove(wrappers.get(r));
        }
        for (FunctionEntryNode ar : almostReachable) {
          FunctionEntryWrapper arWrapper = wrappers.get(ar);
          if (remainEntrySet.contains(arWrapper)) {
            waitList.add(arWrapper);
          }
        }
      }
      if (!remainEntrySet.isEmpty()) {
        FunctionEntryWrapper nextWrapper = remainEntrySet.poll();
        waitList.add(nextWrapper);
      }
    }
  }

  /**
   * Collect reachable functions with respect to the specified call depth.
   * @param pEntry the starting function
   * @param pDepth maximum call chain length
   * @param pReachable reachable functions
   * @param pAlmostReachable unreachable functions adjacent to the furthest reachable functions
   */
  private void collectReachable(FunctionEntryNode pEntry, int pDepth,
                                Set<FunctionEntryNode> pReachable,
                                Set<FunctionEntryNode> pAlmostReachable) {
    if (!pReachable.contains(pEntry)) {
      pReachable.add(pEntry);
      if (pDepth > 1) {
        for (FunctionEntryNode next : callGraph.getCallee(pEntry)) {
          collectReachable(next, pDepth - 1, pReachable, pAlmostReachable);
        }
      } else if (pDepth <= 0) {
        for (FunctionEntryNode next : callGraph.getCallee(pEntry)) {
          collectReachable(next, pDepth, pReachable, pAlmostReachable);
        }
      } else {
        // depth equals to 1
        for (FunctionEntryNode next : callGraph.getCallee(pEntry)) {
          if (!pReachable.contains(next)) {
            pAlmostReachable.add(next);
          }
        }
      }
    }
  }

  private class FunctionEntryWrapper implements Comparable<FunctionEntryWrapper> {

    private final FunctionEntryNode entry;

    private FunctionEntryWrapper(FunctionEntryNode pEntry) {
      entry = pEntry;
    }

    @Override
    public int compareTo(FunctionEntryWrapper pOther) {
      return inDegreeMap.get(entry) - inDegreeMap.get(pOther.entry);
    }

    @Override
    public int hashCode() {
      return entry.hashCode();
    }

    @Override
    public boolean equals(Object pO) {
      if (pO == this) {
        return true;
      }
      if (pO == null || !(pO instanceof FunctionEntryWrapper)) {
        return false;
      }
      FunctionEntryWrapper other = (FunctionEntryWrapper) pO;
      return entry.equals(other.entry);
    }
  }

}
