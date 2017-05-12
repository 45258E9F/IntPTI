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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.solver.api.Formula;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Expression {
  public static abstract class Location extends Expression {
    public static class AliasedLocation extends Location {

      private AliasedLocation(final Formula address) {
        this.address = address;
      }

      public Formula getAddress() {
        return address;
      }

      @Override
      public Kind getKind() {
        return Kind.ALIASED_LOCATION;
      }

      @Override
      public String toString() {
        return toStringHelper(this)
            .add("address", address)
            .toString();
      }

      private final Formula address;
    }

    public static class UnaliasedLocation extends Location {

      private UnaliasedLocation(final String variableName) {
        this.variableName = variableName;
      }

      public String getVariableName() {
        return variableName;
      }

      @Override
      public Kind getKind() {
        return Kind.UNALIASED_LOCATION;
      }

      @Override
      public String toString() {
        return toStringHelper(this)
            .add("variable", variableName)
            .toString();
      }

      private final String variableName;
    }

    public static AliasedLocation ofAddress(final @Nonnull Formula address) {
      return new AliasedLocation(address);
    }

    public static UnaliasedLocation ofVariableName(final @Nonnull String variableName) {
      return new UnaliasedLocation(variableName);
    }

    public boolean isAliased() {
      return this instanceof AliasedLocation;
    }

    public AliasedLocation asAliased() {
      if (this instanceof AliasedLocation) {
        return (AliasedLocation) this;
      } else {
        return null;
      }
    }

    public UnaliasedLocation asUnaliased() {
      if (this instanceof UnaliasedLocation) {
        return (UnaliasedLocation) this;
      } else {
        return null;
      }
    }
  }

  public static class Value extends Expression {
    public static class Nondet extends Value {
      private Nondet() {
        super(null);
      }

      @Override
      public Formula getValue() {
        return null;
      }

      @Override
      public boolean isNondet() {
        return true;
      }

      @Override
      public Kind getKind() {
        return Kind.NONDET;
      }

      @Override
      public String toString() {
        return toStringHelper(this)
            .toString();
      }
    }

    private Value(final Formula value) {
      this.value = value;
    }

    public Formula getValue() {
      return value;
    }

    public boolean isNondet() {
      return false;
    }

    @Override
    public Kind getKind() {
      return Kind.DET_VALUE;
    }

    @Override
    public String toString() {
      return toStringHelper(this)
          .add("value", value)
          .toString();
    }

    @Override
    public boolean equals(Object pOther) {
      if (!(pOther instanceof Value)) {
        return false;
      }
      Value otherValue = (Value) pOther;
      if (this instanceof Nondet || otherValue instanceof Nondet) {
        return false;
      }

      return getValue().equals(otherValue.getValue());
    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
    }

    private final Formula value;
    private static final Value nondet = new Nondet();
  }

  public static Value ofValue(final @Nullable Formula value) {
    return value != null ? new Value(value) : null;
  }

  public static Value nondetValue() {
    return Value.nondet;
  }

  public boolean isLocation() {
    return this instanceof Location;
  }

  public boolean isValue() {
    return this instanceof Value;
  }

  public boolean isNondetValue() {
    return this == Value.nondet;
  }

  public boolean isAliasedLocation() {
    return this.isLocation() && this.asLocation().isAliased();
  }

  public boolean isUnaliasedLocation() {
    return this.isLocation() && !this.asLocation().isAliased();
  }

  public Location asLocation() {
    if (this instanceof Location) {
      return (Location) this;
    } else {
      return null;
    }
  }

  public AliasedLocation asAliasedLocation() {
    if (this.isLocation()) {
      return this.asLocation().asAliased();
    } else {
      return null;
    }
  }

  public UnaliasedLocation asUnaliasedLocation() {
    if (this.isLocation()) {
      return this.asLocation().asUnaliased();
    } else {
      return null;
    }
  }

  public Value asValue() {
    if (this instanceof Value) {
      return (Value) this;
    } else {
      return null;
    }
  }

  public abstract Kind getKind();

  public enum Kind {
    ALIASED_LOCATION,
    UNALIASED_LOCATION,
    DET_VALUE,
    NONDET
  }
}
