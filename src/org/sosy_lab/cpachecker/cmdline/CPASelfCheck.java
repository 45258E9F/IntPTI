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
package org.sosy_lab.cpachecker.cmdline;

import com.google.common.base.Optional;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class CPASelfCheck {

  private static LogManager logManager;
  private static Configuration config;

  /**
   * @param args cmdline parameters, unused here
   */
  public static void main(String[] args) throws Exception {
    config = Configuration.defaultConfiguration();
    logManager = new BasicLogManager(config);

    CFA cfa = createCFA();
    FunctionEntryNode main = cfa.getMainFunction();

    logManager.log(Level.INFO, "Searching for CPAs");
    List<Class<ConfigurableProgramAnalysis>> cpas = getCPAs();

    for (Class<ConfigurableProgramAnalysis> cpa : cpas) {
      logManager.log(Level.INFO, "Checking " + cpa.getCanonicalName() + " ...");
      ConfigurableProgramAnalysis cpaInst;
      try {
        cpaInst = tryToInstantiate(cpa, cfa);
      } catch (InvocationTargetException e) {
        logManager.logException(Level.WARNING, e,
            "Getting factory instance for " + cpa.getCanonicalName() + " failed!");
        continue;
      } catch (NoSuchMethodException e) {
        logManager.logException(Level.WARNING, e,
            "Getting factory instance for " + cpa.getCanonicalName() +
                " failed: no factory method!");
        continue;
      } catch (Exception e) {
        logManager
            .logException(Level.WARNING, e, "Could not instantiate " + cpa.getCanonicalName());
        continue;
      } catch (UnsatisfiedLinkError e) {
        logManager
            .logException(Level.WARNING, e, "Could not instantiate " + cpa.getCanonicalName());
        continue;
      }
      assert cpaInst != null;

      try {
        cpaInst.getInitialState(main, StateSpacePartition.getDefaultPartition());

        boolean ok = true;
        // check domain and lattice
        ok &= checkJoin(cpaInst, main);
        /// TODO checking the invariantes of the transfer relation is a bit more work ...
        // check merge
        ok &= checkMergeSoundness(cpaInst, main);
        // check stop
        ok &= checkStopEmptyReached(cpaInst, main);
        ok &= checkStopReached(cpaInst, main);
        /// TODO check invariants of precision adjustment
        logManager.log(Level.INFO, ok ? " OK" : " ERROR");
      } catch (Exception e) {
        logManager.logException(Level.WARNING, e, "");
      }
    }
  }

  private static CFA createCFA() throws ParserException, InvalidConfigurationException {
    String code = "int main() {\n"
        + "  int a;\n"
        + "  a = 1;\n"
        + "  return (a);\n"
        + "}\n";

    CParser parser = CParser.Factory
        .getParser(config, logManager, CParser.Factory.getDefaultOptions(), MachineModel.LINUX32);
    CSourceOriginMapping sourceOriginMapping = new CSourceOriginMapping();
    ParseResult cfas = parser.parseString("", code, sourceOriginMapping);
    MutableCFA cfa = new MutableCFA(MachineModel.LINUX32, cfas.getFunctions(), cfas.getCFANodes(),
        cfas.getFunctions().get("main"), Language.C);
    return cfa.makeImmutableCFA(Optional.<VariableClassification>absent());
  }

  private static ConfigurableProgramAnalysis tryToInstantiate(
      Class<ConfigurableProgramAnalysis> pCpa,
      CFA cfa)
      throws NoSuchMethodException, InvocationTargetException, InvalidConfigurationException,
             CPAException, IllegalAccessException {
    Method factoryMethod = pCpa.getMethod("factory", new Class<?>[0]);

    CPAFactory factory = (CPAFactory) factoryMethod.invoke(null, new Object[0]);
    return factory.setLogger(logManager)
        .setConfiguration(config)
        .set(cfa, CFA.class)
        .createInstance();
  }

  private static boolean ensure(boolean pB, String pString) {
    if (!pB) {
      logManager.log(Level.WARNING, pString);
      return false;
    }
    return true;
  }

  private static boolean checkJoin(
      ConfigurableProgramAnalysis pCpaInst,
      FunctionEntryNode pMain) throws CPAException, InterruptedException {
    AbstractDomain d = pCpaInst.getAbstractDomain();
    AbstractState initial =
        pCpaInst.getInitialState(pMain, StateSpacePartition.getDefaultPartition());

    return ensure(d.isLessOrEqual(initial, d.join(initial, initial)),
        "Join of same elements is unsound!");
  }

  private static boolean checkMergeSoundness(
      ConfigurableProgramAnalysis pCpaInst,
      FunctionEntryNode pMain) throws CPAException, InterruptedException {
    AbstractDomain d = pCpaInst.getAbstractDomain();
    MergeOperator merge = pCpaInst.getMergeOperator();
    AbstractState initial =
        pCpaInst.getInitialState(pMain, StateSpacePartition.getDefaultPartition());
    Precision initialPrec =
        pCpaInst.getInitialPrecision(pMain, StateSpacePartition.getDefaultPartition());

    return ensure(d.isLessOrEqual(initial, merge.merge(initial, initial, initialPrec)),
        "Merging same elements was unsound!");
  }


  private static boolean checkStopEmptyReached(
      ConfigurableProgramAnalysis pCpaInst,
      FunctionEntryNode pMain) throws CPAException, InterruptedException {
    StopOperator stop = pCpaInst.getStopOperator();
    HashSet<AbstractState> reached = new HashSet<>();
    AbstractState initial =
        pCpaInst.getInitialState(pMain, StateSpacePartition.getDefaultPartition());
    Precision initialPrec =
        pCpaInst.getInitialPrecision(pMain, StateSpacePartition.getDefaultPartition());

    return ensure(!stop.stop(initial, reached, initialPrec), "Stopped on empty set!");
  }

  private static boolean checkStopReached(
      ConfigurableProgramAnalysis pCpaInst,
      FunctionEntryNode pMain) throws CPAException, InterruptedException {
    StopOperator stop = pCpaInst.getStopOperator();
    HashSet<AbstractState> reached = new HashSet<>();
    AbstractState initial =
        pCpaInst.getInitialState(pMain, StateSpacePartition.getDefaultPartition());
    reached.add(initial);
    Precision initialPrec =
        pCpaInst.getInitialPrecision(pMain, StateSpacePartition.getDefaultPartition());

    return ensure(stop.stop(initial, reached, initialPrec), "Did not stop on same element!");
  }

  private static List<Class<ConfigurableProgramAnalysis>> getCPAs() throws IOException {
    Set<ClassInfo> cpaCandidates = ClassPath.from(Thread.currentThread().getContextClassLoader())
        .getTopLevelClasses("org.sosy_lab.cpachecker.cpa");

    List<Class<ConfigurableProgramAnalysis>> cpas = new ArrayList<>();

    Class<ConfigurableProgramAnalysis> targetType = null;

    for (ClassInfo candidateInfo : cpaCandidates) {
      Class<?> candidate = candidateInfo.load();
      if (!Modifier.isAbstract(candidate.getModifiers())
          && !Modifier.isInterface(candidate.getModifiers())
          && ConfigurableProgramAnalysis.class.isAssignableFrom(candidate)) {

        // candidate is non-abstract implementation of CPA interface
        cpas.add(uncheckedGenericCast(candidate, targetType));
      }
    }

    return cpas;
  }

  @SuppressWarnings({"unchecked", "unused"})
  private static <T> Class<T> uncheckedGenericCast(Class<?> classObj, Class<T> targetType) {
    return (Class<T>) classObj;
  }
}