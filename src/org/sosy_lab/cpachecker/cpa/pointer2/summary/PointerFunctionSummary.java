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
package org.sosy_lab.cpachecker.cpa.pointer2.summary;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class PointerFunctionSummary implements Summary<PointerFunctionSummary> {

  private CFANode functionEntryNode;
  private SortedMap<MemoryLocation, LocationSet> changedGlobals;
  private Map<Integer, LocationSet> changedFormalPointerParas;
  private LocationSet returnedSet;
  private Set<CFAEdge> calledFunctions;

  public PointerFunctionSummary(CFANode pFunctionEntryNode) {
    functionEntryNode = pFunctionEntryNode;
    changedGlobals = new TreeMap<>();
    changedFormalPointerParas = new HashMap<>();
    returnedSet = null;
    calledFunctions = new HashSet<>();
  }

  public PointerFunctionSummary(PointerFunctionSummary pFunctionSummary) {
    functionEntryNode = pFunctionSummary.getFunctionEntry();
    changedGlobals = new TreeMap<>();
    changedGlobals.putAll(pFunctionSummary.getChangedGlobals());
    changedFormalPointerParas = new HashMap<>();
    changedFormalPointerParas.putAll(pFunctionSummary.changedFormalPointerParas);
    if (pFunctionSummary.getReturnedSet() instanceof ExplicitLocationSet) {
      returnedSet = ExplicitLocationSet.from(((ExplicitLocationSet) pFunctionSummary
          .getReturnedSet()).getExplicitSet());
    } else {
      returnedSet = pFunctionSummary.returnedSet;
    }
    calledFunctions = new HashSet<>();
    calledFunctions.addAll(pFunctionSummary.getCalledFunctions());
  }

  public PointerFunctionSummary mergeGlobal(
      PointerFunctionSummary merged,
      PointerFunctionSummary other) {
    for (MemoryLocation mem : other.getChangedGlobals().keySet()) {
      merged.addChangedGlobal(mem, other.getChangedGlobals().get(mem));
    }
    return merged;
  }

  public PointerFunctionSummary mergeChangedParas(
      PointerFunctionSummary merged,
      PointerFunctionSummary other) {
    for (Integer index : other.getChangedFormalPointerParas().keySet()) {
      merged.addChangedFormalPointerParas(index, other.getChangedFormalPointerParas().get(index));
    }
    return merged;
  }

  public PointerFunctionSummary mergeReturned(
      PointerFunctionSummary merged,
      PointerFunctionSummary other) {
    merged.setReturnedSet(other.getReturnedSet());
    return merged;
  }

  public PointerFunctionSummary mergeCalledFunctions(
      PointerFunctionSummary merged,
      PointerFunctionSummary other) {
    for (CFAEdge edge : other.getCalledFunctions()) {
      merged.getCalledFunctions().add(edge);
    }
    return merged;
  }

  public PointerFunctionSummary mergeFunctionSummary(PointerFunctionSummary other) {
    PointerFunctionSummary merged = new PointerFunctionSummary(this);
    merged = mergeGlobal(merged, other);
    merged = mergeChangedParas(merged, other);
    merged = mergeReturned(merged, other);
    merged = mergeCalledFunctions(merged, other);
    return merged;
  }

  public CFANode getFunctionEntry() {
    return functionEntryNode;
  }

  public void addChangedGlobal(MemoryLocation pMemoryLocation, LocationSet pLocationSet) {
    if (changedGlobals.containsKey(pMemoryLocation)) {
      if (!changedGlobals.get(pMemoryLocation).containsAll(pLocationSet)) {
        pLocationSet.addElements(changedGlobals.get(pMemoryLocation));
        changedGlobals.put(pMemoryLocation, pLocationSet);
      }
    } else {
      changedGlobals.put(pMemoryLocation, pLocationSet);
    }
    //if the size of explicit locationset beyond bound, change it to Top
    LocationSet locationSet = changedGlobals.get(pMemoryLocation);
    if (locationSet instanceof ExplicitLocationSet) {
      if (((ExplicitLocationSet) locationSet).getSize() > 10) {
        changedGlobals.put(pMemoryLocation, LocationSetTop.INSTANCE);
      }
    }
  }

  public SortedMap<MemoryLocation, LocationSet> getChangedGlobals() {
    return changedGlobals;
  }

  public void addChangedFormalPointerParas(Integer pValue, LocationSet pLocationSet) {
    if (changedFormalPointerParas.containsKey(pValue)) {
      if (!changedFormalPointerParas.get(pValue).containsAll(pLocationSet)) {
        pLocationSet.addElements(changedFormalPointerParas.get(pValue));
        changedFormalPointerParas.put(pValue, pLocationSet);
      }

    } else {
      changedFormalPointerParas.put(pValue, pLocationSet);
    }
  }

  public Map<Integer, LocationSet> getChangedFormalPointerParas() {
    return changedFormalPointerParas;
  }

  public void setReturnedSet(LocationSet pReturnedSet) {
    if (returnedSet != null) {
      if (!returnedSet.containsAll(pReturnedSet)) {
        pReturnedSet.addElements(returnedSet);
        returnedSet = pReturnedSet;
      }
    } else {
      returnedSet = pReturnedSet;
    }
  }

  public LocationSet getReturnedSet() {
    return returnedSet;
  }

  public void addCalledFunctions(CFAEdge pCalledFunction) {
    calledFunctions.add(pCalledFunction);
  }

  @Override
  public Set<CFAEdge> getCalledFunctions() {
    return calledFunctions;
  }

  @Override
  public String getFunctionName() {
    return functionEntryNode.getFunctionName();
  }

  @Override
  public void addChanged(MemoryLocation pMemoryLocation, LocationSet pLocationSet) {
    addChangedGlobal(pMemoryLocation, pLocationSet);
  }

  @Override
  public PointerFunctionSummary copyOf() {
    return new PointerFunctionSummary(this);
  }

  public Set<MemoryLocation> getKnownLocations() {
    return FluentIterable.from(Iterables
        .concat(changedGlobals.keySet(),
            FluentIterable.from(changedGlobals.values()).transformAndConcat
                (new
                     Function<LocationSet, Iterable<? extends MemoryLocation>>() {

                       @Override
                       public Iterable<? extends MemoryLocation> apply(LocationSet pArg0) {
                         if (pArg0 instanceof ExplicitLocationSet) {
                           return (ExplicitLocationSet) pArg0;
                         }
                         return Collections.emptySet();
                       }

                     }))).toSet();
  }
}
