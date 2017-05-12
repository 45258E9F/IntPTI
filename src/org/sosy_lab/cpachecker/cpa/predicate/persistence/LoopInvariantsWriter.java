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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.Maps;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


public class LoopInvariantsWriter {

  private final CFA cfa;
  private final LogManager logger;
  private final AbstractionManager absmgr;
  private final FormulaManagerView fmgr;
  private final RegionManager rmgr;

  public LoopInvariantsWriter(
      CFA pCfa, LogManager pLogger, AbstractionManager pAbsMgr,
      FormulaManagerView pFmMgr, RegionManager pRegMgr) {
    this.cfa = pCfa;
    this.logger = pLogger;
    this.absmgr = pAbsMgr;
    this.fmgr = pFmMgr;
    this.rmgr = pRegMgr;
  }

  private Map<CFANode, Region> getLoopHeadInvariants(ReachedSet reached) {
    if (!cfa.getAllLoopHeads().isPresent()) {
      logger.log(Level.WARNING,
          "Cannot dump loop invariants because loop-structure information is not available.");
      return null;
    }

    Map<CFANode, Region> regions = Maps.newHashMap();

    for (AbstractState state : reached) {
      CFANode loc = extractLocation(state);
      if (cfa.getAllLoopHeads().get().contains(loc)) {
        PredicateAbstractState predicateState = getPredicateState(state);
        if (!predicateState.isAbstractionState()) {
          logger.log(Level.WARNING,
              "Cannot dump loop invariants because a non-abstraction state was found for a loop-head location.");
          return null;
        }

        Region region = firstNonNull(regions.get(loc), rmgr.makeFalse());
        region = rmgr.makeOr(region, predicateState.getAbstractionFormula().asRegion());
        regions.put(loc, region);
      }
    }

    return regions;
  }

  public void exportLoopInvariants(Path invariantsFile, ReachedSet reached) {
    Map<CFANode, Region> regions = getLoopHeadInvariants(reached);
    if (regions == null) {
      return;
    }

    try (Writer writer = Files.openOutputFile(invariantsFile)) {
      for (CFANode loc : from(cfa.getAllLoopHeads().get())
          .toSortedSet(CFAUtils.NODE_NUMBER_COMPARATOR)) {

        Region region = firstNonNull(regions.get(loc), rmgr.makeFalse());
        BooleanFormula formula = absmgr.convertRegionToFormula(region);

        writer.append("loop__");
        writer.append(loc.getFunctionName());
        writer.append("__");
        writer.append(
            "" + ((loc.getNumLeavingEdges() == 0) ? 0 : loc.getLeavingEdge(0).getLineNumber()));
        writer.append(":\n");
        fmgr.dumpFormula(formula).appendTo(writer);
        writer.append('\n');
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write loop invariants to file");
    }
  }

  public void exportLoopInvariantsAsPrecision(Path invariantPrecisionsFile, ReachedSet reached) {
    Map<CFANode, Region> regions = getLoopHeadInvariants(reached);
    if (regions == null) {
      return;
    }

    Set<String> uniqueDefs = new HashSet<>();
    StringBuilder asserts = new StringBuilder();

    try (Writer writer = Files.openOutputFile(invariantPrecisionsFile)) {
      for (CFANode loc : from(cfa.getAllLoopHeads().get())
          .toSortedSet(CFAUtils.NODE_NUMBER_COMPARATOR)) {
        Region region = firstNonNull(regions.get(loc), rmgr.makeFalse());
        BooleanFormula formula = absmgr.convertRegionToFormula(region);
        Pair<String, List<String>> locInvariant =
            PredicatePersistenceUtils.splitFormula(fmgr, formula);

        for (String def : locInvariant.getSecond()) {
          if (uniqueDefs.add(def)) {
            writer.append(def);
            writer.append("\n");
          }
        }

        asserts.append(loc.getFunctionName());
        asserts.append(" ");
        asserts.append(loc.toString());
        asserts.append(":\n");
        asserts.append(locInvariant.getFirst());
        asserts.append("\n\n");
      }

      writer.append("\n");
      writer.append(asserts);

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write loop invariants to file");
    }
  }


}
