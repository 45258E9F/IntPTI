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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;


public class CompleteCertificateConstructionAlgorithm
    implements PartialReachedConstructionAlgorithm {

  @Override
  public AbstractState[] computePartialReachedSet(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    return pReached.asCollection().toArray(new AbstractState[pReached.size()]);
  }

}
