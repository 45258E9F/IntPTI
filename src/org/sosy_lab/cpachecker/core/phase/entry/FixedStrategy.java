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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.Collection;
import java.util.List;

public class FixedStrategy implements StaticEntryStrategy {

  private List<String> targets;
  private List<CFANode> functions;

  public FixedStrategy(List<String> targets) {
    this.targets = ImmutableList.copyOf(targets);
  }

  @Override
  public Collection<CFANode> getInitialEntry(CFA pCFA) {
    return (functions == null) ? computeEntries() : functions;
  }

  private List<CFANode> computeEntries() {
    final CFA cfa = GlobalInfo.getInstance().getCFAInfo().get().getCFA();
    functions = FluentIterable.from(targets).transform(new Function<String, CFANode>() {
      @Override
      public CFANode apply(String name) {
        return cfa.getFunctionHead(name);
      }
    }).filter(Predicates.notNull()).toList();
    return functions;
  }
}
