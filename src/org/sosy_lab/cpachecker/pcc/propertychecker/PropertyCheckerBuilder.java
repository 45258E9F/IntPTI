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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class PropertyCheckerBuilder {

  private static final String PROPERTYCHECKER_CLASS_PREFIX =
      "org.sosy_lab.cpachecker.pcc.propertychecker";

  public static PropertyChecker buildPropertyChecker(
      String propCheckerClassName,
      String pCheckerParamList)
      throws InvalidConfigurationException {
    if (propCheckerClassName == null) {
      throw new InvalidConfigurationException(
          "No property checker defined.");
    }

    Class<?> propertyCheckerClass;
    try {
      propertyCheckerClass = Classes.forName(propCheckerClassName, PROPERTYCHECKER_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Class for property checker  " + propCheckerClassName + " is unknown.", e);
    }

    if (!PropertyChecker.class.isAssignableFrom(propertyCheckerClass)) {
      throw new InvalidConfigurationException(
          "Option propertychecker.className must be set to a class implementing the PropertyChecker interface!");
    }

    // get list of parameters
    String[] param;

    if (pCheckerParamList.equals("")) {
      param = new String[0];
    } else {
      String[] result = pCheckerParamList.split(",", -1);
      param = new String[result.length - 1];
      for (int i = 0; i < param.length; i++) {
        param[i] = result[i];
      }
    }
    // construct property checker instance
    try {
      Constructor<?>[] cons = propertyCheckerClass.getConstructors();

      Class<?>[] paramTypes;
      Constructor<?> constructor = null;
      for (Constructor<?> con : cons) {
        paramTypes = con.getParameterTypes();
        if (paramTypes.length != param.length) {
          continue;
        } else {
          for (Class<?> paramType : paramTypes) {
            if (paramType != String.class) {
              continue;
            }
          }
        }
        constructor = con;
        break;
      }

      if (constructor == null) {
        throw new UnsupportedOperationException(
            "Cannot create PropertyChecker " + propCheckerClassName
                + " if it does not provide a constructor with "
                + param.length + " String parameters.");
      }

      return (PropertyChecker) constructor.newInstance((Object[]) param);
    } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }


}
