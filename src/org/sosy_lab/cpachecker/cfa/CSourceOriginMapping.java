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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import org.sosy_lab.cpachecker.util.Pair;

import java.util.HashMap;
import java.util.Map;


public class CSourceOriginMapping {

  // Each RangeMap in this map contains the mapping for one input file,
  // from its lines to the tuple of (originalFile, lineDelta).
  // The full mapping is a map with those RangeMaps as values,
  // one for each input file.
  private final Map<String, RangeMap<Integer, Pair<String, Integer>>> mapping = new HashMap<>();

  void mapInputLineRangeToDelta(
      String inputFilename,
      String originFilename,
      int fromInputLineNumber,
      int toInputLineNumber,
      int deltaLinesToOrigin) {
    RangeMap<Integer, Pair<String, Integer>> fileMapping = mapping.get(inputFilename);
    if (fileMapping == null) {
      fileMapping = TreeRangeMap.create();
      mapping.put(inputFilename, fileMapping);
    }

    Range<Integer> lineRange = Range.openClosed(fromInputLineNumber - 1, toInputLineNumber);
    fileMapping.put(lineRange, Pair.of(originFilename, deltaLinesToOrigin));
  }

  public Pair<String, Integer> getOriginLineFromAnalysisCodeLine(
      String analysisFile, int analysisCodeLine) {
    RangeMap<Integer, Pair<String, Integer>> fileMapping = mapping.get(analysisFile);

    if (fileMapping != null) {
      Pair<String, Integer> originFileAndLineDelta = fileMapping.get(analysisCodeLine);

      if (originFileAndLineDelta != null) {
        return Pair.of(originFileAndLineDelta.getFirst(),
            analysisCodeLine + originFileAndLineDelta.getSecond());
      }
    }
    return Pair.of(analysisFile, analysisCodeLine);
  }
}
