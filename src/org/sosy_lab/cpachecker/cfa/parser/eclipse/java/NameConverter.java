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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import com.google.common.base.Joiner;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


final class NameConverter {

  private static final String DELIMITER = "_";

  private NameConverter() {

  }

  /**
   * This Method uses the binding of a Method to construct the fully qualified unique
   * method name for Methods and constructor in the CFA. Use whenever possible to avoid
   * Inconsistency.
   *
   * @param binding The JDT Binding of a method to be named
   * @return the fully Qualified, unique method name
   */
  public static String convertName(IMethodBinding binding) {

    StringBuilder name = new StringBuilder((
        convertClassOrInterfaceToFullName(binding.getDeclaringClass())
            + DELIMITER + binding.getName()));

    String[] typeNames = convertTypeNames(binding.getParameterTypes());

    if (typeNames.length > 0) {
      name.append(DELIMITER);
    }

    Joiner.on(DELIMITER).appendTo(name, typeNames);

    return name.toString();
  }

  public static String[] convertTypeNames(ITypeBinding[] parameterTypes) {

    String[] typeNames = new String[parameterTypes.length];

    int c = 0;
    for (ITypeBinding parameterTypeBindings : parameterTypes) {

      // TODO Erase when Library in class Path
      if (parameterTypeBindings.getBinaryName().equals("String")
          || parameterTypeBindings.getQualifiedName().equals("java.lang.String")) {

        typeNames[c] = "java_lang_String";
      } else if (parameterTypeBindings.isArray()) {

        ITypeBinding elementType = parameterTypeBindings.getElementType();

        if (elementType.getBinaryName().equals("String")
            || elementType.getQualifiedName().equals("java.lang.String")) {
          typeNames[c] = "String[]";
        } else {
          typeNames[c] = elementType.getQualifiedName() + "[]";
        }
      } else {
        typeNames[c] = parameterTypeBindings.getQualifiedName();
      }

      c++;
    }
    return typeNames;
  }

  public static String convertName(IVariableBinding vb) {
    StringBuilder name = new StringBuilder();

    // Field Variable are declared with Declaring class before Identifier
    if (vb.isField() && vb.getDeclaringClass() != null) {

      String declaringClassName = convertClassOrInterfaceToFullName(vb.getDeclaringClass());

      name.append(declaringClassName + DELIMITER);
    }

    name.append(vb.getName());

    return name.toString();
  }

  public static String convertClassOrInterfaceToFullName(ITypeBinding classBinding) {

    if (classBinding.isAnonymous()) {

      // Anonymous types do not have a name, so we just use their key. This way, two anonymous
      // declarations with exactly the same content are only assigned once
      String key = classBinding.getKey();

      // cut the semicolon at the end of the key
      assert key.charAt(key.length() - 1) == ';';
      return key.substring(0, key.length() - 1);

    } else {
      return classBinding.getQualifiedName();
    }
  }

  public static String convertClassOrInterfaceToSimpleName(ITypeBinding classBinding) {

    if (classBinding.isAnonymous()) {

      // Anonymous types do not have a name, so we just use their key. This way, two anonymous
      // declarations with exactly the same content are only assigned once
      String key = classBinding.getKey();

      // cut the semicolon at the end of the key
      assert key.charAt(key.length() - 1) == ';';
      return key.substring(0, key.length() - 1);

    } else {
      return classBinding.getName();
    }
  }

  public static String convertDefaultConstructorName(ITypeBinding classBinding) {
    if (classBinding.isAnonymous()) {
      return convertAnonymousClassConstructorName(classBinding, Collections.<JType>emptyList());

    } else {
      return (convertClassOrInterfaceToFullName(classBinding)
          + DELIMITER
          + convertClassOrInterfaceToSimpleName(classBinding));
    }
  }

  public static String convertAnonymousClassConstructorName(
      ITypeBinding pClassBinding, List<JType> pParameters) {

    ITypeBinding declaringClassBinding = pClassBinding.getDeclaringClass();
    assert declaringClassBinding != null : "Anonymous class must be nested!";

    StringBuilder name = new StringBuilder(convertClassOrInterfaceToFullName(declaringClassBinding)
        + "."
        + convertClassOrInterfaceToFullName(pClassBinding)
        + DELIMITER
        + convertClassOrInterfaceToSimpleName(pClassBinding));

    if (!pParameters.isEmpty()) {
      name.append(DELIMITER);
    }

    List<String> parameterTypeNames = new ArrayList<>(pParameters.size());

    for (JType t : pParameters) {
      parameterTypeNames.add(t.toString());
    }

    Joiner.on(DELIMITER).appendTo(name, parameterTypeNames);

    return name.toString();
  }

  public static String createQualifiedName(String pMethodName, String pVariableName) {
    return pMethodName + "::" + pVariableName;
  }
}