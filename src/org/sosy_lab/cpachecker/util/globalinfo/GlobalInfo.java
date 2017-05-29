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
package org.sosy_lab.cpachecker.util.globalinfo;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.function.FunctionMapManager;
import org.sosy_lab.cpachecker.cpa.apron.ApronCPA;
import org.sosy_lab.cpachecker.cpa.apron.ApronManager;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.constraints.checker.PreprocessInfoManager;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.reflect.ClassManager;
import org.sosy_lab.cpachecker.weakness.BugCollector;

import java.util.HashMap;
import java.util.Set;

import javax.annotation.Nullable;


public class GlobalInfo {

  private static GlobalInfo instance;

  private CFAInfo cfaInfo;
  private AutomatonInfo automatonInfo = new AutomatonInfo();
  private ConfigurableProgramAnalysis cpa;
  private FormulaManagerView predicateFormulaManagerView;
  private FormulaManagerView assumptionFormulaManagerView;
  private AbstractionManager absManager;
  private ApronManager apronManager;
  private LogManager apronLogger;

  private BasicIOManager ioManager = null;
  private BugCollector bugCollector = BugCollector.createInstance();
  private FunctionMapManager functionMapManager;
  private ClassManager classManager = new ClassManager();
  private PreprocessInfoManager preInfoManager = new PreprocessInfoManager();

  // TODO: re-organize the following fields

  // inserted by CP Wang
  private HashMap<LocationState, ApronState> apronInvariant = new HashMap<>();

  private GlobalInfo() {

  }

  public static GlobalInfo getInstance() {
    if (instance == null) {
      instance = new GlobalInfo();
    }
    return instance;
  }

  public PreprocessInfoManager getPreInfoManager() {
    return preInfoManager;
  }

  public Set<CFunctionDeclaration> getCStaticFunctions() {
    return preInfoManager.getcStaticFunctionDecls();
  }

  public void storeCFA(CFA cfa) {
    cfaInfo = new CFAInfo(cfa);
  }

  public Optional<CFAInfo> getCFAInfo() {
    return Optional.fromNullable(cfaInfo);
  }

  public Optional<ConfigurableProgramAnalysis> getCPA() {
    return Optional.fromNullable(cpa);
  }

  public void setUpInfoFromCPA(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
    absManager = null;
    apronManager = null;
    apronLogger = null;
    if (cpa != null) {
      for (ConfigurableProgramAnalysis c : CPAs.asIterable(cpa)) {
        if (c instanceof ControlAutomatonCPA) {
          ((ControlAutomatonCPA) c).registerInAutomatonInfo(automatonInfo);
        } else if (c instanceof ApronCPA) {
          Preconditions.checkState(apronManager == null && apronLogger == null);
          ApronCPA apron = (ApronCPA) c;
          apronManager = apron.getManager();
          apronLogger = apron.getLogger();
        } else if (c instanceof AssumptionStorageCPA) {
          Preconditions.checkState(assumptionFormulaManagerView == null);
          assumptionFormulaManagerView = ((AssumptionStorageCPA) c).getFormulaManager();
        } else if (c instanceof PredicateCPA) {
          Preconditions.checkState(absManager == null);
          absManager = ((PredicateCPA) c).getAbstractionManager();
          predicateFormulaManagerView = ((PredicateCPA) c).getSolver().getFormulaManager();
        }
      }
    }
  }

  public AutomatonInfo getAutomatonInfo() {
    Preconditions.checkState(automatonInfo != null);
    return automatonInfo;
  }

  public FormulaManagerView getPredicateFormulaManagerView() {
    Preconditions.checkState(predicateFormulaManagerView != null);
    return predicateFormulaManagerView;
  }

  public AbstractionManager getAbstractionManager() {
    Preconditions.checkState(absManager != null);
    return absManager;
  }

  public ApronManager getApronManager() {
    return apronManager;
  }

  public LogManager getApronLogManager() {
    return apronLogger;
  }

  public FormulaManagerView getAssumptionStorageFormulaManager() {
    Preconditions.checkState(assumptionFormulaManagerView != null);
    return assumptionFormulaManagerView;
  }

  /* *********** */
  /* bug manager */
  /* *********** */

  public void updateErrorCollector(ErrorReport error) {
    bugCollector.addErrorRecord(error);
  }

  public long getBugSize() {
    return bugCollector.getBugSize();
  }

  public Object exportErrorForLog() {
    return bugCollector.exportForLog();
  }

  public Object exportErrorForReport() {
    return bugCollector.exportForReport();
  }

  public void resetBugCollector() {
    bugCollector.resetBugCollector();
  }

  public HashMap<LocationState, ApronState> getApronInvariant() {
    return apronInvariant;
  }

  /* *********** */
  /* I/O manager */
  /* *********** */

  public void setUpToolDirectory(String pDir) {
    ioManager = new BasicIOManager(pDir);
  }

  public void setUpBasicInfo(String directory, boolean secureMode) {
    ioManager.updateBasicInfo(directory, secureMode);
  }

  public void updateInputPrograms(String programs) {
    ioManager.updateProgramNames(programs);
  }

  public BasicIOManager getIoManager() {
    Preconditions.checkState(ioManager != null);
    return ioManager;
  }

  /* ************************ */
  /* Function adapter manager */
  /* ************************ */

  public void setUpFunctionMap(Configuration config) {
    try {
      functionMapManager = new FunctionMapManager(config);
    } catch (InvalidConfigurationException ex) {
      functionMapManager = null;
    }
  }

  public
  @Nullable
  Path queryMapFilePath(Class<?> adaptorClass) {
    if (functionMapManager != null) {
      return functionMapManager.getMapFilePath(adaptorClass);
    }
    return null;
  }

  public boolean queryActiveness(Class<?> adapterClass) {
    return functionMapManager.queryActiveness(adapterClass);
  }

  public boolean queryStopFunction(String functionName) {
    return functionMapManager.isStopFunction(functionName);
  }

  public void addStopFunction(String functionName) {
    functionMapManager.addStopFunction(functionName);
  }

  /* ************* */
  /* class manager */
  /* ************* */

  @Nullable
  public Class<?> retrieveClass(String pName) {
    return classManager.getClassForSimpleName(pName);
  }

}
