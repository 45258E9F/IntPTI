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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;

/**
 * This class represents variable declarations.
 * Example code:
 *
 * int x = 0;
 * struct s { ... } st;
 */
public final class CVariableDeclaration extends AVariableDeclaration implements CDeclaration {

  private final CStorageClass cStorageClass;

  public CVariableDeclaration(
      FileLocation pFileLocation, boolean pIsGlobal,
      CStorageClass pCStorageClass, CType pType, String pName, String pOrigName,
      String pQualifiedName,
      CInitializer pInitializer) {

    super(pFileLocation, pIsGlobal, pType, checkNotNull(pName), pOrigName, pQualifiedName,
        pInitializer);
    cStorageClass = pCStorageClass;

    checkArgument(!(cStorageClass == CStorageClass.EXTERN && getInitializer() != null),
        "Extern declarations cannot have an initializer");
    checkArgument(cStorageClass == CStorageClass.EXTERN || cStorageClass == CStorageClass.AUTO,
        "CStorageClass is %s", cStorageClass);
    checkArgument(pIsGlobal || cStorageClass == CStorageClass.AUTO);
    // TODO enable if we do not have unnecessary temporary variables of type void anymore
//    checkArgument(!(pType.getCanonicalType() instanceof CVoidType),
//        "Cannot declare variable of type void: " + this);
  }

  public static final String DUMMY_NAME = "!LITERAL!";

  public static CVariableDeclaration DUMMY(CType type) {
    return new CVariableDeclaration(FileLocation.DUMMY, false, CStorageClass.AUTO,
        type, DUMMY_NAME, DUMMY_NAME, DUMMY_NAME, null);
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  /**
   * The storage class of this variable (either extern or auto).
   */
  public CStorageClass getCStorageClass() {
    return cStorageClass;
  }

  @Override
  public CInitializer getInitializer() {
    return (CInitializer) super.getInitializer();
  }

  /**
   * Add an initializer.
   * This is only possible if there is no initializer already.
   * DO NOT CALL this method after CFA construction!
   *
   * @param pCInitializer the new initializer
   */
  public void addInitializer(CInitializer pCInitializer) {
    super.addInitializer(pCInitializer);
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(cStorageClass.toASTString());
    lASTString.append(getType().toASTString(getName()));

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString());
    }

    lASTString.append(";");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(cStorageClass);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CVariableDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    CVariableDeclaration other = (CVariableDeclaration) obj;

    return Objects.equals(other.cStorageClass, cStorageClass);
  }

  public int hashCodeWithOutStorageClass() {
    final int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  public boolean equalsWithoutStorageClass(Object obj) {
    return super.equals(obj);
  }

  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

}