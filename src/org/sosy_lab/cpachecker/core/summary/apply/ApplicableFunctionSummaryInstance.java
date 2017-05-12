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
package org.sosy_lab.cpachecker.core.summary.apply;

import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;

/**
 * Apply summary to something, e.g.,
 *
 * summary:   x = y + 1;
 * knowledge: x -> SYM1, y -> SYM2
 * result:    SYM1 = SYM2 + 1;
 *
 * @param R: summary result
 */
public interface ApplicableFunctionSummaryInstance<R> extends SummaryInstance {

  R apply();

}
