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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.types.c.CType;


public class Variable {
  private final String name;
  private final CType type;

  private Variable(String pName, CType pType) {
    super();
    name = pName;
    type = pType;
  }

  public String getName() {
    return name;
  }

  public CType getType() {
    assert type != null;
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Variable other = (Variable) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return type.toASTString(name);
  }

  public Variable withName(String newName) {
    return Variable.create(newName, type);
  }

  public Variable withType(CType pType) {
    return Variable.create(name, pType);
  }

  public static Variable create(String pName, CType pT) {
    return new Variable(pName, pT);
  }
}
