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
package org.sosy_lab.cpachecker.cpa.invariants;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;


public enum CompoundBitVectorIntervalManagerFactory implements CompoundIntervalManagerFactory {

  ALLOW_SIGNED_WRAP_AROUND {
    @Override
    public boolean isSignedWrapAroundAllowed() {
      return true;
    }
  },

  FORBID_SIGNED_WRAP_AROUND {
    @Override
    public boolean isSignedWrapAroundAllowed() {
      return false;
    }
  };

  private final Collection<OverflowEventHandler> overflowEventHandlers =
      new CopyOnWriteArrayList<>();

  private final OverflowEventHandler compositeHandler = new OverflowEventHandler() {

    @Override
    public void signedOverflow() {
      for (OverflowEventHandler component : overflowEventHandlers) {
        component.signedOverflow();
      }
    }
  };

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(
      MachineModel pMachineModel,
      Type pType) {
    return createCompoundIntervalManager(BitVectorInfo.from(pMachineModel, pType));
  }

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return new CompoundBitVectorIntervalManager(pBitVectorInfo, isSignedWrapAroundAllowed(),
        compositeHandler);
  }

  public abstract boolean isSignedWrapAroundAllowed();

  public void addOverflowEventHandler(OverflowEventHandler pOverflowEventHandler) {
    overflowEventHandlers.add(pOverflowEventHandler);
  }

  public void removeOverflowEventHandler(OverflowEventHandler pOverflowEventHandler) {
    overflowEventHandlers.remove(pOverflowEventHandler);
  }

}
