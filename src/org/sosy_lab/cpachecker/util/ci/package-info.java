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

/**
 * Package for management of custom instructions.
 *
 * It is assumed that these custom instructions will execute parts of the program's
 * statements as special purpose instructions i.e. implemented on special HW like FPGAs.
 *
 * Used to support the extraction of requirements for the custom instructions from
 * the software analysis result as explained in approach #3 of paper
 *
 * M.-C. Jakobs, M. Platzner, T. Wiersema, H. Wehrheim:
 * Integrating Softwaren and Hardware Verification
 * Integrated Formal Methods, LNCS, Springer, 2014
 */
package org.sosy_lab.cpachecker.util.ci;