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
package org.sosy_lab.cpachecker.util.octagon;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class OctagonPhantomReference extends PhantomReference<Octagon> {

  private Long octRef;
  private OctagonManager manager;

  public OctagonPhantomReference(Octagon reference, ReferenceQueue<? super Octagon> queue) {
    super(reference, queue);
    octRef = reference.getOctId();
    manager = reference.getManager();
  }

  public void cleanup() {
    manager.free(octRef);
  }
}