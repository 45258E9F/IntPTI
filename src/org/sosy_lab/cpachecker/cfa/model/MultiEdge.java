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
package org.sosy_lab.cpachecker.cfa.model;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.Iterator;
import java.util.List;

/**
 * A single edge which represents a sequence of several other simple edges of
 * the types
 * BlankEdge
 * DeclarationEdge
 * StatementEdge
 * ReturnStatementEdge
 */
public class MultiEdge extends AbstractCFAEdge implements Iterable<CFAEdge> {

  private final ImmutableList<CFAEdge> edges;

  public MultiEdge(CFANode pPredecessor, CFANode pSuccessor, List<CFAEdge> pEdges) {
    super("",
        FileLocation.merge(
            Lists.transform(pEdges, new Function<CFAEdge, FileLocation>() {
              @Override
              public FileLocation apply(CFAEdge pInput) {
                return pInput.getFileLocation();
              }
            })),
        pPredecessor, pSuccessor);
    edges = ImmutableList.copyOf(pEdges);
  }

  public ImmutableList<CFAEdge> getEdges() {
    return edges;
  }

  @Override
  public Iterator<CFAEdge> iterator() {
    return edges.iterator();
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.MultiEdge;
  }

  @Override
  public final String getRawStatement() {
    return Joiner.on('\n').join(Lists.transform(edges, new Function<CFAEdge, String>() {

      @Override
      public String apply(CFAEdge pInput) {
        return pInput.getRawStatement();
      }
    }));
  }

  @Override
  public String getCode() {
    return Joiner.on('\n').join(Lists.transform(edges, new Function<CFAEdge, String>() {

      @Override
      public String apply(CFAEdge pInput) {
        return pInput.getCode();
      }
    }));
  }

  @Override
  public String getDescription() {
    return Joiner.on('\n').join(Lists.transform(edges, new Function<CFAEdge, String>() {

      @Override
      public String apply(CFAEdge pInput) {
        return pInput.getDescription();
      }
    }));
  }

  @Override
  public String toString() {
    return Joiner.on('\n').join(edges);
  }

  @Override
  public boolean equals(Object pOther) {
    if (!super.equals(pOther)) {
      return false;
    }

    if (!(pOther instanceof MultiEdge)) {
      return false;
    }
    MultiEdge otherEdge = (MultiEdge) pOther;
    return edges.equals(otherEdge.edges);
  }

  @Override
  public int hashCode() {
    return edges.hashCode();
  }
}
