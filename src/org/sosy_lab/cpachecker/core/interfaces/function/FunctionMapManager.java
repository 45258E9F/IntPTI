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
package org.sosy_lab.cpachecker.core.interfaces.function;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A manager class for store and query of function mappings.
 */
@Options(prefix = "function")
public class FunctionMapManager {

  @Option(secure = true, name = "adapters", description = "a list of registered function adapters")
  private Set<String> adapterNames = Sets.newHashSet();

  @Option(secure = true, name = "stopFunctions", description = "functions that interrupt the "
      + "execution")
  private Set<String> predefinedStopFunctions = Sets.newHashSet();

  private Map<String, Path> functionMaps;
  private Set<String> stopFunctions;

  public FunctionMapManager(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    functionMaps = Maps.newHashMap();
    stopFunctions = Sets.newHashSet();
    stopFunctions.addAll(predefinedStopFunctions);
    // create a mapping from function adaptor class to corresponding path of map file
    for (String adaptorName : adapterNames) {
      String key = "function.".concat(adaptorName);
      String value = config.getProperty(key);
      if (value != null) {
        Path mapPath = Paths.get(value);
        if (mapPath.exists()) {
          // if the specified path does not exist, then we do not insert this record into mapping
          functionMaps.put(adaptorName, mapPath);
        }
      }
    }
  }

  public
  @Nullable
  Path getMapFilePath(Class<?> adaptorClass) {
    String simpleName = adaptorClass.getSimpleName();
    return functionMaps.get(simpleName);
  }

  public boolean queryActiveness(Class<?> adapterClass) {
    String simpleName = adapterClass.getSimpleName();
    return adapterNames.contains(simpleName);
  }

  public void addStopFunction(String functionName) {
    stopFunctions.add(functionName);
  }

  public boolean isStopFunction(String functionName) {
    return stopFunctions.contains(functionName);
  }

}
