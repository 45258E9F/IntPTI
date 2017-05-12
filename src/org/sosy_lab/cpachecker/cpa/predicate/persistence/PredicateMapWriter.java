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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.LINE_JOINER;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.splitFormula;

import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateDumpFormat;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class writes a set of predicates to a file in the same format that is
 * also used by {@link PredicateMapParser}.
 */
@Options(prefix = "cpa.predicate")
public class PredicateMapWriter {

  @Option(secure = true, name = "predmap.predicateFormat",
      description = "Format for exporting predicates from precisions.")
  private PredicateDumpFormat format = PredicateDumpFormat.SMTLIB2;

  private final FormulaManagerView fmgr;

  public PredicateMapWriter(Configuration config, FormulaManagerView pFmgr)
      throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
  }

  public void writePredicateMap(
      SetMultimap<Pair<CFANode, Integer>,
          AbstractionPredicate> locationInstancePredicates,
      SetMultimap<CFANode, AbstractionPredicate> localPredicates,
      SetMultimap<String, AbstractionPredicate> functionPredicates,
      Set<AbstractionPredicate> globalPredicates,
      Collection<AbstractionPredicate> allPredicates,
      Appendable sb) throws IOException {

    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<AbstractionPredicate, String> predToString = Maps.newHashMap();

    // fill the above set and map
    for (AbstractionPredicate pred : allPredicates) {
      String predString;

      if (format == PredicateDumpFormat.SMTLIB2) {
        Pair<String, List<String>> p = splitFormula(fmgr, pred.getSymbolicAtom());
        predString = p.getFirst();
        definitions.addAll(p.getSecond());
      } else {
        predString = pred.getSymbolicAtom().toString();
      }

      predToString.put(pred, predString);
    }

    LINE_JOINER.appendTo(sb, definitions);
    sb.append("\n\n");

    writeSetOfPredicates(sb, "*", globalPredicates, predToString);

    for (Entry<String, Collection<AbstractionPredicate>> e : functionPredicates.asMap()
        .entrySet()) {
      writeSetOfPredicates(sb, e.getKey(), e.getValue(), predToString);
    }

    for (Entry<CFANode, Collection<AbstractionPredicate>> e : localPredicates.asMap().entrySet()) {
      String key = e.getKey().getFunctionName() + " " + e.getKey().toString();
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }

    for (Entry<Pair<CFANode, Integer>, Collection<AbstractionPredicate>> e : locationInstancePredicates
        .asMap().entrySet()) {
      CFANode loc = e.getKey().getFirst();
      String key = loc.getFunctionName()
          + " " + loc.toString() + "@" + e.getKey().getSecond();
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }
  }

  private void writeSetOfPredicates(
      Appendable sb, String key,
      Collection<AbstractionPredicate> predicates,
      Map<AbstractionPredicate, String> predToString) throws IOException {
    if (!predicates.isEmpty()) {
      sb.append(key);
      sb.append(":\n");
      for (AbstractionPredicate pred : predicates) {
        sb.append(checkNotNull(predToString.get(pred)));
        sb.append('\n');
      }
      sb.append('\n');
    }
  }
}
