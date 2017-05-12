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
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.LinkedList;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Waitlist implementation that considers states in a random order for pop().
 */
@SuppressFBWarnings(value = "BC_BAD_CAST_TO_CONCRETE_COLLECTION",
    justification = "warnings is only because of casts introduced by generics")
public class RandomWaitlist extends AbstractWaitlist<LinkedList<AbstractState>> {

  private final Random rand = new Random();

  protected RandomWaitlist() {
    super(new LinkedList<AbstractState>());
  }

  @Override
  public AbstractState pop() {
    int r = rand.nextInt(waitlist.size());
    return waitlist.remove(r);
  }
}
