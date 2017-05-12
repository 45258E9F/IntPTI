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
package org.sosy_lab.cpachecker.core.summary.instance.access;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.util.access.AccessPath;

import java.util.List;

public class AccessResult {

  public final List<AccessPath> reads;
  public final List<AccessPath> writes;

  public AccessResult(List<AccessPath> pReads, List<AccessPath> pWrites) {
    super();
    reads = pReads;
    writes = pWrites;
  }

  public static AccessResult of(List<AccessPath> pReads, List<AccessPath> pWrites) {
    return new AccessResult(pReads, pWrites);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(reads, writes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof AccessResult)) {
      return false;
    }
    AccessResult that = (AccessResult) obj;
    return Objects.equal(this.reads, that.reads) && Objects.equal(this.writes, that.writes);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("AccessResult reads=\n");
    for (AccessPath ap : reads) {
      buffer.append(ap.toString() + "\n");
    }
    buffer.append("AccessResult writes=\n");
    for (AccessPath ap : writes) {
      buffer.append(ap.toString() + "\n");
    }
    return buffer.toString() + "]\n";
  }


}
