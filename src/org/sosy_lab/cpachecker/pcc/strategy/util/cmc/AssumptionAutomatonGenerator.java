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
package org.sosy_lab.cpachecker.pcc.strategy.util.cmc;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

@Options(prefix = "pcc.cmc")
public class AssumptionAutomatonGenerator {

  private final LogManager logger;

  @Option(secure = true, name = "file", description = "write collected assumptions to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionsFile = Paths.get("assumptions.txt");

  public AssumptionAutomatonGenerator(final Configuration config, final LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  public Set<ARGState> getAllAncestorsFor(final Collection<ARGState> nodes) {
    TreeSet<ARGState> uncoveredAncestors = new TreeSet<>(nodes);
    Deque<ARGState> toAdd = new ArrayDeque<>(nodes);

    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (uncoveredAncestors.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }
    return uncoveredAncestors;
  }

  public void writeAutomaton(final ARGState root, final List<ARGState> incompleteNodes)
      throws CPAException {
    assert (notCovered(incompleteNodes));

    try (Writer w = Files.openOutputFile(assumptionsFile)) {
      logger.log(Level.FINEST, "Write assumption automaton to file ", assumptionsFile);
      AssumptionCollectorAlgorithm.writeAutomaton(w, root, getAllAncestorsFor(incompleteNodes),
          new HashSet<AbstractState>(incompleteNodes), 0, true);
    } catch (IOException e) {
      logger
          .log(Level.SEVERE, "Could not write assumption automaton for next partial ARG checking");
      throw new CPAException("Assumption automaton writing failed", e);
    }
  }

  private boolean notCovered(final List<ARGState> nodes) {
    for (ARGState state : nodes) {
      if (state.isCovered()) {
        return false;
      }
    }
    return true;
  }

}
