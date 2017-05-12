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
package org.sosy_lab.cpachecker.core.summary.manage;

import static org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName.ACCESS_LOOP_INTERNAL;
import static org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName.ACCESS_SUMMARY;
import static org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName.ARITH_INVARIANT;
import static org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName.ARITH_SUMMARY;
import static org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName.RANGE_LOOP_INTERNAL;
import static org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName.RANGE_SUMMARY;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummaryType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Provide Global Access To Summary Stores Summary Store is a storage of all summary instances for a
 * certain type (e.g., AccessSummaryStore).
 */
public final class SummaryProvider {

  public enum SummaryName {
    ACCESS_SUMMARY,
    // skipping fun, loop
    ACCESS_LOOP_INTERNAL,
    // entering loop
    ARITH_SUMMARY,
    // skipping fun, loop
    POINTER_SUMMARY,
    // skipping fun, loop
    ARITH_INVARIANT,
    // entering loop
    RANGE_SUMMARY,
    // skipping fun, loop
    RANGE_LOOP_INTERNAL,            // entering loop
  }

  @Options(prefix = "summary")
  private static class SummaryProviderOptions {

    @Option(secure = true, name = "usedExternalSummary", description = "the set of summary to be "
        + "computed and used externally")
    private Set<SummaryName> usedExternalSummaryNames = Sets.newHashSet();

    @Option(secure = true, name = "usedInternalSummary", description = "the set of summary to be "
        + "computed and used internally (for deriving property inside the loop)")
    private Set<SummaryName> usedInternalSummaryNames = Sets.newHashSet();

    private List<SummaryName> internalSummary;
    private List<SummaryName> externalSummary;

    SummaryProviderOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
      // summary application should have correct sequence
      internalSummary = extractUsedSummary(usedInternalSummaryNames,
          ACCESS_LOOP_INTERNAL, // we use this for access loop internal summary
          RANGE_LOOP_INTERNAL,
          ARITH_INVARIANT);
      externalSummary = extractUsedSummary(usedExternalSummaryNames,
          ACCESS_SUMMARY,
          RANGE_SUMMARY,
          ARITH_SUMMARY);
    }

    ImmutableList<SummaryName> extractUsedSummary(
        Set<SummaryName> specified, SummaryName...
        names) {
      ImmutableList.Builder<SummaryName> builder = ImmutableList.builder();
      for (SummaryName name : names) {
        if (specified.contains(name)) {
          builder.add(name);
        }
      }
      return builder.build();
    }

  }

  private static Map<SummaryName, LoopSummaryStore<?>> loopSummary = Maps.newHashMap();
  private static Map<SummaryName, FunctionSummaryStore<?>> functionSummary = Maps.newHashMap();

  /* ***************************************** */
  /* summary cache for activated summary names */
  /* ***************************************** */

  private static SummaryProviderOptions options = null;
  private static List<LoopSummaryStore<?>> activeInternalLoopSummary = Lists.newArrayList();
  private static List<LoopSummaryStore<?>> activeExternalLoopSummary = Lists.newArrayList();
  private static List<FunctionSummaryStore<?>> activeFunctionSummary = Lists.newArrayList();

  /**
   * This method should be invoked only once at the very first of summary computation phase.
   */
  public static void initialize(Configuration pConfig) throws InvalidConfigurationException {
    if (options == null) {
      options = new SummaryProviderOptions(pConfig);
    }
  }

  public static void registerFunctionSummary(
      SummaryName name,
      FunctionSummaryStore<? extends SummaryInstance> summary) {
    functionSummary.put(name, summary);
  }

  public static void registerLoopSummary(
      SummaryName name,
      LoopSummaryStore<? extends SummaryInstance> summary) {
    loopSummary.put(name, summary);
  }

  public static List<LoopSummaryStore<?>> getExternalLoopSummary() {
    return activeExternalLoopSummary;
  }

  public static List<LoopSummaryStore<?>> getInternalLoopSummary() {
    return activeInternalLoopSummary;
  }

  public static List<FunctionSummaryStore<?>> getFunctionSummary() {
    if (activeFunctionSummary == null) {
      activeFunctionSummary = FluentIterable.from(options.externalSummary)
          .transform(new Function<SummaryName, FunctionSummaryStore<?>>() {
            @Override
            public FunctionSummaryStore<?> apply(SummaryName name) {
              return functionSummary.get(name);
            }
          }).filter(Predicates.notNull()).toList();
    }
    return activeFunctionSummary;
  }

  @SuppressWarnings("unchecked")
  public static void registerSummary(
      SummaryType type,
      SummaryName summaryName,
      SummaryStore summary) {
    if (type == SummaryType.FUNCTION_SUMMARY) {
      SummaryProvider.registerFunctionSummary(summaryName,
          (FunctionSummaryStore<? extends SummaryInstance>) summary);
      // update active function summary list
      activeFunctionSummary = FluentIterable.from(options.externalSummary)
          .transform(new Function<SummaryName, FunctionSummaryStore<?>>() {
            @Override
            public FunctionSummaryStore<?> apply(SummaryName pSummaryName) {
              return functionSummary.get(pSummaryName);
            }
          }).filter(Predicates.notNull()).toList();
    } else if (type == SummaryType.LOOP_SUMMARY) {
      SummaryProvider
          .registerLoopSummary(summaryName, (LoopSummaryStore<? extends SummaryInstance>) summary);
      // update active loop summary list
      activeExternalLoopSummary = FluentIterable.from(options.externalSummary)
          .transform(new Function<SummaryName, LoopSummaryStore<?>>() {
            @Override
            public LoopSummaryStore<?> apply(SummaryName pSummaryName) {
              return loopSummary.get(pSummaryName);
            }
          }).filter(Predicates.notNull()).toList();
      activeInternalLoopSummary = FluentIterable.from(options.internalSummary)
          .transform(new Function<SummaryName, LoopSummaryStore<?>>() {
            @Override
            public LoopSummaryStore<?> apply(SummaryName pSummaryName) {
              return loopSummary.get(pSummaryName);
            }
          }).filter(Predicates.notNull()).toList();
    } else {
      throw new IllegalArgumentException("Unknown summary type: " + type);
    }
  }

}
