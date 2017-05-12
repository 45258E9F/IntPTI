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
package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.location.LocationState.LocationStateFactory.LocationStateType;

class LocationCPAFactory extends AbstractCPAFactory {

  private final LocationStateType locationType;

  private CFA cfa;

  public LocationCPAFactory(LocationStateType pLocationType) {
    locationType = pLocationType;
  }

  @Override
  public <T> LocationCPAFactory set(T pObject, Class<T> pClass) {
    if (CFA.class.isAssignableFrom(pClass)) {
      cfa = (CFA) pObject;
    } else {
      super.set(pObject, pClass);
    }
    return this;
  }

  @Override
  public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
    checkNotNull(cfa, "CFA instance needed to create LocationCPA");

    switch (locationType) {
      case BACKWARD:
        return new LocationCPABackwards(cfa, getConfiguration());
      case BACKWARD_NO_TARGET:
        return new LocationCPABackwardsNoTargets(cfa, getConfiguration());
      case SINGLE_FUNCTION:
        return new LocationCPASingleFunction(cfa, getConfiguration());
      default:
        return new LocationCPA(cfa, getConfiguration());
    }
  }
}