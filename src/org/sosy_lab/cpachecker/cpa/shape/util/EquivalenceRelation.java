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
package org.sosy_lab.cpachecker.cpa.shape.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;

public class EquivalenceRelation<T extends Comparable<? super T>> {

  private TreeMap<T, EquivalenceClass<T>> value2Class;

  private EquivalenceRelation() {
    value2Class = Maps.newTreeMap();
  }

  public static <T extends Comparable<? super T>> EquivalenceRelation<T> of() {
    return new EquivalenceRelation<>();
  }

  /**
   * Add equality relation between v1 and v2.
   * Invariant: each equivalence class should contain at least 2 elements.
   *
   * @return whether the relation already exists
   */
  public boolean addRelation(T v1, T v2) {
    EquivalenceClass<T> t1 = value2Class.get(v1);
    EquivalenceClass<T> t2 = value2Class.get(v2);
    int flag = 2 * (t1 == null ? 0 : 1) + (t2 == null ? 0 : 1);
    switch (flag) {
      case 0: {
        EquivalenceClass<T> closure = new EquivalenceClass<>();
        closure.add(v1);
        closure.add(v2);
        value2Class.put(v1, closure);
        value2Class.put(v2, closure);
        break;
      }
      case 1: {
        assert (t2 != null);
        t2.add(v1);
        value2Class.put(v1, t2);
        // here there is only one copy of equivalence class, thus the modification will effect
        // everywhere the class is used
        break;
      }
      case 2: {
        assert (t1 != null);
        t1.add(v2);
        value2Class.put(v2, t1);
        break;
      }
      default: {
        assert (t1 != null);
        assert (t2 != null);
        if (t1 == t2) {
          return true;
        } else {
          t1.addAll(t2);
          value2Class.put(v2, t1);
        }
      }
    }
    return false;
  }

  /**
   * To prevent inconsistency issue, we reset the original equivalence relation and then perform
   * copying operation.
   */
  public void addAll(EquivalenceRelation<T> that) {
    value2Class.clear();
    value2Class.putAll(that.value2Class);
  }

  private void addClass(EquivalenceClass<T> pClass) {
    assert !pClass.isSingleton();
    for (T element : pClass.values) {
      value2Class.put(element, pClass);
    }
  }

  /**
   * Examine the equivalence of tV under current equivalence relation and oV of other equivalence
   * relation given as the second parameter of the method.
   */
  public boolean isEq(T tV, EquivalenceRelation<T> otherEq, T oV) {
    Set<T> thisValues = getEquivalentValues(tV);
    Set<T> thatValues = otherEq.getEquivalentValues(oV);
    return !Sets.intersection(thisValues, thatValues).isEmpty();
  }

  public Set<T> getEquivalentValues(T v) {
    Set<T> values = new TreeSet<>();
    EquivalenceClass<T> eqClass = value2Class.get(v);
    if (eqClass == null) {
      values.add(v);
    } else {
      values.addAll(eqClass.values);
    }
    return values;
  }

  @Override
  public int hashCode() {
    return value2Class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof EquivalenceRelation)) {
      return false;
    }
    EquivalenceRelation<?> that = (EquivalenceRelation<?>) obj;
    return this.value2Class.equals(that.value2Class);
  }

  /**
   * Merge two equivalence relations.
   * Note: if two relations have different sets of symbols, then the symbol set for merged
   * relation should be the intersection of them. For example:
   * R1: (1,2), (3)
   * R2: (2), (3,4)
   * Then, R1 and R2 are merged into R: (2), (3)
   * ATTENTION: when performing state merge, it is impossible that an abstract value is
   * introduced in the merged equivalence relation. Otherwise, consider v1 ~ e and v2 ~ e such
   * that (v1, v2) -> t. Thus, the result of merging v1 and v2 should be e instead of a fresh
   * value t. That was a contradiction.
   *
   * @param that one equivalence relation to be merged
   * @return merged equivalence relation from this and that
   */
  public EquivalenceRelation<T> merge(EquivalenceRelation<T> that) {
    TreeSet<T> thisKeys = new TreeSet<>(value2Class.keySet());
    TreeSet<T> thatKeys = new TreeSet<>(that.value2Class.keySet());
    Set<T> commonKeys = new TreeSet<>(Sets.intersection(thisKeys, thatKeys));
    EquivalenceRelation<T> result = new EquivalenceRelation<>();
    while (!commonKeys.isEmpty()) {
      T commonKey = commonKeys.iterator().next();
      EquivalenceClass<T> c1 = this.value2Class.get(commonKey);
      EquivalenceClass<T> c2 = that.value2Class.get(commonKey);
      assert (c1 != null && c2 != null);
      EquivalenceClass<T> c = c1.intersection(c2);
      if (c.isSingleton()) {
        // we do not add this key to the merged equivalence relation, since each equivalence
        // relation requires to contain at least 2 elements
        commonKeys.remove(commonKey);
      } else {
        result.addClass(c);
        for (T element : c.values) {
          commonKeys.remove(element);
        }
      }
    }
    return result;
  }

  /**
   * The less_or_equal relation holds only when, each equivalence pair (v1, v2) from `that`
   * should appear in `this`.
   */
  public boolean isLessOrEqual(EquivalenceRelation<T> that) {
    Set<T> thatKeys = new TreeSet<>(that.value2Class.keySet());
    for (T thatKey : thatKeys) {
      EquivalenceClass<T> thatClass = that.value2Class.get(thatKey);
      EquivalenceClass<T> thisClass = this.value2Class.get(thatKey);
      if (thisClass == null) {
        return false;
      }
      if (!thatClass.isContainedBy(thisClass)) {
        return false;
      }
    }
    return true;
  }

  public T getRepresentative(T v) {
    EquivalenceClass<T> t = value2Class.get(v);
    if (t == null) {
      return v;
    } else {
      return t.getRepresentative();
    }
  }

  public Collection<T> getRepresentatives() {
    Set<T> elements = new TreeSet<>();
    for (EquivalenceClass<T> ec : value2Class.values()) {
      elements.add(ec.getRepresentative());
    }
    return Collections.unmodifiableCollection(elements);
  }

  private class EquivalenceClass<K extends Comparable<? super K>> {

    private TreeSet<K> values;

    EquivalenceClass() {
      values = Sets.newTreeSet();
    }

    void add(K element) {
      values.add(element);
    }

    void addAll(EquivalenceClass<K> pClass) {
      values.addAll(pClass.values);
    }

    EquivalenceClass<K> intersection(EquivalenceClass<K> that) {
      EquivalenceClass<K> newClass = new EquivalenceClass<>();
      newClass.values.addAll(Sets.intersection(this.values, that.values));
      return newClass;
    }

    boolean isContainedBy(@Nonnull EquivalenceClass<K> pClass) {
      return pClass.values.containsAll(this.values);
    }

    boolean isSingleton() {
      return values.size() == 1;
    }

    boolean isEmpty() {
      return values.isEmpty();
    }

    K getRepresentative() {
      return values.first();
    }

  }

}
