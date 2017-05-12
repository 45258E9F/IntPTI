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
package org.sosy_lab.cpachecker.cpa.pointer2.util;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Iterator;
import java.util.Set;

public class ExplicitLocationSet implements LocationSet, Iterable<MemoryLocation> {

  private final Set<MemoryLocation> explicitSet;

  /**
   * This flag indicates whether the current location set is tainted.
   * The taint is introduced when we try to join two DIFFERENT location sets.
   */
  private final boolean isTainted;

  private ExplicitLocationSet(ImmutableSet<MemoryLocation> pLocations, boolean pTainted) {
    assert pLocations.size() >= 1;
    this.explicitSet = pLocations;
    isTainted = pTainted;
  }

  @Override
  public boolean mayPointTo(MemoryLocation pLocation) {
    return this.explicitSet.contains(pLocation);
  }

  @Override
  public LocationSet addElement(MemoryLocation pLocation) {
    if (explicitSet.contains(pLocation)) {
      return this;
    }
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    builder.addAll(explicitSet).add(pLocation);
    return new ExplicitLocationSet(builder.build(), false);
  }

  @Override
  public LocationSet addElements(Iterable<MemoryLocation> pLocations) {
    ImmutableSet.Builder<MemoryLocation> builder = null;
    for (MemoryLocation target : pLocations) {
      if (!explicitSet.contains(target)) {
        if (builder == null) {
          builder = ImmutableSet.builder();
          builder.addAll(explicitSet);
        }
        builder.add(target);
      }
    }
    if (builder == null) {
      return this;
    }
    return new ExplicitLocationSet(builder.build(), isTainted);
  }

  // for state-merge purpose only
  public LocationSet addElements(LocationSet pLocationSet, boolean pTainted) {
    if (pLocationSet.isBot()) {
      return this;
    }
    if (pLocationSet.isTop()) {
      return LocationSetTop.INSTANCE;
    }
    ImmutableSet.Builder<MemoryLocation> builder = null;
    for (MemoryLocation target : (ExplicitLocationSet) pLocationSet) {
      if (builder == null) {
        builder = ImmutableSet.builder();
        builder.addAll(explicitSet);
      }
      builder.add(target);
    }
    if (builder == null) {
      return this;
    }
    return new ExplicitLocationSet(builder.build(), pTainted);
  }

  public boolean isTainted() {
    return isTainted;
  }

  @Override
  public LocationSet removeElement(MemoryLocation pLocation) {
    if (!explicitSet.contains(pLocation)) {
      return this;
    }
    if (getSize() == 1) {
      return LocationSetBot.INSTANCE;
    }
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    for (MemoryLocation location : this.explicitSet) {
      if (!location.equals(pLocation)) {
        builder.add(location);
      }
    }
    return new ExplicitLocationSet(builder.build(), isTainted);
  }

  public static LocationSet from(MemoryLocation pLocation) {
    // a fresh location set from the given memory location
    return new ExplicitLocationSet(ImmutableSet.of(pLocation), false);
  }

  public static LocationSet from(Iterable<? extends MemoryLocation> pLocations) {
    Iterator<? extends MemoryLocation> elementIterator = pLocations.iterator();
    if (!elementIterator.hasNext()) {
      return LocationSetBot.INSTANCE;
    }
    ImmutableSet.Builder<MemoryLocation> builder = ImmutableSet.builder();
    while (elementIterator.hasNext()) {
      MemoryLocation location = elementIterator.next();
      builder.add(location);
    }
    return new ExplicitLocationSet(builder.build(), false);
  }

  @Override
  public boolean isBot() {
    return explicitSet.isEmpty();
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    if (pElements == this) {
      return this;
    }
    if (pElements instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pElements;
      return addElements(explicitLocationSet.explicitSet);
    }
    return pElements.addElements((Iterable<MemoryLocation>) this);
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    if (pElements == this) {
      return true;
    }
    if (pElements instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitLocationSet = (ExplicitLocationSet) pElements;
      return explicitSet.containsAll(explicitLocationSet.explicitSet);
    }
    return pElements.containsAll(this);
  }

  @Override
  public String toString() {
    return explicitSet.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof LocationSet) {
      LocationSet o = (LocationSet) pO;
      if (o.isTop()) {
        return false;
      }
      if (o.isBot()) {
        return explicitSet.isEmpty();
      }
      if (o instanceof ExplicitLocationSet) {
        ExplicitLocationSet other = (ExplicitLocationSet) o;
        return explicitSet.equals(other.explicitSet);
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (isBot()) {
      return LocationSetBot.INSTANCE.hashCode();
    }
    if (isTop()) {
      assert false;
      return LocationSetTop.INSTANCE.hashCode();
    }
    return explicitSet.hashCode();
  }

  @Override
  public Iterator<MemoryLocation> iterator() {
    return explicitSet.iterator();
  }

  /**
   * Gets the size of the explicit location set.
   *
   * @return the size of the explicit location set.
   */
  public int getSize() {
    return explicitSet.size();
  }

  public Set<MemoryLocation> getExplicitSet() {
    return explicitSet;
  }

}
