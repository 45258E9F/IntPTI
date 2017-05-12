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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface CFA {

  MachineModel getMachineModel();

  boolean isEmpty();

  int getNumberOfFunctions();

  Set<String> getAllFunctionNames();

  Collection<FunctionEntryNode> getAllFunctionHeads();

  FunctionEntryNode getFunctionHead(String name);

  Map<String, FunctionEntryNode> getAllFunctions();

  Collection<CFANode> getAllNodes();

  FunctionEntryNode getMainFunction();

  Optional<LoopStructure> getLoopStructure();

  Optional<ImmutableSet<CFANode>> getAllLoopHeads();

  Optional<VariableClassification> getVarClassification();

  Optional<LiveVariables> getLiveVariables();

  Language getLanguage();
}