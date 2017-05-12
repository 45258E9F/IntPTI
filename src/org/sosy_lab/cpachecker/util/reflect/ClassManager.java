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
package org.sosy_lab.cpachecker.util.reflect;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.reflections.Reflections;

import java.util.Collection;
import java.util.Set;

/**
 * Manage class information in current checker project.
 * This is used for reflect purpose, especially when we provide an unqualified name for the
 * particular Java class.
 */
public final class ClassManager {

  private final Multimap<String, Class<?>> classBySimpleName;

  private static final String PREFIX = "org.sosy_lab.cpachecker";

  public ClassManager() {
    classBySimpleName = HashMultimap.create();

    // load all classes in current checker project and build the relation from simple name to
    // classes
    Reflections reflections = new Reflections(PREFIX);
    Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
    for (Class<?> c : classes) {
      classBySimpleName.put(c.getSimpleName(), c);
    }
  }

  /**
   * Retrieve a class object according to specified simple name.
   * Note: a simple name denotes a name without qualifier
   *
   * @param pName unqualified name
   * @return <code>null</code> if no such class exists or multiple classes collides, a valid class
   * object otherwise.
   */
  public Class<?> getClassForSimpleName(String pName) {
    Collection<Class<?>> targets = classBySimpleName.get(pName);
    if (targets.size() != 1) {
      return null;
    }
    return targets.iterator().next();
  }

}
