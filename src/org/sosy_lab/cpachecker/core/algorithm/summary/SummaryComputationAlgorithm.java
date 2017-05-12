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
package org.sosy_lab.cpachecker.core.algorithm.summary;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider.SummaryName;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryStore;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Compute summary
 */
@Options(prefix = "summary.algorithm")
public class SummaryComputationAlgorithm implements Algorithm {

  protected LogManager logger;
  protected SummaryComputer computer;

  /**
   * A wait list that satisfies:
   * 1. first push first pop
   * 2. when an element is touched, it is moved to the back
   */
  public static class WaitList<T> implements Comparator<T> {
    private int maxDelay;
    private Map<T, Integer> rank;
    private PriorityQueue<T> elements;

    WaitList() {
      this.maxDelay = 0;
      this.rank = Maps.newHashMap();
      this.elements = new PriorityQueue<>(1, this);
    }

    public void push(T e) {
      rank.put(e, ++maxDelay);
      elements.add(e);
    }

    public T pop() {
      return elements.poll();
    }

    void touch(T e) {
      elements.remove(e);
      rank.put(e, ++maxDelay);
      elements.add(e);
    }

    public boolean isEmpty() {
      return elements.isEmpty();
    }

    @Override
    public int compare(T a, T b) {
      return rank.get(a) - rank.get(b);
    }

    void pushAll(List<T> es) {
      for (T e : es) {
        push(e);
      }
    }
  }

  public SummaryComputationAlgorithm(
      Configuration config,
      SummaryComputer computer,
      LogManager logger) throws InvalidConfigurationException {
    this.computer = computer;
    this.logger = logger;
    config.inject(this);
  }

  public AlgorithmStatus run0() throws CPAException {
    // initialization
    WaitList<SummarySubject> waitList = new WaitList<>();
    computer.preAction();
    // 1. decide computation order
    waitList.pushAll(dumpAndDecideOrder(computer));
    // 2. compute until saturation
    while (!waitList.isEmpty()) {
      SummarySubject subject = waitList.pop();
      Set<SummarySubject> notify;
      try {
        notify = computer.computeFor(subject);
        for (SummarySubject x : notify) {
          waitList.touch(x);
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new CPAException(e.getMessage());
      }
    }
    // 3. post-process
    List<Triple<SummaryType, SummaryName, ? extends SummaryStore>> summaries = computer.build();
    // 4. store summary
    for (Triple<SummaryType, SummaryName, ? extends SummaryStore> pair : summaries) {
      SummaryProvider.registerSummary(pair.getFirst(), pair.getSecond(), pair.getThird());
    }
    computer.postAction();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  /**
   * Does not use the input reached set
   */
  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    return run0();
  }

  private List<SummarySubject> dumpAndDecideOrder(SummaryComputer computer) {
    // use a simplified algorithm: always pick the current element with minimal out degree
    // maintain a min-heap of summary computer
    final Map<SummarySubject, Integer> outDegree = Maps.newHashMap();
    // 1. init
    for (SummarySubject subject : computer.getSubjects()) {
      outDegree.put(subject, computer.getDependee(subject).size());
    }
    PriorityQueue<SummarySubject> remains = new PriorityQueue<>(
        Math.max(1, outDegree.size()),
        new Comparator<SummarySubject>() {
          @Override
          public int compare(SummarySubject a, SummarySubject b) {
            return outDegree.get(a) - outDegree.get(b);
          }
        });
    remains.addAll(outDegree.keySet());
    // 2. pick
    List<SummarySubject> result = Lists.newArrayList();
    Set<SummarySubject> added = Sets.newHashSet();
    while (!remains.isEmpty()) {
      SummarySubject s = remains.poll();
      result.add(s);
      added.add(s);
      for (SummarySubject t : computer.getDepender(s)) {
        // reduce out-degree of t by 1
        remains.remove(t);
        outDegree.put(t, outDegree.get(t) - 1);
        if (!added.contains(t)) {
          remains.add(t);
        }
      }
    }
    return result;
  }

}
