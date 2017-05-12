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
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

import java.util.Collection;

public interface InvariantProvider {

  ExpressionTree<Object> provideInvariantFor(
      CFAEdge pCFAEdge, Optional<? extends Collection<? extends ARGState>> pStates);

  static enum TrueInvariantProvider implements InvariantProvider {
    INSTANCE;

    @Override
    public ExpressionTree<Object> provideInvariantFor(
        CFAEdge pCFAEdge, Optional<? extends Collection<? extends ARGState>> pStates) {
      return ExpressionTrees.getTrue();
    }
  }
}
