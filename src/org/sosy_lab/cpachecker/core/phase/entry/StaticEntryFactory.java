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
package org.sosy_lab.cpachecker.core.phase.entry;

import static org.sosy_lab.cpachecker.core.phase.entry.StaticEntryFactory.StaticEntryStrategyType.MAIN;

import com.google.common.base.Splitter;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "analysis.me")
public final class StaticEntryFactory {

  public enum StaticEntryStrategyType {
    MAIN,
    FUNCTION_HEAD,
    CHEAP_COVER,
    FIXED,
    TOP_K,
  }

  @Option(secure = true, name = "static.strategy", description = "strategy for selecting initial "
      + "analysis point statically")
  private StaticEntryStrategyType type = MAIN;

  // for fixed function entry
  @Option(secure = true, name = "static.fixed", description = "strategy for using the fixed function entry")
  private String fixedFunctionNames = null;

  private Configuration config;

  public StaticEntryFactory(Configuration pConfig) throws InvalidConfigurationException {
    this.config = pConfig;
    pConfig.inject(this);
  }

  public StaticEntryStrategy createStrategy() {
    switch (type) {
      case MAIN:
        return new MainEntryStrategy();
      case FUNCTION_HEAD:
        return new EntryWiseStrategy();
      case CHEAP_COVER:
        int depth = guessCallDepthLimit();
        return new CheapCoverStrategy(depth);
      case FIXED:
        return new FixedStrategy(
            Splitter.on(',').trimResults().omitEmptyStrings().splitToList(fixedFunctionNames)
        );
      case TOP_K:
      default:
        throw new UnsupportedOperationException("strategy not implemented for now");
    }
  }

  private int guessCallDepthLimit() {
    final String key = "cpa.boundary.callDepth";
    int depth = 0;    // default to 0
    if (config.hasProperty(key)) {
      String depthStr = config.getProperty(key);
      assert (depthStr != null);
      depth = Integer.parseInt(depthStr);
    }
    return depth;
  }

}
