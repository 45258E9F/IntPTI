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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.io.IOException;
import java.util.Collection;

/**
 * Interface for classes implementing proof carrying code approaches. Theses classes can be used by
 * ProofGenerator and ProofCheckAlgorithm.
 */
public interface PCCStrategy {

  /**
   * Constructs the proof/certificate from the given save overapproximation. Subsequently writes the
   * certificate to disk, etc.
   *
   * @param pReached - save overapproximation of state space
   */
  public void writeProof(UnmodifiableReachedSet pReached);

  /**
   * Constructs the proof/certificate from the given save overapproximation and saves it in its
   * internal data structures. Thus, certificate checking can immediately follow analysis, e.g. to
   * check correctness of analysis result.
   *
   * @param pReached - save overapproximation of state space
   * @throws InvalidConfigurationException <ul> <li>if format of abstract state does not match
   *                                       expectation of PCC strategy, configuration of PCC
   *                                       strategy</li> <li>if class does not support direct
   *                                       checking of analysis result</li></ul>
   * @throws InterruptedException          if construction took longer than remaining time available
   *                                       for CPAchecker execution
   */
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, InterruptedException;

  /**
   * Reads the certificate from disk, stream, etc. and stores it internally.
   *
   * @throws IOException                   if reading fails
   * @throws ClassNotFoundException        if reading correct object from stream fails
   * @throws InvalidConfigurationException if format of abstract state does not match expectation of
   *                                       PCC strategy
   */
  public void readProof() throws IOException, ClassNotFoundException, InvalidConfigurationException;

  /**
   * Checks the certificate. The certificate is not given and must be available internally, e.g.
   * because method <code>constructInternalProofRepresentation(UnfmodifableReachedSet)</code> or
   * <code>readProof()</code> has been called before.
   *
   * Checks if the certificate is valid. This means it describes an overapproximation of the
   * reachable state space starting in initial state given by <code>pInitialState</code>.
   * Furthermore, the overapproximation may not violate the considered safety criteria.
   *
   * @param reachedSet - contains initial state and initial precision
   * @return true only if the certificate is valid, returns false if certificate is invalid or
   * validity may not be checked
   * @throws CPAException         if e.g. recomputation of successor or coverage check fails
   * @throws InterruptedException if thread is interrupted while checking
   */
  public boolean checkCertificate(final ReachedSet reachedSet)
      throws CPAException, InterruptedException;

  /**
   * Ask strategy for additional statistics information which should be displayed with statistics of
   * proof generation.
   *
   * @return additional statistics which should be displayed with proof generation statistics
   */
  public Collection<Statistics> getAdditionalProofGenerationStatistics();
}
