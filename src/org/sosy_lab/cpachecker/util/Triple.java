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
package org.sosy_lab.cpachecker.util;

import com.google.common.base.Function;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A generic Triple class based on Pair.java.
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
public class Triple<A, B, C> implements Serializable {

  private static final long serialVersionUID = 1272029955865151903L;

  private final
  @Nullable
  A first;
  private final
  @Nullable
  B second;
  private final
  @Nullable
  C third;

  private Triple(@Nullable A first, @Nullable B second, @Nullable C third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public static <A, B, C> Triple<A, B, C> of(
      @Nullable A first, @Nullable B second, @Nullable C third) {
    return new Triple<>(first, second, third);
  }

  public final
  @Nullable
  A getFirst() {
    return first;
  }

  public final
  @Nullable
  B getSecond() {
    return second;
  }

  public final
  @Nullable
  C getThird() {
    return third;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ", " + third + ")";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof Triple<?, ?, ?>)
        && Objects.equals(first, ((Triple<?, ?, ?>) other).first)
        && Objects.equals(second, ((Triple<?, ?, ?>) other).second)
        && Objects.equals(third, ((Triple<?, ?, ?>) other).third);
  }

  @Override
  public int hashCode() {
    if (first == null && second == null) {
      return (third == null) ? 0 : third.hashCode() + 1;
    } else if (first == null && third == null) {
      return second.hashCode() + 2;
    } else if (first == null) {
      return second.hashCode() * 7 + third.hashCode();
    } else if (second == null && third == null) {
      return first.hashCode() + 3;
    } else if (second == null) {
      return first.hashCode() * 11 + third.hashCode();
    } else if (third == null) {
      return first.hashCode() * 13 + second.hashCode();
    } else {
      return first.hashCode() * 17 + second.hashCode() * 5 + third.hashCode();
    }
  }

  public static <T> Function<Triple<? extends T, ?, ?>, T> getProjectionToFirst() {
    return Holder.<T>getInstance().PROJECTION_TO_FIRST;
  }

  public static <T> Function<Triple<?, ? extends T, ?>, T> getProjectionToSecond() {
    return Holder.<T>getInstance().PROJECTION_TO_SECOND;
  }

  public static <T> Function<Triple<?, ?, ? extends T>, T> getProjectionToThird() {
    return Holder.<T>getInstance().PROJECTION_TO_THIRD;
  }

  /*
   * Static holder class for several function objects because if these fields
   * were static fields of the Triple class, they couldn't be generic.
   */
  @SuppressWarnings("membername") // members are in effect static final constantss
  private static final class Holder<T> {

    private static final Holder<?> INSTANCE = new Holder<Void>();

    // Cast is safe because class has no mutable state
    @SuppressWarnings("unchecked")
    public static <T> Holder<T> getInstance() {
      return (Holder<T>) INSTANCE;
    }

    private final Function<Triple<? extends T, ?, ?>, T> PROJECTION_TO_FIRST =
        new Function<Triple<? extends T, ?, ?>, T>() {
          @Override
          public T apply(@Nonnull Triple<? extends T, ?, ?> pArg0) {
            return pArg0.getFirst();
          }
        };

    private final Function<Triple<?, ? extends T, ?>, T> PROJECTION_TO_SECOND =
        new Function<Triple<?, ? extends T, ?>, T>() {
          @Override
          public T apply(@Nonnull Triple<?, ? extends T, ?> pArg0) {
            return pArg0.getSecond();
          }
        };

    private final Function<Triple<?, ?, ? extends T>, T> PROJECTION_TO_THIRD =
        new Function<Triple<?, ?, ? extends T>, T>() {
          @Override
          public T apply(@Nonnull Triple<?, ?, ? extends T> pArg0) {
            return pArg0.getThird();
          }
        };
  }
}
