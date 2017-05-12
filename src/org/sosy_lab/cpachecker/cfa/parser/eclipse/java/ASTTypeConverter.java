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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import com.google.common.base.Preconditions;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;


class ASTTypeConverter extends TypeConverter {

  private final Scope scope;

  public ASTTypeConverter(Scope pScope) {
    scope = pScope;
  }

  /**
   * Searches for a type within the Type Hierarchy.
   * If found, returns it.
   *
   * @param t binding representing the sought after type.
   * @return Returns a type within the TypeHierachie or a Unspecified Type.
   */
  @Override
  public JInterfaceType convertInterfaceType(ITypeBinding t) {

    if (t.isClass()) {
      return JInterfaceType.createUnresolvableType();
    }

    Preconditions.checkArgument(t.isInterface());

    String typeName = NameConverter.convertClassOrInterfaceToFullName(t);

    if (scope.containsInterfaceType(typeName)) {
      return scope.getInterfaceType(typeName);
    } else {
      return scope.createNewInterfaceType(t);
    }
  }

  /**
   * Converts a Class Type by its Binding.
   * This Method searches in the parsed Type Hierarchy for
   * the type, which is represented by the  given binding.
   *
   * @param t type Binding which represents the sought after type
   * @return The Class Type which is represented by t.
   */
  @Override
  public JClassType convertClassType(ITypeBinding t) {

    Preconditions.checkArgument(t.isClass() || t.isEnum());

    String typeName = NameConverter.convertClassOrInterfaceToFullName(t);

    if (scope.containsClassType(typeName)) {
      return scope.getClassType(typeName);
    } else {
      return scope.createNewClassType(t);
    }
  }
}