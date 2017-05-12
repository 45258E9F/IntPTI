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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisMultiInitials;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompositeCPA implements ConfigurableProgramAnalysisMultiInitials, StatisticsProvider,
                                     WrapperCPA, ConfigurableProgramAnalysisWithBAM, ProofChecker {

  @Options(prefix = "cpa.composite")
  private static class CompositeOptions {
    @Option(secure = true, toUppercase = true, values = {"PLAIN", "AGREE", "HYBRID"},
        description = "which composite merge operator to use.\n"
            + "Both delegate to the component cpas, but agree only allows "
            + "merging if all cpas agree on this. This is probably what you want.")
    private String merge = "HYBRID";

    @Option(secure = true,
        description =
            "inform Composite CPA if it is run in a CPA enabled analysis because then it must "
                + "behave differently during merge.")
    private boolean inCPAEnabledAnalysis = false;
  }

  private static class CompositeCPAFactory extends AbstractCPAFactory {

    private CFA cfa = null;
    private ImmutableList<ConfigurableProgramAnalysis> cpas = null;

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      Preconditions.checkState(cpas != null, "CompositeCPA needs wrapped CPAs!");
      Preconditions.checkState(cfa != null, "CompositeCPA needs CFA information!");

      CompositeOptions options = new CompositeOptions();
      getConfiguration().inject(options);

      ImmutableList.Builder<AbstractDomain> domains = ImmutableList.builder();
      ImmutableList.Builder<TransferRelation> transferRelations = ImmutableList.builder();
      ImmutableList.Builder<MergeOperator> mergeOperators = ImmutableList.builder();
      ImmutableList.Builder<StopOperator> stopOperators = ImmutableList.builder();
      ImmutableList.Builder<PrecisionAdjustment> precisionAdjustments = ImmutableList.builder();
      ImmutableList.Builder<SimplePrecisionAdjustment> simplePrecisionAdjustments =
          ImmutableList.builder();

      boolean mergeSep = true;
      boolean simplePrec = true;

      PredicateAbstractionManager abmgr = null;

      for (ConfigurableProgramAnalysis sp : cpas) {
        if (sp instanceof org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA) {
          abmgr = ((org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA) sp).getPredicateManager();
        }

        domains.add(sp.getAbstractDomain());
        transferRelations.add(sp.getTransferRelation());
        stopOperators.add(sp.getStopOperator());

        PrecisionAdjustment prec = sp.getPrecisionAdjustment();
        if (prec instanceof SimplePrecisionAdjustment) {
          simplePrecisionAdjustments.add((SimplePrecisionAdjustment) prec);
        } else {
          simplePrec = false;
        }
        precisionAdjustments.add(prec);

        MergeOperator merge = sp.getMergeOperator();
        if (merge != MergeSepOperator.getInstance()) {
          mergeSep = false;
        }
        mergeOperators.add(merge);
      }

      ImmutableList<StopOperator> stopOps = stopOperators.build();

      MergeOperator compositeMerge;
      if (mergeSep) {
        compositeMerge = MergeSepOperator.getInstance();
      } else {
        if (options.inCPAEnabledAnalysis) {
          if (options.merge.equals("AGREE")) {
            compositeMerge =
                new CompositeMergeAgreeCPAEnabledAnalysisOperator(mergeOperators.build(), stopOps,
                    abmgr);
          } else {
            throw new InvalidConfigurationException(
                "Merge PLAIN is currently not supported in predicated analysis");
          }
        } else {
          if (options.merge.equals("HYBRID")) {
            compositeMerge = new CompositeHybridMergeOperator(getConfiguration(), mergeOperators
                .build(), stopOps);
          } else if (options.merge.equals("AGREE")) {
            compositeMerge = new CompositeMergeAgreeOperator(mergeOperators.build(), stopOps);
          } else if (options.merge.equals("PLAIN")) {
            compositeMerge = new CompositeMergePlainOperator(mergeOperators.build());
          } else {
            throw new AssertionError();
          }
        }
      }

      CompositeDomain compositeDomain = new CompositeDomain(domains.build());
      CompositeTransferRelation compositeTransfer =
          new CompositeTransferRelation(transferRelations.build(), getConfiguration(), cfa);
      CompositeStopOperator compositeStop = new CompositeStopOperator(stopOps);

      PrecisionAdjustment compositePrecisionAdjustment;
      if (simplePrec) {
        compositePrecisionAdjustment =
            new CompositeSimplePrecisionAdjustment(simplePrecisionAdjustments.build());
      } else {
        compositePrecisionAdjustment =
            new CompositePrecisionAdjustment(precisionAdjustments.build(), getLogger());
      }

      return new CompositeCPA(
          compositeDomain, compositeTransfer, compositeMerge, compositeStop,
          compositePrecisionAdjustment, cpas);
    }

    @Override
    public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
        throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Use CompositeCPA to wrap several CPAs!");
    }

    @Override
    public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren) {
      Preconditions.checkNotNull(pChildren);
      Preconditions.checkArgument(!pChildren.isEmpty());
      Preconditions.checkState(cpas == null);

      cpas = ImmutableList.copyOf(pChildren);
      return this;
    }

    @Override
    public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
      if (pClass.equals(CFA.class)) {
        cfa = (CFA) pObject;
      }
      return super.set(pObject, pClass);
    }
  }

  public static CPAFactory factory() {
    return new CompositeCPAFactory();
  }

  private final AbstractDomain abstractDomain;
  private final CompositeTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final CompositeStopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;

  private final ImmutableList<ConfigurableProgramAnalysis> cpas;

  protected CompositeCPA(
      AbstractDomain abstractDomain,
      CompositeTransferRelation transferRelation,
      MergeOperator mergeOperator,
      CompositeStopOperator stopOperator,
      PrecisionAdjustment precisionAdjustment,
      ImmutableList<ConfigurableProgramAnalysis> cpas) {
    this.abstractDomain = abstractDomain;
    this.transferRelation = transferRelation;
    this.mergeOperator = mergeOperator;
    this.stopOperator = stopOperator;
    this.precisionAdjustment = precisionAdjustment;
    this.cpas = cpas;

    List<Reducer> wrappedReducers = new ArrayList<>();
    for (ConfigurableProgramAnalysis cpa : cpas) {
      if (cpa instanceof ConfigurableProgramAnalysisWithBAM) {
        wrappedReducers.add(((ConfigurableProgramAnalysisWithBAM) cpa).getReducer());
      } else {
        wrappedReducers.clear();
        break;
      }
    }
    if (!wrappedReducers.isEmpty()) {
      reducer = new CompositeReducer(wrappedReducers);
    } else {
      reducer = null;
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    Preconditions.checkNotNull(pNode);

    ImmutableList.Builder<AbstractState> initialStates = ImmutableList.builder();
    for (ConfigurableProgramAnalysis sp : cpas) {
      initialStates.add(sp.getInitialState(pNode, pPartition));
    }

    return new CompositeState(initialStates.build());
  }

  @Override
  public Collection<AbstractState> getInitialStates(
      CFANode node, StateSpacePartition partition) {
    Preconditions.checkNotNull(node);
    List<Collection<? extends AbstractState>> components = new ArrayList<>(cpas.size());
    int size = 1;
    for (ConfigurableProgramAnalysis sp : cpas) {
      if (sp instanceof ConfigurableProgramAnalysisMultiInitials) {
        Collection<AbstractState> initials = ((ConfigurableProgramAnalysisMultiInitials) sp).
            getInitialStates(node, partition);
        components.add(initials);
        size *= initials.size();
      } else {
        Set<AbstractState> initialSet = new HashSet<>();
        initialSet.add(sp.getInitialState(node, partition));
        components.add(initialSet);
      }
    }
    // The number of initial states should be non-zero
    assert (size > 0);
    Collection<List<AbstractState>> products = CompositeTransferRelation.createCartesianProduct
        (components, size);
    ImmutableSet.Builder<AbstractState> builder = ImmutableSet.builder();
    for (List<AbstractState> product : products) {
      CompositeState state = new CompositeState(product);
      builder.add(state);
    }
    return builder.build();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition partition) {
    Preconditions.checkNotNull(pNode);

    ImmutableList.Builder<Precision> initialPrecisions = ImmutableList.builder();
    for (ConfigurableProgramAnalysis sp : cpas) {
      initialPrecisions.add(sp.getInitialPrecision(pNode, partition));
    }
    return new CompositePrecision(initialPrecisions.build());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    for (ConfigurableProgramAnalysis cpa : cpas) {
      if (cpa instanceof StatisticsProvider) {
        ((StatisticsProvider) cpa).collectStatistics(pStatsCollection);
      }
    }

    if (precisionAdjustment instanceof StatisticsProvider) {
      ((StatisticsProvider) precisionAdjustment).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (ConfigurableProgramAnalysis cpa : cpas) {
      if (pType.isAssignableFrom(cpa.getClass())) {
        return pType.cast(cpa);
      } else if (cpa instanceof WrapperCPA) {
        T result = ((WrapperCPA) cpa).retrieveWrappedCpa(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return cpas;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement,
      List<AbstractState> otherStates,
      CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    return transferRelation.areAbstractSuccessors(pElement, otherStates, pCfaEdge, pSuccessors,
        cpas);
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException, InterruptedException {
    return stopOperator.isCoveredBy(pElement, pOtherElement, cpas);
  }
}
