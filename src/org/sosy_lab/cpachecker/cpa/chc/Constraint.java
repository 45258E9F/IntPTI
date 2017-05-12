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
package org.sosy_lab.cpachecker.cpa.chc;

import java.util.ArrayList;
import java.util.HashMap;

import jpl.Term;

public class Constraint {

  private ArrayList<Term> cns = null;
  private HashMap<String, Term> vars = null;

  public Constraint() {
    cns = new ArrayList<>();
    vars = new HashMap<>();
  }

  public Constraint(Constraint cn) {
    this.cns = new ArrayList<>(cn.getConstraint());
    this.vars = new HashMap<>(cn.getVars());
  }

  public Constraint(ArrayList<Term> cns, HashMap<String, Term> vars) {
    this.cns = cns;
    this.vars = vars;
  }

  public Constraint(ArrayList<Term> cns) {
    this.cns = cns;
    vars = new HashMap<>();
  }

  public Constraint(ArrayList<Term> cns, ArrayList<Term> vars) {
    this.cns = cns;
    createHashMap(vars);
  }

  public Constraint(Term cn, ArrayList<Term> vars) {
    cns = new ArrayList<>();
    cns.add(cn);
    createHashMap(vars);
  }

  public void addAtomicConstraint(Term t) {
    cns.add(t);
  }

  public void addVar(String var, Term t) {
    vars.put(var, t);
  }

  public void addVars(HashMap<String, Term> vars) {
    this.vars.putAll(vars);
  }

  public void removeVar(String var) {
    vars.remove(var);
  }

  @Override
  public String toString() {
    if (cns == null) {
      return "false";
    } else if (cns.isEmpty()) {
      return "true";
    } else {
      return cns.toString() + " (vars: " + vars.toString() + ")";
    }
  }

  public ArrayList<Term> getConstraint() {
    return cns;
  }

  public HashMap<String, Term> getVars() {
    return vars;
  }

  public boolean isTrue() {
    return cns.isEmpty();
  }

  public boolean isFalse() {

    if (cns == null) {
      return true;
    } else {
      if (cns.size() != 0) {
        if (cns.get(0).toString().equals("false")) {
          return true;
        }
      }
      return false;
    }
  }

  public static boolean isFalse(String cr) {
    if (cr.toString().equals("'.'(false, [])")) {
      return true;
    } else {
      return false;
    }
  }

  public void setTrue() {
    cns = new ArrayList<>();
  }

  public Constraint setFalse() {
    cns = null;
    return this;
  }

  public void and(Constraint cn) {
    cns.addAll(cn.getConstraint());
  }

  public Constraint setConstraint(ArrayList<Term> cns) {
    this.cns = cns;
    return this;
  }

  public void emptyVar() {
    vars = new HashMap<>();
  }

  private void createHashMap(ArrayList<Term> varTerms) {
    vars = new HashMap<>();
    for (Term v : varTerms) {
      vars.put(ConstraintManager.var2CVar(v.name()), v);
    }
  }
}