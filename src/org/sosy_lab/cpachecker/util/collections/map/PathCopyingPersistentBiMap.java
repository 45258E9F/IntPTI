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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

public class PathCopyingPersistentBiMap<K extends Comparable<? super K>, V extends Comparable<? super V>>
    implements PersistentBiMap<K, V> {

  private static final PathCopyingPersistentBiMap<?, ?> EMPTY_MAP =
      new PathCopyingPersistentBiMap();

  private PersistentSortedMap<K, V> self;    // self
  private PersistentSortedMap<V, K> inv;     // inverse

  private PathCopyingPersistentBiMap() {
    this.self = PathCopyingPersistentTreeMap.of();
    this.inv = PathCopyingPersistentTreeMap.of();
  }

  private PathCopyingPersistentBiMap(
      PersistentSortedMap<K, V> self,
      PersistentSortedMap<V, K> inv) {
    this.self = self;
    this.inv = inv;
  }

  public static class BiMapBuilder<K extends Comparable<? super K>, V extends Comparable<? super V>> {
    private PersistentBiMap<K, V> bimap;

    public BiMapBuilder() {
      bimap = new PathCopyingPersistentBiMap<>();
    }

    public BiMapBuilder<K, V> put(K key, V value) {
      bimap = bimap.putAndCopy(key, value);
      return this;
    }

    public PersistentBiMap<K, V> build() {
      return bimap;
    }
  }

  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V extends Comparable<? super V>>
  PathCopyingPersistentBiMap<K, V> of() {
    return (PathCopyingPersistentBiMap<K, V>) EMPTY_MAP;
  }

  public static <K extends Comparable<? super K>, V extends Comparable<? super V>>
  BiMapBuilder<K, V> builder() {
    return new BiMapBuilder<>();
  }

  @Override
  public PersistentSortedMap<K, V> empty() {
    return new PathCopyingPersistentBiMap<>();
  }

  @Override
  public SortedSet<java.util.Map.Entry<K, V>> entrySet() {
    return self.entrySet();
  }

  @Override
  public SortedSet<K> keySet() {
    return self.keySet();
  }

  /**
   * None of key-value can be null
   */
  @Override
  public PersistentBiMap<K, V> putAndCopy(K key, V value) {
    if (key == null || value == null) {
      throw new IllegalArgumentException("both key and value can not be null for PersistedBiMap");
    }
    if (self.containsKey(key) || inv.containsKey(value)) {
      throw new IllegalArgumentException(
          "key or value already bound for (" + key + "," + value + ") pair.");
    }
    PersistentSortedMap<K, V> newSelf = self.putAndCopy(key, value);
    PersistentSortedMap<V, K> newInv = inv.putAndCopy(value, key);
    return new PathCopyingPersistentBiMap<>(newSelf, newInv);
  }

  @Override
  public PersistentBiMap<K, V> removeAndCopy(Object key) {
    if (key == null) {
      throw new IllegalArgumentException("key can not be null for PersistedBiMap");
    }
    if (self.containsKey(key)) {
      V value = self.get(key);
      PersistentSortedMap<K, V> newSelf = self.removeAndCopy(key);
      PersistentSortedMap<V, K> newInv = inv.removeAndCopy(value);
      return new PathCopyingPersistentBiMap<>(newSelf, newInv);
    } else {
      return this;
    }
  }

  @Override
  public void clear() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public V put(K pArg0, V pArg1) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> pArg0) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object pArg0) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsKey(Object key) {
    return self.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return inv.containsKey(value);
  }

  @Override
  public V get(Object key) {
    return self.get(key);
  }

  @Override
  public boolean isEmpty() {
    return self.isEmpty();
  }

  @Override
  public int size() {
    return self.size();
  }

  @Override
  public Collection<V> values() {
    return self.values();
  }

  @Override
  public Comparator<? super K> comparator() {
    return self.comparator();
  }

  @Override
  public K firstKey() {
    return self.firstKey();
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return self.headMap(toKey);
  }

  @Override
  public K lastKey() {
    return self.lastKey();
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return self.subMap(fromKey, toKey);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return self.tailMap(fromKey);
  }

  @Override
  public PersistentBiMap<V, K> inverse() {
    return new PathCopyingPersistentBiMap<>(inv, self);
  }

  @Override
  public String toString() {
    return "{" + FluentIterable.from(self.entrySet())
        .transform(new Function<Entry<K, V>, String>() {
          @Override
          public String apply(java.util.Map.Entry<K, V> entry) {
            return entry.getKey() + ": " + entry.getValue();
          }
        }).join(Joiner.on(", ")) + "}";
  }
}