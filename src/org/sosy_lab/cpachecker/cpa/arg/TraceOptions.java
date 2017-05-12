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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options
public class TraceOptions {

  @Option(secure = true, name = "trace.depth", description = "the depth limitation of error trace")
  private int maxDepth = 1000;

  @Option(secure = true, name = "trace.upperBound", description = "the maximum number of paths "
      + "allowed in analysis")
  private long maxNumOfPath = 0;

  public int getMaximumPathDepth() {
    return maxDepth;
  }

  public long getMaxNumOfPath() {
    return maxNumOfPath;
  }

}
