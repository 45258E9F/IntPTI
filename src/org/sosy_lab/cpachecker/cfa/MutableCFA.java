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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SortedSetMultimap;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

public class MutableCFA implements CFA {

  private final MachineModel machineModel;
  private final SortedMap<String, FunctionEntryNode> functions;
  private final SortedSetMultimap<String, CFANode> allNodes;
  private final FunctionEntryNode mainFunction;
  private final Language language;
  private Optional<LoopStructure> loopStructure = Optional.absent();
  private Optional<LiveVariables> liveVariables = Optional.absent();

  public MutableCFA(
      MachineModel pMachineModel,
      SortedMap<String, FunctionEntryNode> pFunctions,
      SortedSetMultimap<String, CFANode> pAllNodes,
      FunctionEntryNode pMainFunction,
      Language pLanguage) {

    machineModel = pMachineModel;
    functions = pFunctions;
    allNodes = pAllNodes;
    mainFunction = pMainFunction;
    language = pLanguage;

    assert functions.keySet().equals(allNodes.keySet());
    assert functions.get(mainFunction.getFunctionName()) == mainFunction;
  }

  public void addNode(CFANode pNode) {
    assert functions.containsKey(pNode.getFunctionName());
    allNodes.put(pNode.getFunctionName(), pNode);
  }

  public void clear() {
    functions.clear();
    allNodes.clear();
  }

  public void removeNode(CFANode pNode) {
    SortedSet<CFANode> functionNodes = allNodes.get(pNode.getFunctionName());
    assert functionNodes.contains(pNode);
    functionNodes.remove(pNode);

    if (functionNodes.isEmpty()) {
      functions.remove(pNode.getFunctionName());
    }
  }

  @Override
  public MachineModel getMachineModel() {
    return machineModel;
  }

  @Override
  public boolean isEmpty() {
    return functions.isEmpty();
  }

  @Override
  public int getNumberOfFunctions() {
    return functions.size();
  }

  @Override
  public Set<String> getAllFunctionNames() {
    return Collections.unmodifiableSet(functions.keySet());
  }

  @Override
  public Collection<FunctionEntryNode> getAllFunctionHeads() {
    return Collections.unmodifiableCollection(functions.values());
  }

  @Override
  public FunctionEntryNode getFunctionHead(String pName) {
    return functions.get(pName);
  }

  @Override
  public Map<String, FunctionEntryNode> getAllFunctions() {
    return Collections.unmodifiableMap(functions);
  }

  public SortedSet<CFANode> getFunctionNodes(String pName) {
    return Collections.unmodifiableSortedSet(allNodes.get(pName));
  }

  @Override
  public Collection<CFANode> getAllNodes() {
    return Collections.unmodifiableCollection(allNodes.values());
  }

  @Override
  public FunctionEntryNode getMainFunction() {
    return mainFunction;
  }

  @Override
  public Optional<LoopStructure> getLoopStructure() {
    return loopStructure;
  }

  public void setLoopStructure(Optional<LoopStructure> pLoopStructure) {
    loopStructure = checkNotNull(pLoopStructure);
  }

  @Override
  public Optional<ImmutableSet<CFANode>> getAllLoopHeads() {
    if (loopStructure.isPresent()) {
      return Optional.of(loopStructure.get().getAllLoopHeads());
    }
    return Optional.absent();
  }

  public ImmutableCFA makeImmutableCFA(Optional<VariableClassification> pVarClassification) {
    return new ImmutableCFA(machineModel, functions, allNodes, mainFunction,
        loopStructure, pVarClassification, liveVariables, language);
  }

  @Override
  public Optional<VariableClassification> getVarClassification() {
    return Optional.absent();
  }

  @Override
  public Optional<LiveVariables> getLiveVariables() {
    return liveVariables;
  }

  public void setLiveVariables(Optional<LiveVariables> pLiveVariables) {
    liveVariables = pLiveVariables;
  }

  @Override
  public Language getLanguage() {
    return language;
  }

}