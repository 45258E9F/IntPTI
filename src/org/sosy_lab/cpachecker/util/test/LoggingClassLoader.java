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
package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.ChildFirstPatternClassLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is a class loader that keeps a reference to
 * all classes that have been loaded through it.
 */
public class LoggingClassLoader extends ChildFirstPatternClassLoader {

  private final List<Class<?>> loadedClasses = new ArrayList<>();

  /**
   * Create a new class loader.
   *
   * @param pChildFirstPattern The pattern telling which classes should never be loaded by the
   *                           parent.
   * @param pUrls              The sources where this class loader should load classes from.
   * @param pParent            The parent class loader.
   */
  public LoggingClassLoader(
      Pattern pChildFirstPattern, URL[] pUrls, ClassLoader pParent) {
    super(pChildFirstPattern, pUrls, pParent);
  }

  @Override
  public Class<?> loadClass(String pName) throws ClassNotFoundException {
    Class<?> cls = super.loadClass(pName);
    loadedClasses.add(cls);
    return cls;
  }

  public ImmutableList<Class<?>> getLoadedClasses() {
    return ImmutableList.copyOf(loadedClasses);
  }
}
