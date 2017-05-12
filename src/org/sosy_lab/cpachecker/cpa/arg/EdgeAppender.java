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
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;

import java.util.Collection;
import java.util.Map;

interface EdgeAppender {

  void appendNewEdge(
      final GraphMlBuilder pDoc,
      String pFrom,
      String pTo,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pFromState,
      Map<ARGState, CFAEdgeWithAssumptions> pValueMap);

  void appendNewEdgeToSink(
      final GraphMlBuilder pDoc,
      String pFrom,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pFromState,
      Map<ARGState, CFAEdgeWithAssumptions> pValueMap);

}