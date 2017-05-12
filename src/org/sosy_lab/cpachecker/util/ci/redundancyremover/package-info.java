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

/**
 * Package to remove some extracted requirements which are covered by others.
 * Assume that for coverage it is sufficient to look at the part of the requirement
 * which is associated with the variables of the applied custom instructions signature
 */
package org.sosy_lab.cpachecker.util.ci.redundancyremover;