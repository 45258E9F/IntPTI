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
package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

@Options(prefix = "cpa.bam")
public class BAMCache {

  @Option(secure = true, description = "if enabled, cache queries also consider blocks with non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  @Option(secure = true, description = "if enabled, the reached set cache is analysed for each cache miss to find the cause of the miss.")
  boolean gatherCacheMissStatistics = false;

  final Timer hashingTimer = new Timer();
  final Timer equalsTimer = new Timer();
  final Timer searchingTimer = new Timer();

  int cacheMisses = 0;
  int partialCacheHits = 0;
  int fullCacheHits = 0;

  int abstractionCausedMisses = 0;
  int precisionCausedMisses = 0;
  int noSimilarCausedMisses = 0;

  // we use LinkedHashMaps to avoid non-determinism
  private final Map<AbstractStateHash, ReachedSet> preciseReachedCache = new LinkedHashMap<>();
  private final Map<AbstractStateHash, ReachedSet> unpreciseReachedCache = new HashMap<>();
  private final Map<AbstractStateHash, Collection<AbstractState>> returnCache = new HashMap<>();
  private final Map<AbstractStateHash, ARGState> blockARGCache = new HashMap<>();

  private ARGState lastAnalyzedBlock = null;
  private final Reducer reducer;

  private final LogManager logger;

  public BAMCache(Configuration config, Reducer reducer, LogManager logger)
      throws InvalidConfigurationException {
    config.inject(this);
    this.reducer = reducer;
    this.logger = logger;
  }

  public boolean doesAggressiveCaching() {
    return aggressiveCaching;
  }

  private AbstractStateHash getHashCode(
      AbstractState stateKey,
      Precision precisionKey,
      Block context) {
    return new AbstractStateHash(stateKey, precisionKey, context);
  }

  public void put(AbstractState stateKey, Precision precisionKey, Block context, ReachedSet item) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    assert !preciseReachedCache.containsKey(hash);
    preciseReachedCache.put(hash, item);
  }

  public void put(
      AbstractState stateKey, Precision precisionKey, Block context, Collection<AbstractState> item,
      ARGState rootOfBlock) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    assert preciseReachedCache.get(hash) != null : "key not found in cache";
    assert allStatesContainedInReachedSet(item, preciseReachedCache.get(hash))
        : "output-states must be in reached-set";
    returnCache.put(hash, item);
    blockARGCache.put(hash, rootOfBlock);
    setLastAnalyzedBlock(hash);
  }

  private boolean allStatesContainedInReachedSet(
      Collection<AbstractState> pElements,
      ReachedSet reached) {
    for (AbstractState e : pElements) {
      if (!reached.contains(e)) {
        return false;
      }
    }
    return true;
  }

  public void removeReturnEntry(AbstractState stateKey, Precision precisionKey, Block context) {
    returnCache.remove(getHashCode(stateKey, precisionKey, context));
  }

  public void removeBlockEntry(AbstractState stateKey, Precision precisionKey, Block context) {
    blockARGCache.remove(getHashCode(stateKey, precisionKey, context));
  }

  /**
   * This function returns a Pair of the reached-set and the returnStates for the given keys.
   * Both members of the returned Pair are NULL, if there is a cache miss.
   * For a partial cache hit we return the partly computed reached-set and NULL as returnStates.
   */
  public Pair<ReachedSet, Collection<AbstractState>> get(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context) {

    final Pair<ReachedSet, Collection<AbstractState>> pair = get0(stateKey, precisionKey, context);
    Preconditions.checkNotNull(pair);

    // get some statistics
    final ReachedSet reached = pair.getFirst();
    final Collection<AbstractState> returnStates = pair.getSecond();

    if (reached != null && returnStates != null) { // we have reached-set and elements
      assert allStatesContainedInReachedSet(returnStates, reached)
          : "output-states must be in reached-set";
      fullCacheHits++;
    } else if (reached != null) { // we have cached a partly computed reached-set
      partialCacheHits++;
    } else if (returnStates == null) {
      cacheMisses++;
      if (gatherCacheMissStatistics) {
        findCacheMissCause(stateKey, precisionKey, context);
      }
    } else {
      throw new AssertionError("invalid return-value for BAMCache.get(): " + pair);
    }

    return pair;
  }

  private Pair<ReachedSet, Collection<AbstractState>> get0(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);

    ReachedSet result = preciseReachedCache.get(hash);
    if (result != null) {
      setLastAnalyzedBlock(hash);
      logger.log(Level.FINEST, "CACHE_ACCESS: precise entry");
      return Pair.of(result, returnCache.get(hash));
    }

    if (aggressiveCaching) {
      result = unpreciseReachedCache.get(hash);
      if (result != null) {
        AbstractStateHash unpreciseHash =
            getHashCode(stateKey, result.getPrecision(result.getFirstState()), context);
        setLastAnalyzedBlock(unpreciseHash);
        logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, directly from cache");
        return Pair.of(result, returnCache.get(unpreciseHash));
      }

      //search for similar entry
      Pair<ReachedSet, Collection<AbstractState>> pair =
          lookForSimilarState(stateKey, precisionKey, context);
      if (pair != null) {
        //found similar element, use this
        unpreciseReachedCache.put(hash, pair.getFirst());
        setLastAnalyzedBlock(
            getHashCode(stateKey, pair.getFirst().getPrecision(pair.getFirst().getFirstState()),
                context));
        logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, searched in cache");
        return pair;
      }
    }

    lastAnalyzedBlock = null;
    logger.log(Level.FINEST, "CACHE_ACCESS: entry not available");
    return Pair.of(null, null);
  }

  private void setLastAnalyzedBlock(AbstractStateHash pHash) {
    if (BAMTransferRelation.PCCInformation.isPCCEnabled()) {
      lastAnalyzedBlock = blockARGCache.get(pHash);
    }
  }

  public ARGState getLastAnalyzedBlock() {
    return lastAnalyzedBlock;
  }

  private Pair<ReachedSet, Collection<AbstractState>> lookForSimilarState(
      AbstractState pStateKey,
      Precision pPrecisionKey, Block pContext) {
    searchingTimer.start();
    try {
      int min = Integer.MAX_VALUE;
      Pair<ReachedSet, Collection<AbstractState>> result = null;

      for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
        //searchKey != cacheKey, check whether it is the same if we ignore the precision
        AbstractStateHash ignorePrecisionSearchKey =
            getHashCode(pStateKey, cacheKey.precisionKey, pContext);
        if (ignorePrecisionSearchKey.equals(cacheKey)) {
          int distance = reducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
          if (distance < min) { //prefer similar precisions
            min = distance;
            result = Pair.of(
                preciseReachedCache.get(ignorePrecisionSearchKey),
                returnCache.get(ignorePrecisionSearchKey));
          }
        }
      }

      return result;
    } finally {
      searchingTimer.stop();
    }
  }

  private void findCacheMissCause(
      AbstractState pStateKey,
      Precision pPrecisionKey,
      Block pContext) {
    AbstractStateHash searchKey = getHashCode(pStateKey, pPrecisionKey, pContext);
    for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
      assert !searchKey.equals(cacheKey);
      //searchKey != cacheKey, check whether it is the same if we ignore the precision
      AbstractStateHash ignorePrecisionSearchKey =
          getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        precisionCausedMisses++;
        return;
      }
      //precision was not the cause. Check abstraction.
      AbstractStateHash ignoreAbsSearchKey =
          getHashCode(cacheKey.stateKey, pPrecisionKey, pContext);
      if (ignoreAbsSearchKey.equals(cacheKey)) {
        abstractionCausedMisses++;
        return;
      }
    }
    noSimilarCausedMisses++;
  }

  public void clear() {
    preciseReachedCache.clear();
    unpreciseReachedCache.clear();
    returnCache.clear();
  }

  public boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    return preciseReachedCache.containsKey(hash);
  }

  public void updatePrecisionForEntry(
      AbstractState stateKey, Precision precisionKey, Block context,
      Precision newPrecisionKey) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    ReachedSet reachedSet = preciseReachedCache.get(hash);
    if (reachedSet != null) {
      preciseReachedCache.remove(hash);
      preciseReachedCache.put(getHashCode(stateKey, newPrecisionKey, context), reachedSet);
    }
  }

  public Collection<ReachedSet> getAllCachedReachedStates() {
    return preciseReachedCache.values();
  }

  private class AbstractStateHash {

    private final Object wrappedHash;
    private final Block context;
    private final AbstractState stateKey;
    private final Precision precisionKey;

    public AbstractStateHash(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
      wrappedHash = reducer.getHashCodeForState(pStateKey, pPrecisionKey);
      context = checkNotNull(pContext);
      stateKey = pStateKey;
      precisionKey = pPrecisionKey;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof AbstractStateHash)) {
        return false;
      }
      AbstractStateHash other = (AbstractStateHash) pObj;
      equalsTimer.start();
      try {
        return context.equals(other.context)
            && wrappedHash.equals(other.wrappedHash);
      } finally {
        equalsTimer.stop();
      }
    }

    @Override
    public int hashCode() {
      hashingTimer.start();
      try {
        return wrappedHash.hashCode() * 17 + context.hashCode();
      } finally {
        hashingTimer.stop();
      }
    }

    @Override
    public String toString() {
      return "AbstractStateHash [hash=" + hashCode() + ", wrappedHash=" + wrappedHash + ", context="
          + context + ", predicateKey=" + stateKey + ", precisionKey=" + precisionKey + "]";
    }
  }
}
