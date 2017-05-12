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
package org.sosy_lab.cpachecker.util.states;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.PointerVisitor;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * This class describes a location in the memory.
 */
public class MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  private static final long serialVersionUID = -8910967707373729034L;
  private final String functionName;
  private final String identifier;
  private final Optional<Long> offset;

  /**
   * This function can be used to {@link com.google.common.collect.Iterables#transform transform}
   * a collection of {@link String}s to a collection of {@link MemoryLocation}s, representing the
   * respective memory location of the identifiers.
   */
  public static final Function<String, MemoryLocation> FROM_STRING_TO_MEMORYLOCATION =
      new Function<String, MemoryLocation>() {
        @Override
        public MemoryLocation apply(String variableName) {
          return MemoryLocation.valueOf(variableName);
        }
      };

  /**
   * This function can be used to {@link com.google.common.collect.Iterables#transform transform} a
   * collection of {@link MemoryLocation}s
   * to a collection of {@link String}s, representing the respective variable identifiers.
   */
  public static final Function<MemoryLocation, String> FROM_MEMORYLOCATION_TO_STRING =
      new Function<MemoryLocation, String>() {
        @Override
        public String apply(MemoryLocation memoryLocation) {
          return memoryLocation.getAsSimpleString();
        }
      };

  /**
   * NULL object in the C memory model.
   */
  public static final MemoryLocation NULL_OBJECT = new MemoryLocation("!NULL!", Optional
      .<Long>absent());

  private MemoryLocation(String pFunctionName, String pIdentifier, Optional<Long> pOffset) {
    checkNotNull(pFunctionName);
    checkNotNull(pIdentifier);

    functionName = pFunctionName;
    identifier = pIdentifier;
    offset = pOffset;
  }

  private MemoryLocation(String pIdentifier, Optional<Long> pOffset) {
    checkNotNull(pIdentifier);

    int separatorIndex = pIdentifier.indexOf("::");
    if (separatorIndex >= 0) {
      functionName = pIdentifier.substring(0, separatorIndex);
      identifier = pIdentifier.substring(separatorIndex + 2);
    } else {
      functionName = null;
      identifier = pIdentifier;
    }
    offset = pOffset;
  }

  private MemoryLocation(MemoryLocation location, Long pOffset) {
    this.functionName = location.functionName;
    this.identifier = location.identifier;
    this.offset = Optional.of(pOffset);
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (!(other instanceof MemoryLocation)) {
      return false;
    }

    MemoryLocation otherLocation = (MemoryLocation) other;

    return Objects.equals(functionName, otherLocation.functionName)
        && Objects.equals(identifier, otherLocation.identifier)
        && offset.equals(otherLocation.offset);
  }

  @Override
  public int hashCode() {

    int hc = 17;
    int hashMultiplier = 59;

    hc = hc * hashMultiplier + Objects.hashCode(functionName);
    hc = hc * hashMultiplier + identifier.hashCode();
    hc = hc * hashMultiplier + offset.hashCode();

    return hc;
  }

  public static MemoryLocation valueOf(String pFunctionName, String pIdentifier) {
    return new MemoryLocation(pFunctionName, pIdentifier, Optional.<Long>absent());
  }

  public static MemoryLocation valueOf(String pFunctionName, String pIdentifier, long pOffest) {
    return new MemoryLocation(pFunctionName, pIdentifier, Optional.of(pOffest));
  }

  public static MemoryLocation valueOf(String pIdentifier, long pOffset) {
    return new MemoryLocation(pIdentifier, Optional.of(pOffset));
  }

  public static MemoryLocation valueOf(String pVariableName) {

    // if the variable name starts with "LITERAL:", we treat it as a memory location without offset
    if (pVariableName.startsWith(PointerVisitor.LITERAL_PREFIX)) {
      return new MemoryLocation(pVariableName, Optional.<Long>absent());
    }

    String[] nameParts = pVariableName.split("::");
    String[] offsetParts = pVariableName.split("/");

    boolean isScoped = nameParts.length == 2;
    boolean hasOffset = offsetParts.length == 2;

    Optional<Long> offset = hasOffset ? Optional.of(Long.parseLong(offsetParts[1])) :
                            Optional.<Long>absent();

    if (isScoped) {
      if (hasOffset) {
        nameParts[1] = nameParts[1].replace("/" + offset.get(), "");
      }
      return new MemoryLocation(nameParts[0], nameParts[1], offset);

    } else {
      if (hasOffset) {
        nameParts[0] = nameParts[0].replace("/" + offset.get(), "");
      }
      return new MemoryLocation(nameParts[0].replace("/" + offset, ""), offset);
    }
  }

  public static MemoryLocation valueOf(MemoryLocation oldLocation, long pOffset) {
    return new MemoryLocation(oldLocation, pOffset);
  }

  public static MemoryLocation withOffset(MemoryLocation oldLocation, long pOffset) {
    if (pOffset == 0) {
      return oldLocation;
    } else {
      long newOffset;
      if (oldLocation.offset.isPresent()) {
        newOffset = oldLocation.offset.get() + pOffset;
      } else {
        newOffset = pOffset;
      }
      return new MemoryLocation(oldLocation, newOffset);
    }
  }

  public String getAsSimpleString() {
    String variableName = isOnFunctionStack() ? (functionName + "::" + identifier) : (identifier);
    if (!offset.isPresent()) {
      return variableName;
    }
    return variableName + "/" + offset.get();
  }

  public String getDeclaredName() {
    return isOnFunctionStack() ? (functionName + "::" + identifier) : identifier;
  }

  public String serialize() {
    return getAsSimpleString();
  }

  public boolean isOnFunctionStack() {
    return functionName != null;
  }

  public boolean isOnFunctionStack(String pFunctionName) {
    return functionName != null && pFunctionName.equals(functionName);
  }

  public String getFunctionName() {
    return checkNotNull(functionName);
  }

  public String getIdentifier() {
    return identifier;
  }

  public boolean isReference() {
    return offset.isPresent();
  }

  /**
   * Gets the offset of a reference. Only valid for references.
   * See {@link MemoryLocation#isReference()}.
   *
   * @return the offset of a reference.
   */
  public long getOffset() {
    return offset.isPresent() ? offset.get() : 0;
  }

  public static boolean sameMemoryBlock(MemoryLocation loc1, MemoryLocation loc2) {
    String functionName1 = loc1.functionName;
    String functionName2 = loc2.functionName;
    if ((functionName1 == null) != (functionName2 == null)) {
      return false;
    }
    if (functionName1 != null && !functionName1.equals(functionName2)) {
      return false;
    }
    return loc1.getIdentifier().equals(loc2.getIdentifier());
  }

  @Override
  public String toString() {
    return getAsSimpleString();
  }

  public static PersistentMap<MemoryLocation, Long> transform(
      PersistentMap<String, Long> pConstantMap) {

    PersistentMap<MemoryLocation, Long> result = PathCopyingPersistentTreeMap.of();

    for (Map.Entry<String, Long> entry : pConstantMap.entrySet()) {
      result = result.putAndCopy(valueOf(entry.getKey()), checkNotNull(entry.getValue()));
    }

    return result;
  }

  @Override
  public int compareTo(MemoryLocation other) {

    int result = 0;

    if (isOnFunctionStack()) {
      if (other.isOnFunctionStack()) {
        result = functionName.compareTo(other.functionName);
      } else {
        result = 1;
      }
    } else {
      if (other.isOnFunctionStack()) {
        result = -1;
      } else {
        result = 0;
      }
    }

    if (result != 0) {
      return result;
    }

    return ComparisonChain.start()
        .compare(identifier, other.identifier)
        .compare(offset.orNull(), other.offset.orNull(), Ordering.<Long>natural().nullsFirst())
        .result();
  }
}
