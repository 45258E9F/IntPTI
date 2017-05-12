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
package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.base.Function;

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * It is part of the declaration of types and variables (see {@link ADeclaration})
 * and functions (see {@link AFunctionDeclaration}).
 * It is also used stand-alone for the declaration of members of composite types
 * (e.g. structs) and for the declaration of function parameters.
 */
public interface ASimpleDeclaration extends AAstNode {

  public String getName();

  public String getOrigName();

  Type getType();

  /**
   * Get globally unique name of this declaration, qualified with the function name.
   */
  public String getQualifiedName();

  public static final Function<ASimpleDeclaration, String> GET_QUALIFIED_NAME =
      new Function<ASimpleDeclaration, String>() {
        @Override
        public String apply(ASimpleDeclaration pInput) {
          return pInput.getQualifiedName();
        }
      };
}
