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
package org.sosy_lab.cpachecker.util.refinement;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Set;

/**
 * State that allows forgetting values of {@link MemoryLocation}s and readding them.
 *
 * @param <T> arbitrary type containing all information necessary for the implementation to recreate
 *            the previous state after a delete
 */
public interface ForgetfulState<T> extends AbstractState {

  T forget(MemoryLocation location);

  void remember(MemoryLocation location, T forgottenInformation);

  Set<MemoryLocation> getTrackedMemoryLocations();

  int getSize();
}
