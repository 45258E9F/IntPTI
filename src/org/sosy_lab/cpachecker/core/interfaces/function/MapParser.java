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

import com.google.common.base.Splitter;

import org.sosy_lab.common.io.Path;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A utility class that parses map file into mapping data structure.
 */
public final class MapParser {

  private MapParser() {
  }

  private final static Splitter ALIAS_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults();
  private final static Splitter TARGET_SPLITTER = Splitter.on(' ').omitEmptyStrings().trimResults();

  public static void loadFromFile(Path file, Map<String, String> handlerMap) {
    if (!file.exists()) {
      // then nothing is loaded to mapping
      return;
    }
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(file.toFile()));
      String lineBuffer;
      while ((lineBuffer = br.readLine()) != null) {
        List<String> alias = ALIAS_SPLITTER.splitToList(lineBuffer);
        if (alias.size() < 1) {
          continue;
        }
        String mainEntity = alias.get(0);
        handlerMap.put(mainEntity, mainEntity);
        if (alias.size() > 1) {
          String aliases = alias.get(1);
          List<String> targets = TARGET_SPLITTER.splitToList(aliases);
          for (String target : targets) {
            handlerMap.put(target, mainEntity);
          }
        }
      }
      br.close();
    } catch (IOException ex) {
      throw new UnsupportedOperationException("Failed to load map file: " + file.getAbsolutePath());
    }
  }

}
