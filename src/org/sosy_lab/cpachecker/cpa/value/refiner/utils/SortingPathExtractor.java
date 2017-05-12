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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import com.google.common.collect.FluentIterable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * {@link PathExtractor} that sorts paths by their length or interpolant quality.
 * To sort paths by their interpolant quality, set {@link #itpSortedTargets} by specifying
 * configuration property <code>cpa.value.refinement.itpSortedTargets</code>.
 */
@Options(prefix = "cpa.value.refinement")
public class SortingPathExtractor extends PathExtractor {

  @Option(
      secure = true,
      description = "heuristic to sort targets based on the quality of interpolants derivable from them")
  private boolean itpSortedTargets = false;

  private final PrefixSelector prefixSelector;
  private final PrefixProvider prefixProvider;

  public SortingPathExtractor(
      final PrefixProvider pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final LogManager pLogger,
      final Configuration pConfig
  ) throws InvalidConfigurationException {

    super(pLogger, pConfig);
    pConfig.inject(this, SortingPathExtractor.class);

    prefixProvider = pPrefixProvider;
    prefixSelector = pPrefixSelector;
  }

  /**
   * This method returns an unsorted, non-empty collection of target states
   * found during the analysis.
   *
   * @param pReached the set of reached states
   * @return the target states
   */
  @Override
  public Collection<ARGState> getTargetStates(final ARGReachedSet pReached)
      throws RefinementFailedException {

    // sort the list, to either favor shorter paths or better interpolants
    Comparator<ARGState> comparator = new Comparator<ARGState>() {
      @Override
      public int compare(ARGState target1, ARGState target2) {
        try {
          ARGPath path1 = ARGUtils.getOnePathTo(target1);
          ARGPath path2 = ARGUtils.getOnePathTo(target2);

          if (itpSortedTargets) {
            List<InfeasiblePrefix> prefixes1 = prefixProvider.extractInfeasiblePrefixes(path1);
            List<InfeasiblePrefix> prefixes2 = prefixProvider.extractInfeasiblePrefixes(path2);

            int score1 =
                prefixSelector.obtainScoreForPrefixes(prefixes1, PrefixPreference.DOMAIN_MIN);
            int score2 =
                prefixSelector.obtainScoreForPrefixes(prefixes2, PrefixPreference.DOMAIN_MIN);

            return score1 - score2;
          } else {
            return path1.size() - path2.size();
          }
        } catch (CPAException | InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    };

    return FluentIterable.from(super.getTargetStates(pReached)).toSortedList(comparator);
  }
}
