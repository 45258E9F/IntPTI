/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.appengine.common;

import com.google.appengine.api.ThreadManager;

import java.util.concurrent.ThreadFactory;

/**
 * Implementation of {@link ThreadFactory}
 * that returns threads bound to the current request
 * using {@link ThreadManager#currentRequestThreadFactory()}.
 * The "current request" is the determined at thread creation time,
 * not before.
 */
public class CurrentRequestThreadFactory implements ThreadFactory {

  @Override
  public Thread newThread(Runnable pR) {
    return ThreadManager.currentRequestThreadFactory().newThread(pR);
  }
}
