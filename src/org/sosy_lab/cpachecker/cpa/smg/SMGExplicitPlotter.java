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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;


public final class SMGExplicitPlotter {
  private final HashMap<Location, String> locationIndex = new HashMap<>();
  private int offset = 0;
  private final ValueAnalysisState explicitState;
  private final SMGState smgState;

  public SMGExplicitPlotter(ValueAnalysisState pExplicitState, SMGState pSmgState) {
    explicitState = pExplicitState;
    smgState = pSmgState;
  }

  public String smgAsDot(CLangSMG smg, String name, String location) {
    StringBuilder sb = new StringBuilder();

    sb.append("digraph gr_" + name.replace('-', '_') + "{\n");
    offset += 2;
    sb.append(newLineWithOffset("label = \"Location: " + location.replace("\"", "") + "\";"));

    addStackSubgraph(smg, sb);

    for (SMGObject heapObject : smg.getHeapObjects()) {
      sb.append(newLineWithOffset(smgObjectAsDot(heapObject, smg)));
      locationIndex.put(Location.valueOf(heapObject), heapObject.getLabel());
    }

    // This only works after creating all heap Objects,
    // because we can't differentiate between global Memlocs and heap Memlocs
    addGlobalObjectSubgraph(smg, sb);

    Map<Integer, MemoryLocation> coveredBySMG = new HashMap<>();
    Set<MemoryLocation> coveredMemloc = new HashSet<>();

    for (SMGEdgeHasValue edge : smg.getHVEdges()) {

      SMGObject obj = edge.getObject();
      String functionName = smg.getFunctionName(obj);
      MemoryLocation memloc =
          smgState.resolveMemLoc(SMGAddress.valueOf(obj, edge.getOffset()), functionName);
      if (explicitState.contains(memloc)) {
        coveredBySMG.put(edge.getValue(), memloc);
        coveredMemloc.add(memloc);
      }
    }

    for (int value : smg.getValues()) {
      sb.append(newLineWithOffset(smgValueAsDot(value, coveredBySMG)));
    }

    Set<MemoryLocation> notCoveredBySMG = new HashSet<>();

    for (MemoryLocation memloc : explicitState.getTrackedMemoryLocations()) {
      // We don't consider values from the old Nomenclature in explicit cpa
      if (!coveredMemloc.contains(memloc) && !memloc.getAsSimpleString().contains("->")) {
        sb.append(newLineWithOffset(explicitValueAsDot(memloc)));
        notCoveredBySMG.add(memloc);
      }
    }

    for (SMGEdgeHasValue edge : smg.getHVEdges()) {
      sb.append(newLineWithOffset(smgHVEdgeAsDot(edge, smg)));
    }

    for (MemoryLocation memloc : notCoveredBySMG) {
      sb.append(newLineWithOffset(memlocAsDot(memloc)));
    }

    for (SMGEdgePointsTo edge : smg.getPTEdges().values()) {
      sb.append(newLineWithOffset(smgPTEdgeAsDot(edge, smg)));
    }

    sb.append("}");

    return sb.toString();
  }

  private String memlocAsDot(MemoryLocation pMemloc) {
    return locationIndex.get(Location.valueOf(pMemloc))
        + " -> expValue_"
        + explicitState.getValueFor(pMemloc)
        + "[label=\"["
        + (pMemloc.isReference() ? pMemloc.getOffset() : 0)
        + "]\"];";
  }

  private String explicitValueAsDot(MemoryLocation pMemloc) {
    Value value = explicitState.getValueFor(pMemloc);
    return "expValue_" + value.toString() + "[label=\"" + value.toString() + "\"];";
  }

  private void addStackSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_stack {"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Stack\";"));

    int i = 0;
    for (CLangStackFrame stack_item : pSmg.getStackFrames()) {
      addStackItemSubgraph(stack_item, pSb, i);
      i++;
    }
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private void addStackItemSubgraph(CLangStackFrame pStackFrame, StringBuilder pSb, int pIndex) {

    String functionName = pStackFrame.getFunctionDeclaration().getName();

    pSb.append(newLineWithOffset("subgraph cluster_stack_" + functionName + "{"));
    offset += 2;
    pSb.append(newLineWithOffset("fontcolor=blue;"));
    pSb.append(
        newLineWithOffset("label=\"" + pStackFrame.getFunctionDeclaration().toASTString() + "\";"));

    pSb.append(newLineWithOffset(
        smgScopeFrameAsDot(pStackFrame.getVariables(), String.valueOf(pIndex), functionName)));

    offset -= 2;
    pSb.append(newLineWithOffset("}"));

  }

  @Nullable
  private String smgScopeFrameAsDot(
      Map<String, SMGRegion> pNamespace,
      String pStructId,
      String pFunctionName) {
    StringBuilder sb = new StringBuilder();
    sb.append("struct" + pStructId + "[shape=record,label=\" ");

    // I sooo wish for Python list comprehension here...
    ArrayList<String> nodes = new ArrayList<>();
    for (Entry<String, SMGRegion> entry : pNamespace.entrySet()) {
      String key = entry.getKey();
      SMGObject obj = entry.getValue();

      if (key.equals("node")) {
        // escape Node1
        key = "node1";
      }

      nodes.add("<" + key + "> " + obj.toString());
      Location location = Location.valueOf(obj, pFunctionName);
      locationIndex.put(location, "struct" + pStructId + ":" + key);
    }

    Set<MemoryLocation> memoryLocations;

    if (pFunctionName == null) {
      memoryLocations = explicitState.getGlobalMemoryLocations();
    } else {
      memoryLocations = explicitState.getMemoryLocationsOnStack(pFunctionName);
    }

    for (MemoryLocation memloc : memoryLocations) {
      Location location = Location.valueOf(memloc);
      //  We don't consider values written into explicit cpa under the old
      //  Nomenclature
      if (!locationIndex.containsKey(location) && !location.location.contains("->")) {
        // We don't know the size of the memory location
        nodes.add("<" + memloc.getIdentifier() + "> " + memloc.getIdentifier());
        locationIndex.put(location, "struct" + pStructId + ":" + memloc.getIdentifier());
      }
    }

    sb.append(Joiner.on(" | ").join(nodes));
    sb.append("\"];\n");
    return sb.toString();
  }

  private void addGlobalObjectSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_global{"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Global objects\";"));
    pSb.append(newLineWithOffset(smgScopeFrameAsDot(pSmg.getGlobalObjects(), "global", null)));
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private String smgHVEdgeAsDot(SMGEdgeHasValue pEdge, CLangSMG smg) {
    SMGObject obj = pEdge.getObject();
    Location location = Location.valueOf(obj, smg.getFunctionName(obj));

    return locationIndex.get(location) + " -> value_" + pEdge.getValue() + "[label=\"[" + pEdge
        .getOffset() + "]\"];";
  }

  private String smgPTEdgeAsDot(SMGEdgePointsTo pEdge, CLangSMG smg) {

    SMGObject obj = pEdge.getObject();
    Location location = Location.valueOf(obj, smg.getFunctionName(obj));

    return "value_" + pEdge.getValue() + " -> " + locationIndex.get(location) + "[label=\"+" + pEdge
        .getOffset()
        + "B\"];";
  }

  private static String smgObjectAsDot(SMGObject pObject, CLangSMG pSmg) {

    String valid = pSmg.isObjectValid(pObject) ? "" : " : invalid ";
    return pObject.getLabel() + " [ shape=rectangle, label = \"" + pObject.toString() + valid
        + "\"];";
  }

  private String smgValueAsDot(int value, Map<Integer, MemoryLocation> pCoveredBySMG) {

    if (pCoveredBySMG.containsKey(value)) {
      return "value_" + value + "[label=\"#" + value + " : "
          + explicitState.getValueFor(pCoveredBySMG.get(value)) + "\"];";
    } else {
      return "value_" + value + "[label=\"#" + value + "\"];";
    }
  }

  private String newLineWithOffset(String pLine) {
    return Strings.repeat(" ", offset) + pLine + "\n";
  }

  private final static class Location {

    private final String location;

    private Location(SMGObject pSmgObject, String functionName) {
      location = functionName + "::" + pSmgObject.getLabel();
    }

    public static Location valueOf(MemoryLocation pMemloc) {
      return new Location(pMemloc);
    }

    @Nullable
    public static Location valueOf(SMGObject pObj, String pFunctionName) {
      if (pFunctionName == null) {
        return new Location(pObj);
      } else {
        return new Location(pObj, pFunctionName);
      }
    }

    public static Location valueOf(SMGObject pHeapObject) {
      return new Location(pHeapObject);
    }

    private Location(SMGObject pSmgObject) {
      location = pSmgObject.getLabel();
    }

    private Location(MemoryLocation pMemloc) {
      location = pMemloc.getAsSimpleString();
    }

    @Override
    public boolean equals(Object pObj) {

      if (pObj instanceof Location) {
        return location.equals(((Location) pObj).location);
      }

      return false;
    }

    @Override
    public int hashCode() {
      return location.hashCode();
    }

    @Override
    public String toString() {
      return location;
    }
  }

}