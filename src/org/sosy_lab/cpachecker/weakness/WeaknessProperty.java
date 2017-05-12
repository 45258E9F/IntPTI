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
package org.sosy_lab.cpachecker.weakness;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.core.interfaces.Property;

import javax.annotation.Nonnull;

/**
 * The base class for violation property of weakness
 */
public class WeaknessProperty implements Property {

  private
  @Nonnull
  Weakness weakness;

  public WeaknessProperty(@Nonnull Weakness type) {
    weakness = type;
  }

  @Override
  public String toString() {
    return weakness.getWeaknessName();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(weakness);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    WeaknessProperty thatProperty = (WeaknessProperty) obj;
    if (weakness != thatProperty.weakness) {
      return false;
    }
    return true;
  }

}
