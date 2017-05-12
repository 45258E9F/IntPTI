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
package org.sosy_lab.cpachecker.core.interfaces.pcc;


public interface PartitioningCheckingHelper {

  /**
   * Does the necessary actions to abort the complete certificate checking process.
   * Informs all certificate checking components that certificate check failed.
   * Possibly does more actions like stops checking of other partitions,
   * prohibits start of a partition check.
   */
  public void abortCheckingPreparation();

  /**
   * Returns intermediate size of the certificate. The returned size
   * contains all elements which belong to an already checked partition
   * as well as those elements recomputed in an already checked partition.
   * The size may or may not include elements already explored in a
   * partition check which is not completed.
   *
   * @return current size of certificate
   */
  public int getCurrentCertificateSize();

}
