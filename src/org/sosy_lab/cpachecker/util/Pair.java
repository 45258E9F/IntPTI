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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ordering.from;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A generic Pair class. Code borrowed from here:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6229146
 *
 * PLEASE DO NOT USE THIS CLASS!
 * It is better design to use proper specific classes
 * that have semantically meaningful names instead of Pair and Triple.
 * There might be cases where usage of such generic classes
 * is understandable, but their mere presence invites to misuse them
 * and introduce non-understandable code using things like
 * Triple<String, String, String>.
 * Thus the general goal is to remove these classes completely,
 * CPAchecker just relies too heavily on them for now.
 *
 * Please do not use these two classes in new code.
 * Either write a custom class with meaningful names,
 * or we start using AutoValue in CPAchecker to generate such classes
 * automatically.
 */
public class Pair<A, B> implements Serializable {

  private static final long serialVersionUID = -8410959888808077296L;

  private final
  @Nullable
  A first;
  private final
  @Nullable
  B second;

  private Pair(@Nullable A first, @Nullable B second) {
    this.first = first;
    this.second = second;
  }

  public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    return new Pair<>(first, second);
  }

  public
  @Nullable
  A getFirst() {
    return first;
  }

  public
  @Nullable
  B getSecond() {
    return second;
  }

  /**
   * Get the first parameter, crash if it is null.
   */
  public A getFirstNotNull() {
    checkNotNull(first);
    return first;
  }

  /**
   * Get the second parameter, crash if it is null.
   */
  public B getSecondNotNull() {
    checkNotNull(second);
    return second;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ")";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof Pair<?, ?>)
        && Objects.equals(first, ((Pair<?, ?>) other).first)
        && Objects.equals(second, ((Pair<?, ?>) other).second);
  }

  @Override
  public int hashCode() {
    if (first == null) {
      return (second == null) ? 0 : second.hashCode() + 1;
    } else if (second == null) {
      return first.hashCode() + 2;
    } else {
      return first.hashCode() * 17 + second.hashCode();
    }
  }

  public static <T> Function<Pair<? extends T, ?>, T> getProjectionToFirst() {
    return Holder.<T>getInstance().PROJECTION_TO_FIRST;
  }

  public static <T> Function<Pair<?, ? extends T>, T> getProjectionToSecond() {
    return Holder.<T>getInstance().PROJECTION_TO_SECOND;
  }

  /*
   * Static holder class for several function objects because if these fields
   * were static fields of the Pair class, they couldn't be generic.
   */
  @SuppressWarnings("membername") // members are in effect static final constants
  private static final class Holder<T> {

    private static final Holder<?> INSTANCE = new Holder<Void>();

    // Cast is safe because class has no state
    @SuppressWarnings("unchecked")
    public static <T> Holder<T> getInstance() {
      return (Holder<T>) INSTANCE;
    }

    private final Function<Pair<? extends T, ?>, T> PROJECTION_TO_FIRST =
        new Function<Pair<? extends T, ?>, T>() {
          @Override
          public T apply(@Nonnull Pair<? extends T, ?> pArg0) {
            return pArg0.getFirst();
          }
        };

    private final Function<Pair<?, ? extends T>, T> PROJECTION_TO_SECOND =
        new Function<Pair<?, ? extends T>, T>() {
          @Override
          public T apply(@Nonnull Pair<?, ? extends T> pArg0) {
            return pArg0.getSecond();
          }
        };
  }

  public static <A, B> List<Pair<A, B>> zipList(
      Collection<? extends A> a, Collection<? extends B> b) {
    List<Pair<A, B>> result = new ArrayList<>(a.size());

    Iterator<? extends A> iteratorA = a.iterator();
    Iterator<? extends B> iteratorB = b.iterator();
    while (iteratorA.hasNext()) {
      checkArgument(iteratorB.hasNext(), "Second list is shorter");

      result.add(Pair.<A, B>of(iteratorA.next(), iteratorB.next()));
    }
    checkArgument(!iteratorB.hasNext(), "Second list is longer");

    return result;
  }

  /**
   * Return a comparator for comparing pairs lexicographically,
   * if their component types define a natural ordering.
   */
  public static <A extends Comparable<? super A>, B extends Comparable<? super B>>
  Ordering<Pair<A, B>> lexicographicalNaturalComparator() {

    return lexicographicalComparator(Ordering.<A>natural(), Ordering.<B>natural());
  }

  /**
   * Return a comparator for comparing pairs lexicographically,
   * delegating the comparison of the components to two comparators.
   */
  public static <A, B> Ordering<Pair<A, B>> lexicographicalComparator(
      Comparator<A> firstOrdering, Comparator<B> secondOrdering) {

    return from(firstOrdering)
        .onResultOf(Pair.<A>getProjectionToFirst())
        .compound(from(secondOrdering).onResultOf(Pair.<B>getProjectionToSecond()));
  }
}
