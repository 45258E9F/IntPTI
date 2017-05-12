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
package org.sosy_lab.cpachecker.util.automaton;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;

import java.util.Objects;

public class CachingTargetLocationProvider implements TargetLocationProvider {

  private final TargetLocationProvider backingTargetLocationProvider;

  private final LoadingCache<CacheKey, ImmutableSet<CFANode>> cache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .weakValues()
          .<CacheKey, ImmutableSet<CFANode>>build(
              new CacheLoader<CacheKey, ImmutableSet<CFANode>>() {

                @Override
                public ImmutableSet<CFANode> load(CacheKey pCacheKey) {
                  return backingTargetLocationProvider.tryGetAutomatonTargetLocations(
                      pCacheKey.node, pCacheKey.automaton);
                }
              });

  public CachingTargetLocationProvider(TargetLocationProvider pBackingTargetLocationProvider) {
    this.backingTargetLocationProvider = pBackingTargetLocationProvider;
  }

  public CachingTargetLocationProvider(
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier,
      LogManager pLogManager, Configuration pConfig, CFA pCfa) {
    this(new TargetLocationProviderImpl(pReachedSetFactory, pShutdownNotifier, pLogManager, pConfig,
        pCfa));
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(CFANode pRootNode) {
    return tryGetAutomatonTargetLocations(pRootNode, Optional.<Automaton>absent());
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      CFANode pRootNode, Optional<Automaton> pAutomaton) {
    return cache.getUnchecked(new CacheKey(pRootNode, pAutomaton));
  }

  private static class CacheKey {

    private final CFANode node;

    private final Optional<Automaton> automaton;

    public CacheKey(CFANode pNode, Optional<Automaton> pAutomaton) {
      node = pNode;
      automaton = pAutomaton;
    }

    @Override
    public String toString() {
      return node + ": " + automaton;
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, automaton);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof CacheKey) {
        CacheKey other = (CacheKey) pObj;
        return node.equals(other.node) && automaton.equals(other.automaton);
      }
      return false;
    }

  }

}
