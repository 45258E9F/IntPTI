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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.interfaces.Property;

import java.util.Set;


public final class NamedProperty implements Property {

  private final String text;

  private NamedProperty(String pText) {
    this.text = Preconditions.checkNotNull(pText);
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof NamedProperty)) {
      return false;
    }
    return this.text.equals(pOther.toString());
  }

  @Override
  public String toString() {
    return text;
  }

  public static NamedProperty create(final String pText) {
    return new NamedProperty(pText);
  }

  public static Set<Property> singleton(final String pText) {
    return ImmutableSet.<Property>of(NamedProperty.create(pText));
  }

}
