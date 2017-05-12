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
package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Objects;

public class FileLocation {

  private final int endingLine;
  private final String fileName;
  private final String niceFileName;
  private final int length;
  private final int offset;
  private final int startingLine;
  private final int startingLineInOrigin;

  public FileLocation(
      int pEndingLine, String pFileName, int pLength,
      int pOffset, int pStartingLine) {
    this(pEndingLine, pFileName, pFileName, pLength, pOffset, pStartingLine, pStartingLine);
  }

  public FileLocation(
      int pEndingLine, String pFileName, String pNiceFileName,
      int pLength, int pOffset, int pStartingLine, int pStartingLineInOrigin) {
    endingLine = pEndingLine;
    fileName = checkNotNull(pFileName);
    niceFileName = checkNotNull(pNiceFileName);
    length = pLength;
    offset = pOffset;
    startingLine = pStartingLine;
    startingLineInOrigin = pStartingLineInOrigin;
  }

  public static final FileLocation DUMMY = new FileLocation(0, "<none>", 0, 0, 0) {
    @Override
    public String toString() {
      return "none";
    }
  };

  public static final FileLocation MULTIPLE_FILES =
      new FileLocation(0, "<multiple files>", 0, 0, 0) {
        @Override
        public String toString() {
          return getFileName();
        }
      };

  public static FileLocation merge(List<FileLocation> locations) {
    checkArgument(!Iterables.isEmpty(locations));

    String fileName = null;
    String niceFileName = null;
    int startingLine = Integer.MAX_VALUE;
    int startingLineInOrigin = Integer.MAX_VALUE;
    int endingLine = Integer.MIN_VALUE;
    for (FileLocation loc : locations) {
      if (loc == DUMMY) {
        continue;
      }
      if (fileName == null) {
        fileName = loc.fileName;
        niceFileName = loc.niceFileName;
      } else if (!fileName.equals(loc.fileName)) {
        return MULTIPLE_FILES;
      }

      startingLine = Math.min(startingLine, loc.getStartingLineNumber());
      startingLineInOrigin = Math.min(startingLineInOrigin, loc.getStartingLineInOrigin());
      endingLine = Math.max(endingLine, loc.getEndingLineNumber());
    }

    if (fileName == null) {
      // only DUMMY elements
      return DUMMY;
    }
    return new FileLocation(endingLine, fileName, niceFileName, 0, 0, startingLine,
        startingLineInOrigin);
  }

  public int getStartingLineInOrigin() {
    return startingLineInOrigin;
  }

  public int getEndingLineNumber() {
    return endingLine;
  }

  public String getFileName() {
    return fileName;
  }

  public int getNodeLength() {
    return length;
  }

  public int getNodeOffset() {
    return offset;
  }

  public int getStartingLineNumber() {
    return startingLine;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + endingLine;
    result = prime * result + Objects.hashCode(fileName);
    result = prime * result + length;
    result = prime * result + offset;
    result = prime * result + startingLine;
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof FileLocation)) {
      return false;
    }

    FileLocation other = (FileLocation) obj;

    return other.endingLine == endingLine
        && other.startingLine == startingLine
        && other.length == length
        && other.offset == offset
        && Objects.equals(other.fileName, fileName);
  }

  @Override
  public String toString() {
    String prefix = niceFileName.isEmpty()
                    ? ""
                    : niceFileName + ", ";
    if (startingLine == endingLine) {
      return prefix + "line " + startingLineInOrigin;
    } else {
      // TODO ending line number could be wrong
      return prefix + "lines " + startingLineInOrigin + "-" + (endingLine - startingLine
          + startingLineInOrigin);
    }
  }
}
