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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;

import java.util.Collection;

/**
 * A transfer relation with internal store of error reports.
 * Only transfer relation of wrapper CPA should implement this interface.
 */
public interface TransferRelationWithErrorStore extends TransferRelation {

  /**
   * Get the stored error reports
   *
   * @return the collection of error reports
   */
  Collection<ErrorReport> getStoredErrorReports();

  /**
   * Clear all stored error reports
   */
  void resetErrorReports();

}
