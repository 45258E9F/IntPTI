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
package org.sosy_lab.cpachecker.cpa.apron;

import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import apron.Box;
import apron.Manager;
import apron.Octagon;
import apron.Polka;
import apron.PolkaEq;
import apron.SetUp;

@Options(prefix = "cpa.apron")
public class ApronManager {

  static {
    SetUp.init(
        NativeLibraries.getNativeLibraryPath().resolve("apron").getAbsolutePath());
  }

  @Option(secure = true, name = "domain", toUppercase = true, values = {"BOX", "OCTAGON", "POLKA", "POLKA_STRICT", "POLKA_EQ"},
      description = "Use this to change the underlying abstract domain in the APRON library")
  private String domainType = "OCTAGON";

  private Manager manager;

  public ApronManager(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    if (domainType.equals("BOX")) {
      manager = new Box();
    } else if (domainType.equals("OCTAGON")) {
      manager = new Octagon();
    } else if (domainType.equals("POLKA")) {
      manager = new Polka(false);
    } else if (domainType.equals("POLKA_STRICT")) {
      manager = new Polka(true);
    } else if (domainType.equals("POLKA_EQ")) {
      manager = new PolkaEq();
    } else {
      throw new InvalidConfigurationException("Invalid argument for domain option.");
    }
  }

  public Manager getManager() {
    return manager;
  }
}