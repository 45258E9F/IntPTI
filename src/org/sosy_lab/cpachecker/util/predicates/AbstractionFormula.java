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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Instances of this class should hold a state formula (the result of an
 * abstraction computation) in several representations:
 * First, as an abstract region (usually this would be a BDD).
 * Second, as a symbolic formula.
 * Third, again as a symbolic formula, but this time all variables have names
 * which include their SSA index at the time of the abstraction computation.
 *
 * Additionally the formula for the block immediately before the abstraction
 * computation is stored (this also has SSA indices as it is a path formula,
 * even if it is not of the type PathFormula).
 *
 * Abstractions are not considered equal even if they have the same formula.
 */
public class AbstractionFormula implements Serializable {

  private static final long serialVersionUID = -7756517128231447937L;
  private
  @Nullable
  transient final Region region; // Null after de-serializing from proof
  private transient final BooleanFormula formula;
  private final BooleanFormula instantiatedFormula;

  /**
   * The formula of the block directly before this abstraction.
   * (This formula was used to create this abstraction).
   */
  private final PathFormula blockFormula;

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final transient int id = idGenerator.getFreshId();
  private final transient BooleanFormulaManager mgr;
  private final transient ImmutableSet<Integer> idsOfStoredAbstractionReused;

  public AbstractionFormula(
      FormulaManagerView mgr,
      Region pRegion, BooleanFormula pFormula,
      BooleanFormula pInstantiatedFormula, PathFormula pBlockFormula,
      Set<Integer> pIdOfStoredAbstractionReused) {
    this.mgr = checkNotNull(mgr.getBooleanFormulaManager());
    this.region = checkNotNull(pRegion);
    this.formula = checkNotNull(pFormula);
    this.instantiatedFormula = checkNotNull(pInstantiatedFormula);
    this.blockFormula = checkNotNull(pBlockFormula);
    this.idsOfStoredAbstractionReused = ImmutableSet.copyOf(pIdOfStoredAbstractionReused);
  }

  public boolean isReusedFromStoredAbstraction() {
    return !idsOfStoredAbstractionReused.isEmpty();
  }

  public boolean isTrue() {
    return mgr.isTrue(formula);
  }

  public boolean isFalse() {
    return mgr.isFalse(formula);
  }

  public
  @Nullable
  Region asRegion() {
    return region;
  }

  /**
   * Returns the formula representation where all variables do not have SSA indices.
   */
  public BooleanFormula asFormula() {
    return formula;
  }

  /**
   * Returns the formula representation where all variables DO have SSA indices.
   */
  public BooleanFormula asInstantiatedFormula() {
    return instantiatedFormula;
  }

  public PathFormula getBlockFormula() {
    return blockFormula;
  }

  public int getId() {
    return id;
  }

  public ImmutableSet<Integer> getIdsOfStoredAbstractionReused() {
    return idsOfStoredAbstractionReused;
  }

  @Override
  public String toString() {
    // we print the formula only when it is small
    String abs = "";
    if (isTrue()) {
      abs = ": true";
    } else if (isFalse()) {
      abs = ": false";
    }
    return "ABS" + id + abs;
  }

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   *
   * @param in an input stream
   */
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 2349286L;
    private final String instantiatedFormulaDump;
    private final PathFormula blockFormula;

    public SerializationProxy(AbstractionFormula pAbstractionFormula) {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      instantiatedFormulaDump = mgr.dumpFormula(
          pAbstractionFormula.asInstantiatedFormula()).toString();
      blockFormula = pAbstractionFormula.getBlockFormula();
    }

    private Object readResolve() {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      BooleanFormula instantiatedFormula = mgr.parse(instantiatedFormulaDump);
      BooleanFormula notInstantiated = mgr.uninstantiate(instantiatedFormula);
      return new AbstractionFormula(
          mgr,
          GlobalInfo.getInstance().getAbstractionManager()
              .convertFormulaToRegion(notInstantiated), notInstantiated,
          instantiatedFormula, blockFormula, ImmutableSet.<Integer>of());
    }
  }
}
