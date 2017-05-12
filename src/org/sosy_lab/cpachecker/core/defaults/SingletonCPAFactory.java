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
package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

public class SingletonCPAFactory extends AbstractCPAFactory {

  private final ConfigurableProgramAnalysis instance;

  public SingletonCPAFactory(ConfigurableProgramAnalysis pInstance) {
    instance = checkNotNull(pInstance);
  }

  public static SingletonCPAFactory forInstance(ConfigurableProgramAnalysis pInstance) {
    return new SingletonCPAFactory(pInstance);
  }

  @Override
  public ConfigurableProgramAnalysis createInstance() {
    return instance;
  }

}
