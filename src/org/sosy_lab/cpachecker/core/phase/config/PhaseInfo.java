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
package org.sosy_lab.cpachecker.core.phase.config;

import com.google.common.base.Objects;

/**
 * Created by Xi Cheng on 4/28/16.
 */
public class PhaseInfo {

  /**
   * The name of CPAPhase class
   */
  private final String className;
  /**
   * The identifier of this phase
   */
  private final String idName;
  /**
   * The configuration text for this phase
   */
  private final String config;

  public PhaseInfo(String pID) {
    idName = pID;
    className = "";
    config = "";
  }

  public PhaseInfo(String pClass, String pID, String pConfigPath) {
    className = pClass;
    idName = pID;
    config = pConfigPath;
  }

  public String getIdentifier() {
    return idName;
  }

  public String getClassName() {
    return className;
  }

  public String getConfig() {
    return config;
  }

  @Override
  public int hashCode() {
    // if there are multiple configs for the same identifier, we discard
    // all previous ones and only keep the last one
    return Objects.hashCode(idName);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PhaseInfo)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (((PhaseInfo) o).idName.equals(this.idName)) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    sb.append(idName);
    sb.append(" :: ");
    sb.append(className);
    sb.append(" ->\n");
    sb.append(config);
    sb.append("\n]");
    return sb.toString();
  }

}
