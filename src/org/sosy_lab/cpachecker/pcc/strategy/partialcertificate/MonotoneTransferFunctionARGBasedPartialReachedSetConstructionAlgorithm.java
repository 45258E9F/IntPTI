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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.ArrayList;
import java.util.List;

public class MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm
    implements PartialReachedConstructionAlgorithm {

  private final boolean returnARGStates;
  private final boolean withCMC;

  public MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm(
      final boolean pReturnARGStatesInsteadOfWrappedStates, final boolean pWithCMC) {
    returnARGStates = pReturnARGStatesInsteadOfWrappedStates;
    withCMC = pWithCMC;
  }

  @Override
  public AbstractState[] computePartialReachedSet(final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    if (!(pReached.getFirstState() instanceof ARGState)) {
      throw new InvalidConfigurationException(
          "May only compute partial reached set with this algorithm if an ARG is constructed and ARG is top level state.");
    }
    ARGState root = (ARGState) pReached.getFirstState();

    NodeSelectionARGPass argPass = getARGPass(pReached.getPrecision(root), root);
    argPass.passARG(root);

    List<? extends AbstractState> reachedSetSubset = argPass.getSelectedNodes();
    return reachedSetSubset.toArray(new AbstractState[reachedSetSubset.size()]);
  }

  /**
   * @param pRootPrecision the root precision
   * @param pRoot          the root state
   * @throws InvalidConfigurationException may be thrown in subclasses
   */
  protected NodeSelectionARGPass getARGPass(final Precision pRootPrecision, final ARGState pRoot)
      throws InvalidConfigurationException {
    return new NodeSelectionARGPass(pRoot);
  }


  protected class NodeSelectionARGPass extends AbstractARGPass {

    private final ARGState root;

    public NodeSelectionARGPass(final ARGState pRoot) {
      super(false);
      root = pRoot;
    }

    private List<AbstractState> wrappedARGStates = new ArrayList<>();
    private List<ARGState> argStates = new ArrayList<>();

    @Override
    public void visitARGNode(final ARGState pNode) {
      if (isToAdd(pNode)) {
        if (returnARGStates) {
          argStates.add(pNode);
        } else {
          wrappedARGStates.add(pNode.getWrappedState());
        }
      }
    }

    protected boolean isToAdd(final ARGState pNode) {
      return pNode == root || pNode.getParents().size() > 1
          || pNode.getCoveredByThis().size() > 0 && !pNode.isCovered()
          || withCMC && (pNode.getChildren().size() > 1
          || !pNode.isCovered() && (pNode.getChildren().size() == 0
          || pNode.getParents().iterator().next().getChildren().size() > 1));
    }

    @Override
    public boolean stopPathDiscovery(final ARGState pNode) {
      return false;
    }

    public List<? extends AbstractState> getSelectedNodes() {
      if (returnARGStates) {
        return argStates;
      } else {
        return wrappedARGStates;
      }
    }

  }


}
