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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

import java.util.List;

public abstract class AbstractCPAFactory implements CPAFactory {

  private LogManager logger = null;
  private Configuration configuration = null;
  private ShutdownNotifier shutdownNotifier = null;

  @Override
  public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot wrap CPA");
  }

  @Override
  public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot wrap CPAs");
  }

  @Override
  public CPAFactory setConfiguration(Configuration pConfiguration) {
    Preconditions.checkNotNull(pConfiguration);
    Preconditions.checkState(configuration == null, "setConfiguration called twice on CPAFactory");

    configuration = pConfiguration;
    return this;
  }

  @Override
  public CPAFactory setLogger(LogManager pLogger) {
    Preconditions.checkNotNull(pLogger);
    Preconditions.checkState(logger == null, "setLogger called twice on CPAFactory");

    logger = pLogger;
    return this;
  }

  @Override
  public CPAFactory setShutdownNotifier(ShutdownNotifier pShutdownNotifier) {
    Preconditions.checkNotNull(pShutdownNotifier);
    Preconditions
        .checkState(shutdownNotifier == null, "setShutdownNotifier called twice on CPAFactory");

    shutdownNotifier = pShutdownNotifier;
    return this;
  }

  protected LogManager getLogger() {
    Preconditions.checkState(logger != null, "LogManager object needed to create CPA");
    return logger;
  }

  protected Configuration getConfiguration() {
    Preconditions.checkState(configuration != null, "Configuration object needed to create CPA");
    return configuration;
  }

  public ShutdownNotifier getShutdownNotifier() {
    Preconditions
        .checkState(shutdownNotifier != null, "ShutdownNotifier object needed to create CPA");
    return shutdownNotifier;
  }

  @Override
  public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
    // ignore other objects
    return this;
  }
}
