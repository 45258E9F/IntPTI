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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.solver.SolverException;

@Options(prefix = "cpa.predicate")
public class PredicateAbstractDomain implements AbstractDomain {

  @Option(secure = true, description = "whether to include the symbolic path formula in the "
      + "coverage checks or do only the fast abstract checks")
  private boolean symbolicCoverageCheck = false;

  // statistics
  public final Timer coverageCheckTimer = new Timer();
  public final Timer bddCoverageCheckTimer = new Timer();
  public final Timer symbolicCoverageCheckTimer = new Timer();

  private final PredicateAbstractionManager mgr;

  public PredicateAbstractDomain(Configuration config, PredicateAbstractionManager pPredAbsManager)
      throws InvalidConfigurationException {
    config.inject(this, PredicateAbstractDomain.class);
    mgr = pPredAbsManager;
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState element1,
      AbstractState element2) throws CPAException, InterruptedException {
    coverageCheckTimer.start();
    try {

      PredicateAbstractState e1 = (PredicateAbstractState) element1;
      PredicateAbstractState e2 = (PredicateAbstractState) element2;

      // TODO time statistics (previously in formula manager)
    /*
  long start = System.currentTimeMillis();
  entails(f1, f2);
  long end = System.currentTimeMillis();
  stats.bddCoverageCheckMaxTime = Math.max(stats.bddCoverageCheckMaxTime,
      (end - start));
  stats.bddCoverageCheckTime += (end - start);
  ++stats.numCoverageChecks;
     */

      if (e1.isAbstractionState() && e2.isAbstractionState()) {
        bddCoverageCheckTimer.start();

        // if e1's predicate abstraction entails e2's pred. abst.
        boolean result = mgr.checkCoverage(e1.getAbstractionFormula(), e2.getAbstractionFormula());

        bddCoverageCheckTimer.stop();
        return result;

      } else if (e2.isAbstractionState()) {
        if (symbolicCoverageCheck) {
          symbolicCoverageCheckTimer.start();

          boolean result = mgr.checkCoverage(e1.getAbstractionFormula(), e1.getPathFormula(),
              e2.getAbstractionFormula());

          symbolicCoverageCheckTimer.stop();
          return result;

        } else {
          return false;
        }

      } else if (e1.isAbstractionState()) {
        return false;

      } else {
        // only the fast check which returns true if a merge occurred for this element
        return e1.getMergedInto() == e2;
      }

    } catch (SolverException e) {
      throw new CPAException("Solver Exception", e);
    } finally {
      coverageCheckTimer.stop();
    }
  }

  @Override
  public AbstractState join(
      AbstractState pElement1,
      AbstractState pElement2) throws CPAException {
    throw new UnsupportedOperationException();
  }
}