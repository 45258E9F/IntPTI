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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import java.util.Objects;

public final class PathFormulaWithStartSSA {
  private final PathFormula pathFormula;
  private final SSAMap startMap;

  public PathFormulaWithStartSSA(PathFormula pPathFormula, SSAMap pStartMap) {
    pathFormula = pPathFormula;
    startMap = pStartMap;
  }

  public SSAMap getStartMap() {
    return startMap;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    PathFormulaWithStartSSA that = (PathFormulaWithStartSSA) pO;
    return Objects.equals(pathFormula, that.pathFormula) &&
        Objects.equals(startMap, that.startMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pathFormula, startMap);
  }
}
