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
package org.sosy_lab.cpachecker.cpa.shape.communicator;

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph.MemoryPoint;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.values.Address;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.access.AccessPath;
import org.sosy_lab.cpachecker.util.access.PathSegment;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public final class CoreCommunicator {

  private static CoreCommunicator instance;

  public static CoreCommunicator getInstance() {
    if (instance == null) {
      instance = new CoreCommunicator();
    }
    return instance;
  }

  /* ******************** */
  /* value interpretation */
  /* ******************** */

  /**
   * Derive the symbolic value of the given memory location along with type.
   *
   * @param pState    the specified shape state
   * @param pLocation memory location containing identifier, function name and offset
   * @param pType     the type of value to be read from the state
   */
  public ShapeSymbolicValue getValueFor(ShapeState pState, MemoryLocation pLocation, CType pType) {
    SGHasValueEdge edge = getHasValueEdgeFor(pState, pLocation, pType);
    if (edge == null) {
      return UnknownValue.getInstance();
    }
    SGObject object = edge.getObject();
    int offset = edge.getOffset();
    CType type = edge.getType();
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(object).filterByType(type)
        .filterAtOffset(offset);
    Set<SGHasValueEdge> matchingEdges = pState.getHasValueEdgesFor(filter);
    if (matchingEdges.isEmpty()) {
      return UnknownValue.getInstance();
    }
    SGHasValueEdge oneEdge = Iterables.getOnlyElement(matchingEdges);
    return KnownSymbolicValue.valueOf(oneEdge.getValue());
  }

  /**
   * Derive the symbolic value of the given access path.
   *
   * @param pState the specified shape state
   * @param pPath  the access path
   */
  public ShapeSymbolicValue getValueFor(ShapeState pState, AccessPath pPath) {
    SGHasValueEdge edge = getHasValueEdgeFor(pState, pPath);
    if (edge == null) {
      return UnknownValue.getInstance();
    }
    SGObject object = edge.getObject();
    int offset = edge.getOffset();
    CType type = edge.getType();
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(object).filterByType(type)
        .filterAtOffset(offset);
    Set<SGHasValueEdge> matchingEdges = pState.getHasValueEdgesFor(filter);
    if (matchingEdges.isEmpty()) {
      return UnknownValue.getInstance();
    }
    SGHasValueEdge oneEdge = Iterables.getOnlyElement(matchingEdges);
    return KnownSymbolicValue.valueOf(oneEdge.getValue());
  }

  public Pair<ShapeSymbolicValue, CType> getValueFor(ShapeState pState, String qualifiedName,
                                                     List<PathSegment> remSegments) {
    SGHasValueEdge edge = getHasValueEdgeFor(pState, qualifiedName, remSegments);
    ShapeSymbolicValue result;
    if (edge == null) {
      result = UnknownValue.getInstance();
      return Pair.of(result, null);
    }
    SGObject object = edge.getObject();
    int offset = edge.getOffset();
    CType type = edge.getType();
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(object).filterByType(type)
        .filterAtOffset(offset);
    Set<SGHasValueEdge> matchingEdges = pState.getHasValueEdgesFor(filter);
    if (matchingEdges.isEmpty()) {
      result = UnknownValue.getInstance();
      return Pair.of(result, null);
    }
    SGHasValueEdge oneEdge = Iterables.getOnlyElement(matchingEdges);
    result = KnownSymbolicValue.valueOf(oneEdge.getValue());
    return Pair.of(result, type);
  }

  public Pair<ShapeSymbolicValue, CType> getValueFor(ShapeState pState, Address pAddress,
                                                     CType pType, List<PathSegment> pRemSegments) {
    SGHasValueEdge edge = getHasValueEdgeFor(pState, pAddress, pType, pRemSegments);
    ShapeSymbolicValue result;
    if (edge == null) {
      result = UnknownValue.getInstance();
      return Pair.of(result, null);
    }
    SGObject object = edge.getObject();
    int offset = edge.getOffset();
    CType type = edge.getType();
    SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(object).filterByType(type)
        .filterAtOffset(offset);
    Set<SGHasValueEdge> matchingEdges = pState.getHasValueEdgesFor(filter);
    if (matchingEdges.isEmpty()) {
      result = UnknownValue.getInstance();
      return Pair.of(result, null);
    }
    SGHasValueEdge oneEdge = Iterables.getOnlyElement(matchingEdges);
    result = KnownSymbolicValue.valueOf(oneEdge.getValue());
    return Pair.of(result, type);
  }


  /* ****************************** */
  /* memory location interpretation */
  /* ****************************** */

  @Nullable
  public SGHasValueEdge getHasValueEdgeFor(
      ShapeState pState, MemoryLocation pLocation, CType
      pType) {
    SGObject object = pState.getObjectForMemoryLocation(pLocation);
    if (object == null) {
      return null;
    }
    int offset = (int) pLocation.getOffset();
    return new SGHasValueEdge(pType, offset, object, 0);
  }

  @Nullable
  public SGHasValueEdge getHasValueEdgeFor(ShapeState pState, AccessPath pPath) {
    MemoryPoint location = pState.getMemoryPointForAccessPath(pPath);
    if (location == null) {
      return null;
    }
    MemoryLocation mLoc = location.getLocation();
    int offset = (int) location.getOffset();
    CType type = location.getType();
    SGObject object = pState.getObjectForMemoryLocation(mLoc);
    if (object == null) {
      return null;
    }
    return new SGHasValueEdge(type, offset, object, 0);
  }

  @Nullable
  public SGHasValueEdge getHasValueEdgeFor(ShapeState pState, String pDeclaredName,
                                           List<PathSegment> pSegments) {
    MemoryPoint location = pState.getMemoryPointForAccessPath(pDeclaredName, pSegments);
    if (location == null) {
      return null;
    }
    MemoryLocation mLoc = location.getLocation();
    int offset = (int) location.getOffset();
    CType type = location.getType();
    SGObject object = pState.getObjectForMemoryLocation(mLoc);
    if (object == null) {
      return null;
    }
    return new SGHasValueEdge(type, offset, object, 0);
  }

  @Nullable
  public SGHasValueEdge getHasValueEdgeFor(
      ShapeState pState, Address pAddress, CType pType,
      List<PathSegment> pRemSegments) {
    MemoryPoint location = pState.getMemoryPointForAddress(pAddress, pType, pRemSegments);
    if (location == null) {
      return null;
    }
    MemoryLocation mLoc = location.getLocation();
    int offset = (int) location.getOffset();
    CType type = location.getType();
    SGObject object = pState.getObjectForMemoryLocation(mLoc);
    if (object == null) {
      return null;
    }
    return new SGHasValueEdge(type, offset, object, 0);
  }

}
