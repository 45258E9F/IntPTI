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
package org.sosy_lab.cpachecker.cpa.livevar.checker;

import static org.sosy_lab.cpachecker.util.LiveVariables.LIVE_DECL_EQUIVALENCE;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerWithDelayedErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.PL;
import org.sosy_lab.cpachecker.core.interfaces.checker.StateChecker;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesCPA;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesState;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class UnusedVarsChecker
    implements StateChecker<LiveVariablesState>, CheckerWithDelayedErrorReport {

  private HashSet<CFAEdge> unUsedVars;
  private HashSet<CFAEdge> redundantAssignments;
  private final List<ErrorReport> errorStore;
  private Boolean globalIsAlwaysLive;


  public UnusedVarsChecker(Configuration config) {
    unUsedVars = new HashSet<>();
    redundantAssignments = new HashSet<>();
    errorStore = new ArrayList<>();
    globalIsAlwaysLive = false;
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.UNUSED_VARIABLE;
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return LiveVariablesState.class;
  }

  @Override
  public void checkAndRefine(
      LiveVariablesState pPostState,
      Collection<AbstractState> pPostOtherStates, CFAEdge pCfaEdge,
      Collection<LiveVariablesState> pNewPostStates) throws CPATransferException {
    pNewPostStates.add(pPostState);
  }

  @Override
  public Collection<ErrorReport> getErrorReport() {
    GlobalInfo gInfo = GlobalInfo.getInstance();
    if (gInfo.getCPA().isPresent()) {
      ARGCPA argCpa = (ARGCPA) gInfo.getCPA().get();
      CompositeCPA compCpa = (CompositeCPA) argCpa.getWrappedCPAs().get(0);
      for (ConfigurableProgramAnalysis cpa : compCpa.getWrappedCPAs()) {
        if (cpa instanceof LiveVariablesCPA) {
          LiveVariablesTransferRelation liveVariablesTransfer =
              (LiveVariablesTransferRelation) cpa.getTransferRelation();
          unUsedVars = liveVariablesTransfer.getUnusedSet();
          redundantAssignments = liveVariablesTransfer.getRedundantSet();
          globalIsAlwaysLive = liveVariablesTransfer.getGlobalAlwaysTrue();
          tryToRemoveUnusedVars(liveVariablesTransfer);
          handleUnusedVars();
          handleRedundantAssignments();
          break;
        }
      }
    }
    for (ErrorReport er : errorStore) {
      if (er.getErrorSpot().getASTNode().isPresent()) {
        System.out.println(er.getErrorSpot().getASTNode().get().toString());
      } else {
        System.out.println(er.getErrorSpot().getCFAEdge().get().getRawStatement());
      }
    }
    return errorStore;
  }

  public void handleUnusedVars() {
    for (CFAEdge curEdge : unUsedVars) {
      if (curEdge.getRawAST().isPresent()) {
        UnusedVarsErrorReport er =
            new UnusedVarsErrorReport((CAstNode) curEdge.getRawAST().get(), null);
        errorStore.add(er);
      } else {
        if (curEdge instanceof BlankEdge) {
          UnusedVarsErrorReport er = new UnusedVarsErrorReport(null, curEdge);
          errorStore.add(er);
        }
      }
    }
  }

  public void handleRedundantAssignments() {
    for (CFAEdge curEdge : redundantAssignments) {
      if (curEdge.getRawAST().isPresent()) {
        UnusedVarsErrorReport er =
            new UnusedVarsErrorReport((CAstNode) curEdge.getRawAST().get(), null);
        errorStore.add(er);
      } else {
        if (curEdge instanceof BlankEdge) {
          UnusedVarsErrorReport er = new UnusedVarsErrorReport(null, curEdge);
          errorStore.add(er);
        }
      }
    }
  }

  //try to remove some vars, for example, global vars (optional,
  //based on assumeGlobalVariablesAreAlwaysLive) and vars like:
  //int y,z=1; if(z==1) y=z;else y=2; if(y>1) ...
  //in branch y=z and y=2 , y is live.
  //In the successor node of int y, y is not live,
  //then y is handled as unused variable in our algorithm, which is wrong.
  //This methods removes wrong unused variables like that.

  public void tryToRemoveUnusedVars(LiveVariablesTransferRelation liveTR) {
    final Multimap<CFANode, Wrapper<ASimpleDeclaration>> liveVariables = liveTR.getLiveVariables();
    HashSet<CFAEdge> toRemoveEdges = new HashSet<>();
    for (CFAEdge curEdge : unUsedVars) {
      if (curEdge instanceof ADeclarationEdge) {
        ADeclaration decl = ((ADeclarationEdge) curEdge).getDeclaration();
        Wrapper<ASimpleDeclaration> varDecl = LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration) decl);
        if (liveVariables.containsValue(varDecl)) {
          toRemoveEdges.add(curEdge);
        }
      }
    }

    //remove decls of global vars if flag is true
    if (globalIsAlwaysLive) {
      for (CFAEdge curEdge : unUsedVars) {
        if (curEdge instanceof ADeclarationEdge) {
          ADeclaration decl = ((ADeclarationEdge) curEdge).getDeclaration();
          if (decl.isGlobal() == true) {
            toRemoveEdges.add(curEdge);
          }
        }
      }
      for (CFAEdge curEdge : redundantAssignments) {
        if (curEdge instanceof ADeclarationEdge) {
          ADeclaration decl = ((ADeclarationEdge) curEdge).getDeclaration();
          if (decl.isGlobal() == true) {
            toRemoveEdges.add(curEdge);
          }
        }
      }
    }
    for (CFAEdge curEdge : toRemoveEdges) {
      liveTR.addUsedEdge(curEdge);
    }
  }
}
