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
package org.sosy_lab.cpachecker.util.collections.map;

import org.sosy_lab.common.collect.PersistentSortedMap;

public interface PersistentBiMap<K, V> extends PersistentSortedMap<K, V> {
  public PersistentBiMap<V, K> inverse();

  @Override
  public PersistentBiMap<K, V> putAndCopy(K key, V value);

  @Override
  public PersistentBiMap<K, V> removeAndCopy(Object key);
}