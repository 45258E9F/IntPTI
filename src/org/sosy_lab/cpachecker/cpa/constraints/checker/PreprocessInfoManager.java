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
package org.sosy_lab.cpachecker.cpa.constraints.checker;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;

public class PreprocessInfoManager {

  // the dead code and always true or false expressions detected in preprocess stage are stored
  // for further reporting
  // the static functions are stored for further using
  private Collection<CFAEdge> deadCode;
  private Collection<CFAEdge> alwaysTrue;
  private Collection<CFAEdge> alwaysFalse;
  private HashSet<CFunctionDeclaration> cStaticFunctionDeclarations;

  // Since static function has transformed name in the CFA, we store
  //   file name, function name -> transformed name
  // for retrieving the transformed name in the CDT AST.
  private Table<String, String, String> cStaticFunctionNameMapping = HashBasedTable.create();

  //get info in CFACreator

  public PreprocessInfoManager() {
    deadCode = new ArrayList<>();
    alwaysTrue = new ArrayList<>();
    alwaysFalse = new ArrayList<>();
    cStaticFunctionDeclarations = new HashSet<>();
  }

  public void addDeadCode(CFAEdge edge) {
    deadCode.add(edge);
  }

  public Collection<CFAEdge> getDeadCode() {
    return deadCode;
  }

  public void addAlwaysTrue(CFAEdge edge) {
    alwaysTrue.add(edge);
  }

  public Collection<CFAEdge> getAlwaysTrue() {
    return alwaysTrue;
  }

  public void addAlwaysFalse(CFAEdge edge) {
    alwaysFalse.add(edge);
  }

  public Collection<CFAEdge> getAlwaysFalse() {
    return alwaysFalse;
  }

  public HashSet<CFunctionDeclaration> getcStaticFunctionDecls() {
    return cStaticFunctionDeclarations;
  }

  public void addStaticFunctionInfo(String fileName, String funcName, String transformedName) {
    cStaticFunctionNameMapping.put(fileName, funcName, transformedName);
  }

  @Nullable
  public String getTransformedName(String fileName, String funcName) {
    return cStaticFunctionNameMapping.get(fileName, funcName);
  }

}
