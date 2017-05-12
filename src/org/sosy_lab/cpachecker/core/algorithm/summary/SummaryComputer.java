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
package org.sosy_lab.cpachecker.core.algorithm.summary;

import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryStore;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.List;
import java.util.Set;

/**
 * Performs the following functionalities:
 * 1. Provides summary subjects;
 * 2. Resolve dependencies among summary subjects;
 * 3. Perform computation for a single summary subject;
 */
public interface SummaryComputer {
  // all subjects
  Set<SummarySubject> getSubjects();

  // dependers
  Set<SummarySubject> getDepender(SummarySubject subject);

  // dependees
  Set<SummarySubject> getDependee(SummarySubject subject);

  // computer summary for the given subject
  Set<SummarySubject> computeFor(SummarySubject subject) throws Exception;

  // return whether depended summaries needs to be re computed?
  boolean update(SummarySubject subject, SummaryInstance instance);

  // set the initial summary for subject
  void initSummary();

  // build the summary to a summary store, always return non-null
  List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> build();

  void preAction();

  void postAction();

}
