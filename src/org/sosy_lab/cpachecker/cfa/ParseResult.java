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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.SortedSetMultimap;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.List;
import java.util.SortedMap;

/**
 * Class representing the result of parsing a C file before function calls
 * are bound to their targets.
 *
 * It consists of a map that stores the CFAs for each function and a list of
 * declarations of global variables.
 *
 * This class is immutable, but it does not ensure that it's content also is.
 * It is recommended to use it only as a "transport" data class, not for
 * permanent storage.
 */
public class ParseResult {

  private final SortedMap<String, FunctionEntryNode> functions;

  private final SortedSetMultimap<String, CFANode> cfaNodes;

  private final List<Pair<ADeclaration, String>> globalDeclarations;

  private final Language language;

  public ParseResult(
      SortedMap<String, FunctionEntryNode> pFunctions,
      SortedSetMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      Language pLanguage) {
    functions = pFunctions;
    cfaNodes = pCfaNodes;
    globalDeclarations = pGlobalDeclarations;
    language = pLanguage;

  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  public SortedMap<String, FunctionEntryNode> getFunctions() {
    return functions;
  }

  public SortedSetMultimap<String, CFANode> getCFANodes() {
    return cfaNodes;
  }

  public List<Pair<ADeclaration, String>> getGlobalDeclarations() {
    return globalDeclarations;
  }

  public Language getLanguage() {
    return language;
  }
}
