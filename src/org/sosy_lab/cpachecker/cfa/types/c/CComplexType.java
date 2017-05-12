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
package org.sosy_lab.cpachecker.cfa.types.c;


/**
 * Interface for types representing enums, structs, and unions.
 */
public interface CComplexType extends CType {

  ComplexTypeKind getKind();

  /**
   * Returns the unqualified name, e.g. for the type "struct s", this returns "s".
   *
   * @return A name string or the empty string if the type has no name.
   */
  String getName();

  /**
   * Returns the unqualified name, e.g. for the type "struct s", this returns "struct s".
   * If the name is empty, this contains only the qualifier.
   */
  String getQualifiedName();

  /**
   * Returns the unqualified original name, e.g. for the type "struct s", this
   * returns "."
   *
   * @returnA name string or the empty string if the type has no name.
   */
  String getOrigName();

  /**
   * Returns true if the compared objects are equal regarding the common
   * rules for the equals method. The only difference is, that if a CComplexType
   * is anonymous (thus, the origName is an empty string) the name comparison
   * is left out.
   */
  boolean equalsWithOrigName(Object obj);

  public static enum ComplexTypeKind {
    ENUM,
    STRUCT,
    UNION;

    public String toASTString() {
      return name().toLowerCase();
    }
  }
}
