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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

import java.util.List;

public class CompositePrecision implements WrapperPrecision {
  private final List<Precision> precisions;

  public CompositePrecision(List<Precision> precisions) {
    this.precisions = ImmutableList.copyOf(precisions);
  }

  public List<Precision> getPrecisions() {
    return precisions;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other == null || !(other instanceof CompositePrecision)) {
      return false;
    }

    return precisions.equals(((CompositePrecision) other).precisions);
  }

  @Override
  public int hashCode() {
    return precisions.hashCode();
  }

  public Precision get(int idx) {
    return precisions.get(idx);
  }

  @Override
  public String toString() {
    return precisions.toString();
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (Precision precision : precisions) {
      if (pType.isAssignableFrom(precision.getClass())) {
        return pType.cast(precision);

      } else if (precision instanceof WrapperPrecision) {
        T result = ((WrapperPrecision) precision).retrieveWrappedPrecision(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public Precision replaceWrappedPrecision(
      Precision newPrecision,
      Predicate<? super Precision> replaceType) {

    if (replaceType.apply(this)) {
      return newPrecision;
    }

    ImmutableList.Builder<Precision> newPrecisions = ImmutableList.builder();
    boolean changed = false;
    for (Precision precision : precisions) {
      if (replaceType.apply(precision)) {
        newPrecisions.add(newPrecision);
        changed = true;

      } else if (precision instanceof WrapperPrecision) {
        Precision newWrappedPrecision =
            ((WrapperPrecision) precision).replaceWrappedPrecision(newPrecision, replaceType);
        if (newWrappedPrecision != null) {
          newPrecisions.add(newWrappedPrecision);
          changed = true;

        } else {
          newPrecisions.add(precision);
        }
      } else {
        newPrecisions.add(precision);
      }
    }
    return changed ? new CompositePrecision(newPrecisions.build()) : null;
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return precisions;
  }
}
