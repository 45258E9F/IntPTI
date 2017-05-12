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
package org.sosy_lab.cpachecker.cmdline;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.ShutdownManager;


/**
 * This class is a thread which should be registered as a VM shutdown hook.
 * It will try to stop the analysis when the user presses Ctrl+C.
 */
public class ShutdownHook extends Thread {

  private final Thread mainThread;
  private final ShutdownManager shutdownManager;

  private volatile boolean enabled = true;

  /**
   * Create a shutdown hook. This constructor needs to be called from the
   * thread in which CPAchecker is run.
   */
  public ShutdownHook(ShutdownManager pShutdownManager) {
    super("Shutdown Hook");
    shutdownManager = checkNotNull(pShutdownManager);

    mainThread = Thread.currentThread();
  }

  public void disableAndStop() {
    enabled = false;
    this.interrupt(); // in case it is already running
  }

  public void disable() {
    enabled = false;
  }

  // We want to use Thread.stop() to force the main thread to stop
  // when interrupted by the user.
  @Override
  public void run() {

    if (enabled && mainThread.isAlive()) {
      // probably the user pressed Ctrl+C
      shutdownManager.requestShutdown(
          "The JVM is shutting down, probably because Ctrl+C was pressed.");

      // Keep this thread alive to that the main thread has the chance to
      // print the statistics.
      // (This thread should be the only thing that prevents the JVM
      // from immediate termination.)
      try {
        mainThread.join();
      } catch (InterruptedException e) {
      }
    }
  }
}