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
package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Interface that describes a partition of the state space.
 *
 * Two partitions are equivalent if their partition key is equivalent (see interface {@link
 * Partitionable}).
 *
 * (A class is more flexible than an enum: own instances of StateSpacePartition can be created in
 * the project-specific code.)
 */
public class StateSpacePartition implements Partitionable {

  private static final StateSpacePartition defaultPartition =
      getPartitionWithKey(Integer.valueOf(0));

  public static StateSpacePartition getDefaultPartition() {
    return defaultPartition;
  }

  public static synchronized StateSpacePartition getPartitionWithKey(final Object pPartitionKey) {
    return new StateSpacePartition(pPartitionKey);
  }

  private final
  @Nonnull
  Object partitionKey;

  private StateSpacePartition(Object pPartitionKey) {
    Preconditions.checkNotNull(pPartitionKey);
    this.partitionKey = pPartitionKey;
  }

  @Override
  public Object getPartitionKey() {
    return partitionKey;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof StateSpacePartition)) {
      return false;
    }
    return ((StateSpacePartition) pObj).getPartitionKey().equals(partitionKey);
  }

  @Override
  public String toString() {
    return partitionKey.toString();
  }

  @Override
  public int hashCode() {
    return partitionKey.hashCode();
  }

}
