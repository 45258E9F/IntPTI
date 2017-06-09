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
package org.sosy_lab.cpachecker.cpa.pointer2;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Instances of this class are pointer states that are used as abstract elements
 * in the pointer CPA.
 */
public class PointerState implements AbstractState, Graphable {

  private Pointer2State innerState;

  /**
   * Creates a new pointer state with an empty initial points-to map.
   */
  private PointerState() {
    innerState = Pointer2State.INITIAL_STATE;
  }

  /**
   * Creates a new pointer state from the given persistent points-to map.
   */
  private PointerState(Pointer2State pState) {
    innerState = new Pointer2State(pState);
  }

  public PointerState copyOf() {
    return new PointerState(innerState);
  }

  /**
   * Gets a pointer state representing the points to information of this state
   * combined with the information that the first given identifier points to the
   * second given identifier.
   *
   * @param pSource the first identifier.
   * @param pTarget the second identifier.
   * @return the pointer state.
   */
  public PointerState addPointsToInformation(MemoryLocation pSource, MemoryLocation pTarget) {
    return new PointerState(innerState.addPointsToInformation(pSource, pTarget));
  }

  /**
   * Gets a pointer state representing the points to information of this state    curLoop = pCurrentLoop;
   * combined with the information that the first given identifier points to the
   * given target identifiers.
   *
   * @param pSource  the first identifier.
   * @param pTargets the target identifiers.
   * @return the pointer state.
   */
  public PointerState addPointsToInformation(
      MemoryLocation pSource,
      Iterable<MemoryLocation> pTargets) {
    return new PointerState(innerState.addPointsToInformation(pSource, pTargets));
  }

  /**
   * Gets a pointer state representing the points to information of this state
   * combined with the information that the first given identifier points to the
   * given target identifiers.
   *
   * @param pSource  the first identifier.
   * @param pTargets the target identifiers.
   * @return the pointer state.
   */
  public PointerState addPointsToInformation(MemoryLocation pSource, LocationSet pTargets) {
    return new PointerState(innerState.addPointsToInformation(pSource, pTargets));
  }

  /**
   * Gets the points-to set mapped to the given identifier.
   *
   * @param pSource the identifier pointing to the points-to set in question.
   * @return the points-to set of the given identifier.
   */
  public LocationSet getPointsToSet(MemoryLocation pSource) {
    return innerState.getPointsToSet(pSource);
  }

  /**
   * Checks whether or not the first identifier points to the second identifier.
   *
   * @param pSource the first identifier.
   * @param pTarget the second identifier.
   * @return <code>true</code> if the first identifier definitely points to the second identifier,
   * <code>false</code> if it definitely does not point to the second identifier and
   * <code>null</code> if it might point to it.curLoop
   */
  @Nullable
  public Boolean pointsTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return innerState.pointsTo(pSource, pTarget);
  }

  /**
   * Checks whether or not the first identifier is known to point to the second
   * identifier.
   *
   * @return <code>true</code> if the first identifier definitely points to the second identifier,
   * <code>false</code> if it might point to it or is known not to point to it.
   */
  public boolean definitelyPointsTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return innerState.definitelyPointsTo(pSource, pTarget);
  }

  /**
   * Checks whether or not the first identifier is known to not point to the
   * second identifier.
   *
   * @return <code>true</code> if the first identifier definitely does not points to the second
   * identifier, <code>false</code> if it might point to it or is known to point to it.
   */
  public boolean definitelyNotPointsTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return innerState.definitelyNotPointsTo(pSource, pTarget);
  }

  /**
   * Checks whether or not the first identifier is may point to the second
   * identifier.
   *
   * @return <code>true</code> if the first identifier definitely points to the second identifier or
   * might point to it, <code>false</code> if it is known not to point to it.
   */
  public boolean mayPointTo(MemoryLocation pSource, MemoryLocation pTarget) {
    return innerState.mayPointTo(pSource, pTarget);
  }

  /**
   * Gets all locations known to the state.
   *
   * @return all locations known to the state.
   */
  Set<MemoryLocation> getKnownLocations() {
    return innerState.getKnownLocations();
  }

  /**
   * Gets the points-to map of this state.
   *
   * @return the points-to map of this state.
   */
  public Map<MemoryLocation, LocationSet> getPointsToMap() {
    return innerState.getPointsToMap();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || !(pO instanceof PointerState)) {
      return false;
    }
    PointerState other = (PointerState) pO;
    return Objects.equal(innerState, other.innerState);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(innerState);
  }

  @Override
  public String toString() {
    return innerState.toString();
  }

}
