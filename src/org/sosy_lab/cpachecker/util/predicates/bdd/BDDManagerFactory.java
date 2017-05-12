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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

/**
 * Factory for creating a RegionManager for one of the available BDD packages
 * (chosen according to configuration).
 */
@Options(prefix = "bdd")
public class BDDManagerFactory {

  @Option(secure = true, name = "package",
      description = "Which BDD package should be used?"
          + "\n- java:   JavaBDD (default, no dependencies, many features)"
          + "\n- sylvan: Sylvan (only 64bit Linux, uses multiple threads)"
          + "\n- cudd:   CUDD (native library required, reordering not supported)"
          + "\n- micro:  MicroFactory (maximum number of BDD variables is 1024, slow, but less memory-comsumption)"
          + "\n- buddy:  Buddy (native library required)"
          + "\n- cal:    CAL (native library required)"
          + "\n- jdd:    JDD",
      values = {"JAVA", "SYLVAN", "CUDD", "MICRO", "BUDDY", "CAL", "JDD"},
      toUppercase = true)
  // documentation of the packages can be found at source of BDDFactory.init()
  private String bddPackage = "JAVA";

  private final Configuration config;
  private final LogManager logger;

  public BDDManagerFactory(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
  }

  public RegionManager createRegionManager() throws InvalidConfigurationException {
    if (bddPackage.equals("SYLVAN")) {
      return new SylvanBDDRegionManager(config, logger);
    } else {
      return new JavaBDDRegionManager(bddPackage, config, logger);
    }
  }
}
