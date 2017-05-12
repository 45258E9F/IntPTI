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

import com.google.common.base.Optional;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerWithDelayedErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.PL;
import org.sosy_lab.cpachecker.core.interfaces.checker.StateChecker;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;

public class DeadCodeChecker
    implements StateChecker<ConstraintsState>, CheckerWithDelayedErrorReport {

  private HashSet<CFAEdge> deadEdges;
  private HashSet<CFAEdge> unsatDeadAssumptionEdges;
  private HashSet<CFAEdge> unUsedFuncEdges;
  private HashSet<CFAEdge> liveEdges;
  private final List<ErrorReport> errorStore;

  public DeadCodeChecker(Configuration config) {
    deadEdges = new HashSet<>();
    unsatDeadAssumptionEdges = new HashSet<>();
    unUsedFuncEdges = new HashSet<>();
    liveEdges = new HashSet<>();
    errorStore = new ArrayList<>();
    GlobalInfo gInfo = GlobalInfo.getInstance();
    Optional<CFAInfo> cfaInfo = gInfo.getCFAInfo();
    if (cfaInfo.isPresent()) {
      int nodeNum = cfaInfo.get().getCFA().getAllNodes().size();
      for (int i = 0; i < nodeNum; i++) {
        CFANode curNode = cfaInfo.get().getNodeByNodeNumber(i);
        if (curNode != null) {
          int leavingEdgeNum = curNode.getNumLeavingEdges();
          for (int j = 0; j < leavingEdgeNum; j++) {
            deadEdges.add(curNode.getLeavingEdge(j));
          }
        }
      }
    }
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.DEAD_CODE;
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return ConstraintsState.class;
  }

  @Override
  public void checkAndRefine(
      ConstraintsState pPostState,
      Collection<AbstractState> pPostOtherStates, CFAEdge pCfaEdge,
      Collection<ConstraintsState> pNewPostStates) throws CPATransferException {
    if (liveEdges.contains(pCfaEdge) == false) {
      switch (pCfaEdge.getEdgeType()) {
        case AssumeEdge:
          if (pPostState.getPreEdgeUnsat() == true) {
            unsatDeadAssumptionEdges.add(pCfaEdge);
          } else {
            liveEdges.add(pCfaEdge);
            if (deadEdges.contains(pCfaEdge) == true) {
              deadEdges.remove(pCfaEdge);
            }
            if (unsatDeadAssumptionEdges.contains(pCfaEdge) == true) {
              unsatDeadAssumptionEdges.remove(pCfaEdge);
            }
          }
          break;
        default:
          liveEdges.add(pCfaEdge);
          if (deadEdges.contains(pCfaEdge) == true) {
            deadEdges.remove(pCfaEdge);
          }
          break;
      }
    }
    pNewPostStates.add(pPostState);
  }

  public void handleDeadEdges() {
    Map<Integer, Integer> nodesANDcurEnteringEdgeNum = new HashMap<>();
    for (CFAEdge edge : deadEdges) {
      int successorNodeNum = edge.getSuccessor().getNodeNumber();
      if (nodesANDcurEnteringEdgeNum.containsKey(successorNodeNum)) {
        int oldNum = nodesANDcurEnteringEdgeNum.remove(successorNodeNum);
        int newNum = oldNum + 1;
        nodesANDcurEnteringEdgeNum.put(successorNodeNum, newNum);
      } else {
        nodesANDcurEnteringEdgeNum.put(successorNodeNum, 1);
      }
    }
    handleNodesAndCurLeavingEdgesMap(nodesANDcurEnteringEdgeNum);

    //extract dead code detected in preprocess stage
    GlobalInfo gInfo = GlobalInfo.getInstance();
    Collection<CFAEdge> deadCode = gInfo.getPreInfoManager().getDeadCode();
    for (CFAEdge curEdge : deadCode) {
      if (curEdge.getRawAST().isPresent()) {
        DeadCodeErrorReport er =
            new DeadCodeErrorReport((CAstNode) curEdge.getRawAST().get(), curEdge);
        errorStore.add(er);
      } else {
        if (curEdge instanceof BlankEdge) {
          DeadCodeErrorReport er = new DeadCodeErrorReport(null, curEdge);
          errorStore.add(er);
        }
      }
    }
  }

  public void handleNodesAndCurLeavingEdgesMap(Map<Integer, Integer> nodesANDenteringEdges) {
    GlobalInfo gInfo = GlobalInfo.getInstance();
    TreeSet<Integer> deadNodeNum = new TreeSet<>();
    Optional<CFAInfo> cfaInfo = gInfo.getCFAInfo();
    if (cfaInfo.isPresent()) {
      for (int nodeNum : nodesANDenteringEdges.keySet()) {
        int totalLeavingEdgeNum = cfaInfo.get().getNodeByNodeNumber(nodeNum).getNumEnteringEdges();
        int deadEdgeNum = nodesANDenteringEdges.get(nodeNum);
        if (totalLeavingEdgeNum == deadEdgeNum) {
          deadNodeNum.add(nodeNum);
        }
      }
      handleDeadNodes(deadNodeNum);
    }
  }

  public void handleDeadNodes(TreeSet<Integer> deadNodeNum) {
    GlobalInfo gInfo = GlobalInfo.getInstance();
    Optional<CFAInfo> cfaInfo = gInfo.getCFAInfo();
    if (cfaInfo.isPresent()) {
      int allNodesNum = cfaInfo.get().getCFA().getAllNodes().size();
      for (int nodeNum = 1; nodeNum < allNodesNum; nodeNum++) {
        if (deadNodeNum.contains(nodeNum)) {
          CFANode curNode = cfaInfo.get().getNodeByNodeNumber(nodeNum);
          int leavingEdgeNum = curNode.getNumLeavingEdges();
          for (int edgeIndex = 0; edgeIndex < leavingEdgeNum; edgeIndex++) {
            CFAEdge curEdge = curNode.getLeavingEdge(edgeIndex);
            /*String statement = curEdge.getDescription();
            if(statement.contains("CPACHECKER")){
              continue;
            }*/
            if (curEdge.getRawAST().isPresent()) {
              DeadCodeErrorReport er =
                  new DeadCodeErrorReport((CAstNode) curEdge.getRawAST().get(), curEdge);
              errorStore.add(er);
            } else {
              if (curEdge instanceof BlankEdge) {
                DeadCodeErrorReport er = new DeadCodeErrorReport(null, curEdge);
                errorStore.add(er);
              }
            }
          }
        }
      }
    }
  }

  /*public void handleUnsatAssumptionEdges(){
    TreeMap<Integer, String> lineNumANDconstraints = new TreeMap<>();
    for(CFAEdge edge : unsatDeadAssumptionEdges){
      AssumeEdge assume = (AssumeEdge)edge;
      int lineNum = assume.getFileLocation().getStartingLineInOrigin();
      String constraints = assume.getRawStatement();
      if(lineNumANDconstraints.containsKey(lineNum) == false){
        if(assume.getTruthAssumption() == true) {
          lineNumANDconstraints.put(lineNum, "!(" + constraints + ")" + " always true." + constraints + " always false  ");
        }
        else{
          constraints = constraints.replace("!", "");
          lineNumANDconstraints.put(lineNum, constraints + " always true." + "!(" + constraints + ")" + " always false  ");
        }
      }
      else{
        String newConstriants;
        if(assume.getTruthAssumption() == true) {
          newConstriants = lineNumANDconstraints.get(lineNum) + "!(" + constraints + ")" + " always true." + constraints + " always false  ";
        }
        else{
          constraints = constraints.replace("!", "");
          newConstriants = lineNumANDconstraints.get(lineNum) + constraints + " always true." + "!(" + constraints + ")" + " always false  ";
        }
        lineNumANDconstraints.put(lineNum, newConstriants);
      }
    }
    lineNumANDconstraints.comparator();
    System.out.println(lineNumANDconstraints.toString());
  }*/

  public void handleUnsatAssumptionEdges() {
    for (CFAEdge edge : unsatDeadAssumptionEdges) {
      AssumeEdge assume = (AssumeEdge) edge;
      if (assume.getTruthAssumption() == true) {
        if (assume.getRawAST().isPresent()) {
          AlwaysFalseErrorReport er =
              new AlwaysFalseErrorReport((CAstNode) assume.getRawAST().get(), edge);
          errorStore.add(er);
        }
      } else {
        if (assume.getRawAST().isPresent()) {
          AlwaysTrueErrorReport er =
              new AlwaysTrueErrorReport((CAstNode) assume.getRawAST().get(), edge);
          errorStore.add(er);
        }
      }
    }

    GlobalInfo gInfo = GlobalInfo.getInstance();
    //extract always false expressions detected in preprocess stage
    Collection<CFAEdge> alwaysFlase = gInfo.getPreInfoManager().getAlwaysFalse();
    for (CFAEdge curEdge : alwaysFlase) {
      if (curEdge.getRawAST().isPresent()) {
        AlwaysFalseErrorReport preER =
            new AlwaysFalseErrorReport((CAstNode) curEdge.getRawAST().get(), curEdge);
        errorStore.add(preER);
      } else {
        if (curEdge instanceof BlankEdge) {
          AlwaysFalseErrorReport er = new AlwaysFalseErrorReport(null, curEdge);
          errorStore.add(er);
        }
      }
    }
    //extract always true expressions detected in preprocess stage
    Collection<CFAEdge> alwaysTrue = gInfo.getPreInfoManager().getAlwaysTrue();
    for (CFAEdge curEdge : alwaysTrue) {
      if (curEdge.getRawAST().isPresent()) {
        AlwaysTrueErrorReport preER =
            new AlwaysTrueErrorReport((CAstNode) curEdge.getRawAST().get(), curEdge);
        errorStore.add(preER);
      } else {
        if (curEdge instanceof BlankEdge) {
          AlwaysTrueErrorReport er = new AlwaysTrueErrorReport(null, curEdge);
          errorStore.add(er);
        }
      }
    }
  }

  public void getUnusedFunctions() {
    GlobalInfo gInfo = GlobalInfo.getInstance();
    Optional<CFAInfo> cfaInfo = gInfo.getCFAInfo();
    if (cfaInfo.isPresent()) {
      CFA cfa = cfaInfo.get().getCFA();
      CFANode mainNode = cfa.getMainFunction();
      HashSet<FunctionEntryNode> allFunctions = new HashSet<>();
      allFunctions.addAll(cfa.getAllFunctionHeads());
      Stack<CFANode> waitingSet = new Stack<>();
      HashSet<CFANode> reachedSet = new HashSet<>();
      waitingSet.push(mainNode);
      while (waitingSet.isEmpty() == false) {
        CFANode curNode = waitingSet.pop();
        if (curNode instanceof FunctionEntryNode && allFunctions.contains(curNode)) {
          allFunctions.remove(curNode);
        }
        reachedSet.add(curNode);
        int leavingEdgeNum = curNode.getNumLeavingEdges();
        for (int i = 0; i < leavingEdgeNum; i++) {
          CFANode sucNode = curNode.getLeavingEdge(i).getSuccessor();
          if (reachedSet.contains(sucNode) == false) {
            waitingSet.push(sucNode);
          }
        }
      }
      if (allFunctions.isEmpty() == false) {
        for (CFANode curEntry : allFunctions) {
          if (gInfo.getCStaticFunctions()
              .contains(((FunctionEntryNode) (curEntry)).getFunctionDefinition()) == false) {
            continue;
          }
          CFAEdge repreEdge =
              new BlankEdge(((FunctionEntryNode) curEntry).getFunctionDefinition().toString(),
                  ((FunctionEntryNode) curEntry).getFileLocation(), curEntry,
                  curEntry.getLeavingEdge(0).getSuccessor(),
                  ((FunctionEntryNode) curEntry).getFunctionDefinition().toString());
          unUsedFuncEdges.add(repreEdge);
        }
      }
    }
  }

  public void handleUnusedFuncEdges() {
    getUnusedFunctions();
    if (unUsedFuncEdges.isEmpty()) {
      return;
    }
    for (CFAEdge curEdge : unUsedFuncEdges) {
      if (curEdge instanceof BlankEdge) {
        DeadCodeErrorReport er = new DeadCodeErrorReport(null, curEdge);
        errorStore.add(er);
      }
    }
  }

  @Override
  public Collection<ErrorReport> getErrorReport() {
    handleDeadEdges();
    handleUnsatAssumptionEdges();
    handleUnusedFuncEdges();
    for (ErrorReport er : errorStore) {
      if (er.getErrorSpot().getASTNode().isPresent()) {
        System.out.println(er.getErrorSpot().getASTNode().get().toString());
      } else {
        System.out.println(er.getErrorSpot().getCFAEdge().get().getRawStatement());
      }
    }
    return errorStore;
  }

}
