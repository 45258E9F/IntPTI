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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.argReplay.ARGReplayState;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage.AbstractionNode;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter.PrecisionConverter;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.FormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides additional predicates for a given source of information.
 * The predicates can be used in the precision adjustment.
 */
@Options(prefix = "cpa.predicate")
public class PredicateProvider {

  @Option(secure = true, description = "try to reuse old abstractions from file during strengthening")
  private boolean strengthenWithReusedAbstractions = false;

  @Option(description = "file that consists of old abstractions, to be used during strengthening")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path strengthenWithReusedAbstractionsFile = Paths.get("abstractions.txt");

  private final CFA cfa;
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final PredicateAbstractionManager predFmgr;

  private Multimap<Integer, BooleanFormula> abstractions = null; // lazy initialization

  PredicateProvider(
      Configuration config, CFA pCfa, LogManager pLogger,
      FormulaManagerView pFmgr, PredicateAbstractionManager pPredMgr)
      throws InvalidConfigurationException {
    config.inject(this);
    cfa = pCfa;
    logger = pLogger;
    fmgr = pFmgr;
    predFmgr = pPredMgr;
  }

  /**
   * Get predicates either extracted from the given state or
   * received from other source (i.e. file) related to the state.
   * The relation might just be the same location.
   */
  public Set<AbstractionPredicate> getPredicates(AbstractState pFullState)
      throws CPATransferException {
    Set<AbstractionPredicate> result = new HashSet<>();

    if (strengthenWithReusedAbstractions) {
      CFANode location = AbstractStates.extractLocation(pFullState);
      result.addAll(getPredicatesFromAbstractionFromFile(location));
    }

    for (ARGReplayState state : AbstractStates.asIterable(pFullState)
        .filter(ARGReplayState.class)) {
      result.addAll(getPredicatesFromState(state));
    }

    return result;
  }

  private Set<AbstractionPredicate> getPredicatesFromAbstractionFromFile(CFANode pLocation)
      throws CPATransferException {

    if (abstractions == null) { // lazy initialization
      PredicateAbstractionsStorage abstractionStorage;
      Converter converter = Converter.getConverter(PrecisionConverter.INT2BV, cfa, logger);
      try {
        abstractionStorage = new PredicateAbstractionsStorage(strengthenWithReusedAbstractionsFile,
            logger, fmgr, converter);
      } catch (PredicateParsingFailedException e) {
        throw new CPATransferException("cannot read abstractions from file, parsing fail", e);
      }

      abstractions = HashMultimap.create();
      for (AbstractionNode absNode : abstractionStorage.getAbstractions().values()) {
        Optional<Integer> location = absNode.getLocationId();
        if (location.isPresent()) {
          abstractions.put(location.get(), absNode.getFormula());
        }
      }
    }

    Set<AbstractionPredicate> result = new HashSet<>();
    for (BooleanFormula possibleConstraint : abstractions.get(pLocation.getNodeNumber())) {
      // lets try all available abstractions formulas, perhaps more of them are valid
      result.addAll(predFmgr.getPredicatesForAtomsOf(possibleConstraint));
    }
    return result;
  }

  private Set<AbstractionPredicate> getPredicatesFromState(ARGReplayState state) {
    Set<AbstractionPredicate> result = new HashSet<>();
    for (ARGState innerState : state.getStates()) {
      PredicateAbstractState oldPredicateState =
          AbstractStates.extractStateByType(innerState, PredicateAbstractState.class);
      if (oldPredicateState != null && oldPredicateState.isAbstractionState()) {
        PredicateCPA oldPredicateCPA = CPAs.retrieveCPA(state.getCPA(), PredicateCPA.class);
        result.addAll(getPredicatesFromAbstractionState(oldPredicateState, oldPredicateCPA));
        // we can either break here, or we use all available matching states.
      }
    }
    return result;
  }

  private Collection<AbstractionPredicate> getPredicatesFromAbstractionState(
      PredicateAbstractState pOldPredicateState, PredicateCPA oldPredicateCPA) {

    StringBuilder in = new StringBuilder();
    Converter converter = Converter.getConverter(PrecisionConverter.INT2BV, cfa, logger);
    Appender app = oldPredicateCPA.getTransferRelation().fmgr.dumpFormula(
        pOldPredicateState.getAbstractionFormula().asFormula());

    try {
      app.appendTo(in);
    } catch (IOException e) {
      throw new AssertionError(e.getMessage());
    }

    LogManagerWithoutDuplicates logger2 = new LogManagerWithoutDuplicates(logger);
    StringBuilder out = new StringBuilder();
    for (String line : in.toString().split("\n")) {
      line = FormulaParser.convertFormula(Preconditions.checkNotNull(converter), line, logger2);
      if (line != null) {
        out.append(line).append("\n");
      }
    }

    BooleanFormula constraint = this.fmgr.parse(out.toString());
    return predFmgr.getPredicatesForAtomsOf(constraint);
  }
}
