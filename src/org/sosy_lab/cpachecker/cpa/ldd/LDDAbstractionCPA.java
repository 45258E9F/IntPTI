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
package org.sosy_lab.cpachecker.cpa.ldd;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegionManager;

import java.util.HashMap;
import java.util.Map;

public class LDDAbstractionCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LDDAbstractionCPA.class);
  }

  private final LDDAbstractDomain domain;

  private final StopOperator stopOperator;

  private final LDDAbstractionTransferRelation transferRelation;

  private final LDDRegionManager regionManager;

  private final LDDAbstractState initialState;

  public LDDAbstractionCPA(CFA cfa) {
    Map<String, Integer> variables = new HashMap<>();

    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        if (edge instanceof CDeclarationEdge) {
          CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
          CDeclaration declaration = declarationEdge.getDeclaration();
          if (declaration instanceof CVariableDeclaration) {
            String name = declaration.getName();
            CType type = declaration.getType();
            registerVariable(name, type, variables);
          } else if (declaration instanceof CFunctionDeclaration) {
            CFunctionDeclaration funDecl = (CFunctionDeclaration) declaration;
            for (CParameterDeclaration paramDecl : funDecl.getParameters()) {
              String name = paramDecl.getName();
              CType type = paramDecl.getType();
              registerVariable(name, type, variables);
            }
          }
        }
      }
    }
    for (FunctionEntryNode node : cfa.getAllFunctionHeads()) {
      if (node instanceof CFunctionEntryNode) {
        CFunctionEntryNode fDefNode = (CFunctionEntryNode) node;
        for (CParameterDeclaration paramDecl : fDefNode.getFunctionDefinition().getParameters()) {
          String name = paramDecl.getName();
          CType type = paramDecl.getType();
          registerVariable(name, type, variables);
        }
      }
    }
    this.regionManager = new LDDRegionManager(variables.size());
    this.domain = new LDDAbstractDomain(this.regionManager);
    this.stopOperator = new StopSepOperator(this.domain);
    this.initialState = new LDDAbstractState(this.regionManager.makeTrue());
    this.transferRelation = new LDDAbstractionTransferRelation(this.regionManager, variables);
  }

  private void registerVariable(String name, CType type, Map<String, Integer> variables) {
    if (name != null && !name.isEmpty() && type != null && type instanceof CSimpleType) {
      CBasicType basicType = ((CSimpleType) type).getType();
      if (basicType == CBasicType.INT) {
        variables.put(name, variables.size());
      }
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return this.stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return this.initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

}
