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
package org.sosy_lab.cpachecker.pcc.strategy;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public abstract class SequentialReadStrategy extends AbstractStrategy {

  public SequentialReadStrategy(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
  }

  @Override
  protected void writeProofToStream(ObjectOutputStream out, UnmodifiableReachedSet reached)
      throws IOException, InvalidConfigurationException {
    out.writeObject(getProofToWrite(reached));
  }

  @Override
  protected void readProofFromStream(ObjectInputStream in)
      throws ClassNotFoundException, InvalidConfigurationException, IOException {
    prepareForChecking(in.readObject());
  }

  protected abstract Object getProofToWrite(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException;

  protected abstract void prepareForChecking(Object pReadObject)
      throws InvalidConfigurationException;
}
