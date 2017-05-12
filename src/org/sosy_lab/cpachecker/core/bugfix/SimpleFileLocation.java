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
package org.sosy_lab.cpachecker.core.bugfix;

import com.google.common.base.Objects;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * A simplified file location structure, which only contains core information.
 */
public class SimpleFileLocation {

  private final String fileName;
  private final int offset;
  private final int length;

  public SimpleFileLocation(String pFileName, int pOffset, int pLength) {
    fileName = pFileName;
    offset = pOffset;
    length = pLength;
  }

  public static final SimpleFileLocation DUMMY = new SimpleFileLocation("<none>", 0, 0);

  @Override
  public int hashCode() {
    return Objects.hashCode(fileName, offset, length);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof SimpleFileLocation)) {
      return false;
    }
    SimpleFileLocation that = (SimpleFileLocation) obj;
    return Objects.equal(fileName, that.fileName) && offset == that.offset && length == that.length;
  }

  public static SimpleFileLocation from(FileLocation loc) {
    if (loc == null) {
      return DUMMY;
    }
    return new SimpleFileLocation(loc.getFileName(), loc.getNodeOffset(), loc.getNodeLength());
  }

  public static SimpleFileLocation from(IASTFileLocation loc) {
    if (loc == null) {
      return DUMMY;
    }
    return new SimpleFileLocation(loc.getFileName(), loc.getNodeOffset(), loc.getNodeLength());
  }

}
