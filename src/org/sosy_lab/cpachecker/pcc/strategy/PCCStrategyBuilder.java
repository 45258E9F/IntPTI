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

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class PCCStrategyBuilder {

  private static final String STRATEGY_CLASS_PREFIX = "org.sosy_lab.cpachecker.pcc.strategy";
  private static final String PARALLEL_STRATEGY_CLASS_PREFIX =
      "org.sosy_lab.cpachecker.pcc.strategy.parallel";

  public static PCCStrategy buildStrategy(
      String pPccStrategy, Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, ConfigurableProgramAnalysis pCpa, CFA pCfa)
      throws InvalidConfigurationException {
    if (pPccStrategy == null) {
      throw new InvalidConfigurationException(
          "No PCC strategy defined.");
    }

    Class<?> pccStrategyClass;
    try {
      pccStrategyClass = Classes.forName(pPccStrategy, STRATEGY_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      try {
        pccStrategyClass = Classes.forName(pPccStrategy, PARALLEL_STRATEGY_CLASS_PREFIX);
      } catch (ClassNotFoundException e1) {
        throw new InvalidConfigurationException(
            "Class for pcc checker  " + pPccStrategy + " is unknown.", e1);
      }
    }

    if (!PCCStrategy.class.isAssignableFrom(pccStrategyClass)) {
      throw new InvalidConfigurationException(
          "Specified class " + pPccStrategy + "does not implement the pPccStrategy interface!");
    }

    // construct property checker instance
    try {
      Constructor<?>[] cons = pccStrategyClass.getConstructors();

      Class<?>[] paramTypes;
      for (Constructor<?> con : cons) {
        paramTypes = con.getParameterTypes();
        if (paramTypes.length == 4) {
          if (checkRequiredParameters(paramTypes)) {
            if (paramTypes[3] == CFA.class) {
              return (PCCStrategy) con.newInstance(pConfig, pLogger, pShutdownNotifier, pCfa);
            } else {
              if (pCpa == null
                  || (paramTypes[3] == ProofChecker.class && pCpa instanceof ProofChecker)
                  || (paramTypes[3] == PropertyCheckerCPA.class
                  && pCpa instanceof PropertyCheckerCPA)) {
                return (PCCStrategy) con.newInstance(pConfig, pLogger, pShutdownNotifier, pCpa);
              }
            }
          }
        }
      }

      throw new UnsupportedOperationException(
          "Cannot create PCC Strategy "
              + pPccStrategy
              +
              " if it does not provide a constructor (Configuration, LogManager, ShutdownNotifier, (PropertyCheckerCPA|ProofChecker)");
    } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }

  private static boolean checkRequiredParameters(Class<?>[] paramTypes) {
    return paramTypes[0] == Configuration.class
        && paramTypes[1] == LogManager.class
        && paramTypes[2] == ShutdownNotifier.class;
  }
}
