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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;

@Options(prefix = "pcc.partial")
public class PartialCertificateTypeProvider {

  public enum PartialCertificateTypes {
    ALL,
    HEURISTIC,
    ARG,
    MONOTONESTOPARG
  }

  private final boolean withCMC;

  @Option(secure = true,
      description = "Selects the strategy used for partial certificate construction")
  private PartialCertificateTypes certificateType = PartialCertificateTypes.HEURISTIC;

  public PartialCertificateTypeProvider(
      final Configuration pConfig,
      final boolean pHeuristicAllowed)
      throws InvalidConfigurationException {
    this(pConfig, pHeuristicAllowed, false);
  }

  public PartialCertificateTypeProvider(
      final Configuration pConfig, final boolean pHeuristicAllowed,
      final boolean partialCertificateForCMC) throws InvalidConfigurationException {
    pConfig.inject(this);
    if (!pHeuristicAllowed && certificateType == PartialCertificateTypes.HEURISTIC) {
      certificateType = PartialCertificateTypes.ARG;
    }
    withCMC = partialCertificateForCMC;
  }

  public PartialReachedConstructionAlgorithm getPartialCertificateConstructor() {
    return getPartialCertificateConstructor(false);
  }

  private PartialReachedConstructionAlgorithm getPartialCertificateConstructor(boolean pKeepARGState) {
    switch (certificateType) {
      case ARG:
        return new ARGBasedPartialReachedSetConstructionAlgorithm(pKeepARGState);
      case MONOTONESTOPARG:
        return new MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm(
            pKeepARGState, withCMC);
      default:// HEURISTIC
        return new HeuristicPartialReachedSetConstructionAlgorithm();
    }
  }

  public PartialReachedConstructionAlgorithm getCertificateConstructor() {
    if (certificateType == PartialCertificateTypes.ALL) {
      return new CompleteCertificateConstructionAlgorithm();
    }
    return getPartialCertificateConstructor(true);
  }

}
