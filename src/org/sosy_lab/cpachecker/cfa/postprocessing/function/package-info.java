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
 * Post-processings for the CFA that change the CFA structure,
 * executed (optionally) between parsing and returning the finished CFA.
 * The post-processings in this package all work only inside functions.
 *
 * Be careful when you want to add something here.
 * If possible, do not change the CFA,
 * but write you analysis such that it handles the unprocessed CFA.
 * If your analysis depends on a specifically post-processed CFA,
 * it may not be possible to combine it with other CPAs.
 */
package org.sosy_lab.cpachecker.cfa.postprocessing.function;