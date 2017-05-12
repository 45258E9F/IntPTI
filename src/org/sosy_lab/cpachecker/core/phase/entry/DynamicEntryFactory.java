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
package org.sosy_lab.cpachecker.core.phase.entry;

import static org.sosy_lab.cpachecker.core.phase.entry.DynamicEntryFactory.DynamicEntryStrategyType.VOID;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "analysis.me")
public final class DynamicEntryFactory {

  public enum DynamicEntryStrategyType {
    VOID,
    BOUND
  }

  @Option(secure = true, name = "dynamic.strategy", description = "strategy for deriving entry "
      + "point for analysis dynamically")
  private DynamicEntryStrategyType type = VOID;

  public DynamicEntryFactory(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public DynamicEntryStrategy createStrategy() {
    switch (type) {
      case VOID:
        return new VoidStrategy();
      case BOUND:
        return new BoundedStrategy();
      default:
        throw new UnsupportedOperationException("strategy not implemented for now");
    }
  }

}
