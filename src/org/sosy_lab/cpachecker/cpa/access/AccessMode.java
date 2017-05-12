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
package org.sosy_lab.cpachecker.cpa.access;


/**
 * Access mode of a variable / accesspath (a.x, a.p.q)
 *
 * It is mutable
 */
public class AccessMode {
  // bit flags
  public static final byte READ_BIT = 1;
  public static final byte WRITE_BIT = 2;
  // bottom and top
  public static final byte NO_ACCESS = 0;
  public static final byte READ_WRITE = READ_BIT | WRITE_BIT;
  // unknown state, e.g., function implementation is not avaiable.
  public static final byte UNKNOWN = 4;

  private byte mode;

  public AccessMode(byte pMode) {
    super();
    mode = pMode;
  }

  public boolean hasRead() {
    return (mode & READ_BIT) != 0;
  }

  public boolean hasWrite() {
    return (mode & WRITE_BIT) != 0;
  }

  public boolean isUnknown() {
    return (mode & UNKNOWN) != 0;
  }

  public void addRead() {
    mode |= READ_BIT;
  }

  public void addWrite() {
    mode |= WRITE_BIT;
  }

  @Override
  public String toString() {
    if (isUnknown()) {
      return "*";
    } else {
      return (hasRead() ? "R" : "") + (hasWrite() ? "W" : "");
    }
  }

  public static AccessMode join(AccessMode x, AccessMode y) {
    if (x.isUnknown()) {
      return y.copyOf();
    } else if (y.isUnknown()) {
      return x.copyOf();
    } else {
      return new AccessMode((byte) (x.mode | y.mode));
    }
  }

  public AccessMode copyOf() {
    return new AccessMode(mode);
  }
}
