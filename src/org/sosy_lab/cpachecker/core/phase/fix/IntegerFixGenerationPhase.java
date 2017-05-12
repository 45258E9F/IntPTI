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
package org.sosy_lab.cpachecker.core.phase.fix;

import com.google.common.base.Joiner;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.bugfix.FixInformation;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFix.IntegerFixMode;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerFixInfo;
import org.sosy_lab.cpachecker.core.bugfix.instance.integer.IntegerTypeConstraint;
import org.sosy_lab.cpachecker.core.phase.CPAPhase;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A phase to generate fixes for integer errors.
 * This phase should be used after fix detection phase.
 */
@Options(prefix = "phase.repair.integer")
public class IntegerFixGenerationPhase extends CPAPhase {

  @Option(secure = true, name = "MAXSMTSolver", values = {"z3", "yices"}, description = "solver "
      + "specified for solving MAX-SMT problem")
  private String maxSMTSolver = "z3";

  @Option(secure = true, name = "MAXSMTFileName", description = "name for temporary file of "
      + "MAXSMT constraints")
  private String maxSMTFileName = "constraint.smt2";

  @Option(secure = true, name = "cover", description = "weight for cover relation")
  private int coverWeight = 100;

  @Option(secure = true, name = "declarationCover", description = "weight for cover relation in "
      + "declaration")
  private int declarationCoverWeight = 5;

  @Option(secure = true, name = "equal", description = "weight for equal relation")
  private int equalWeight = 1;

  private final MachineModel machineModel;

  // accumulate total punishment for ITC (integer type constraint)
  private long totalPunishment = 0;

  public IntegerFixGenerationPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
    // try to obtain the current machine model
    if (GlobalInfo.getInstance().getCFAInfo().isPresent()) {
      CFAInfo info = GlobalInfo.getInstance().getCFAInfo().get();
      machineModel = info.getCFA().getMachineModel();
    } else {
      throw new InvalidConfigurationException("CFA creation phase should be descendant of current "
          + "phase");
    }
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    FixInformation fixInfo = FixProvider.getFixInfo(BugCategory.INTEGER);
    if (fixInfo == null) {
      // no fix information found
      return CPAPhaseStatus.FAIL;
    }
    assert (fixInfo instanceof IntegerFixInfo);
    IntegerFixInfo intFixInfo = (IntegerFixInfo) fixInfo;

    // STEP 1: solve type constraint
    Map<String, CSimpleType> solveResult = solveTypeConstraint(intFixInfo);
    // add punishment to overlong types
    // totalPunishment = totalPunishment + ((IntegerFixInfo) fixInfo).getPunishCount() *
    // coverWeight;

    // STEP 2: generate TA (declared type alteration) fixes
    for (Entry<String, CSimpleType> entry : solveResult.entrySet()) {
      String varName = entry.getKey();
      if (!intFixInfo.containsLeftName(varName)) {
        // if the key is not a left name in the original source, we do not generate a specifier
        // fix for it
        continue;
      }
      CSimpleType targetType = entry.getValue();
      // further check if the specifier fix is necessary
      CSimpleType oldType = ((IntegerFixInfo) fixInfo).getDeclaredType(varName);
      if (oldType != null) {
        if (Types.canHoldAllValues(oldType, targetType, machineModel)) {
          continue;
        }
      }
      FileLocation loc = intFixInfo.getLocation(varName);
      if (loc == null) {
        logger.log(Level.SEVERE, "unexpected identifier: " + varName);
        continue;
      }
      intFixInfo.addCandidateFix(loc, IntegerFixMode.SPECIFIER, targetType);
    }

    // STEP 3: merge multiple fixes for the same location
    intFixInfo.mergeFixes(machineModel);

    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  public long getTotalPunishment() {
    return totalPunishment;
  }

  private Map<String, CSimpleType> solveTypeConstraint(IntegerFixInfo pFixInfo) throws IOException {
    Map<String, CSimpleType> typeMap = new HashMap<>();

    // check if the constraint file exists
    File smt2File = new File(maxSMTFileName);
    // generate MAX-SMT constraints from fix information
    pFixInfo.generateTypeConstraint(smt2File.getAbsolutePath(), coverWeight,
        declarationCoverWeight, equalWeight, machineModel);

    // now, the SMTLIB2 file should exist
    if (!smt2File.exists()) {
      logger.log(Level.SEVERE, "type constraint not found");
      return typeMap;
    }

    // solve MAX-SMT constraint using z3 (z3opt)
    logger.log(Level.INFO, "Z3 starts working");
    ProcessBuilder process = new ProcessBuilder(maxSMTSolver, smt2File.getAbsolutePath());
    process.redirectErrorStream(true);
    Process proc = process.start();
    InputStreamReader isr = new InputStreamReader(proc.getInputStream());
    BufferedReader br = new BufferedReader(isr);
    String readLine;
    List<String> outputByLine = new ArrayList<>();
    while ((readLine = br.readLine()) != null) {
      outputByLine.add(readLine);
    }
    try {
      br.close();
      isr.close();
      proc.waitFor();
      proc.destroy();
    } catch (InterruptedException ex) {
      logger.log(Level.SEVERE, "MAX-SMT solver is interrupted unexpectedly");
    } finally {
      // constraint file is no longer needed
      if (smt2File.exists()) {
        smt2File.deleteOnExit();
      }
    }
    logger.log(Level.INFO, "Z3 finished working");

    // parse result produced by MAX-SMT solver
    String output = Joiner.on("").join(outputByLine);
    MaxSMTModel model = parseMaxSMTModel(output);

    if (model.getSat()) {
      Map<String, Object> valuation = model.getValuation();
      for (Entry<String, Object> entry : valuation.entrySet()) {
        typeMap.put(entry.getKey(), (CSimpleType) entry.getValue());
      }
    }
    return typeMap;

  }

  private final Pattern objectivePattern =
      Pattern.compile("\\(objectives\\s+\\(\\s*(\\d+)\\s*\\)\\s*\\)");
  private final Pattern definePattern =
      Pattern.compile("\\(define-fun\\s+(\\S+)\\s+\\(\\)\\s+I\\s+(\\S+)\\s*\\)");

  /**
   * Parse string as MAX-SMT model.
   * It is worth noting that the format of MAX-SMT model is not unified forcibly. Here the
   * parsing method is based on Z3 with the version newer than 4.4.2.
   */
  private MaxSMTModel parseMaxSMTModel(String modelString) {
    if (!modelString.startsWith("sat")) {
      // If isSat is false, the result can be UNSAT or UNKNOWN.
      return new MaxSMTModel(false, 0);
    }
    modelString = modelString.substring(3);
    // Extract the first S-expression to extract the objective value.
    MaxSMTModel model;
    String objectiveStr = "";
    int bracketLevel = 0;
    for (int i = 0; i < modelString.length(); i++) {
      char c = modelString.charAt(i);
      if (c == '(') {
        bracketLevel++;
      } else if (c == ')') {
        bracketLevel--;
        if (bracketLevel == 0) {
          objectiveStr = modelString.substring(0, i + 1);
          modelString = modelString.substring(i + 1);
          break;
        }
      }
    }
    Matcher objectiveMatch = objectivePattern.matcher(objectiveStr);
    if (objectiveMatch.find()) {
      int value = Integer.valueOf(objectiveMatch.group(1));
      model = new MaxSMTModel(true, value);
      totalPunishment += model.getObjective();
    } else {
      // in most cases, we reach here because no constraints are generated
      logger.log(Level.WARNING, "Failed to find the objective");
      return new MaxSMTModel(false, 0);
    }
    // Extract valuation from the remaining S-expression
    if (!modelString.startsWith("(model")) {
      logger.log(Level.SEVERE, "Failed to parse model assignment");
      return model;
    }
    modelString = modelString.substring(7, modelString.length() - 1).trim();
    List<String> defineList = new ArrayList<>();
    bracketLevel = 0;
    int startIndex = 0, endIndex;
    for (int i = 0; i < modelString.length(); i++) {
      char c = modelString.charAt(i);
      if (c == '(') {
        if (bracketLevel == 0) {
          startIndex = i;
        }
        bracketLevel++;
      } else if (c == ')') {
        bracketLevel--;
        if (bracketLevel == 0) {
          endIndex = i;
          defineList.add(modelString.substring(startIndex, endIndex + 1));
        }
      }
    }
    for (String defineStr : defineList) {
      Matcher defineMatch = definePattern.matcher(defineStr);
      if (defineMatch.find()) {
        String varName = defineMatch.group(1);
        // restore identifier
        if (varName.startsWith("!!")) {
          varName = varName.substring(2);
        }
        varName = varName.replace("!!", "::");
        String typeStr = defineMatch.group(2);
        CSimpleType type = IntegerTypeConstraint.fromTypeString(typeStr);
        if (type != null) {
          model.addValuation(varName, type);
        }
      }
    }
    return model;
  }

  /**
   * The model for MAX-SMT problem. It consists of (1) SAT/UNSAT, (2) objective, as well as the
   * punishment value, (3) valuation for variables.
   */
  public static class MaxSMTModel {

    private boolean isSat;
    private int objective;
    private Map<String, Object> valuation;

    MaxSMTModel(boolean pIsSat, int pObjective) {
      isSat = pIsSat;
      objective = pObjective;
      valuation = new HashMap<>();
    }

    void addValuation(String key, Object value) {
      valuation.put(key, value);
    }

    Map<String, Object> getValuation() {
      return valuation;
    }

    boolean getSat() {
      return isSat;
    }

    int getObjective() {
      return objective;
    }

  }

}
