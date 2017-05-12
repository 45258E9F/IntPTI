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
package org.sosy_lab.cpachecker.util.access;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.range.ArrayUncertainIndexSegment;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Types;
import org.sosy_lab.cpachecker.util.VariableClassificationBuilder;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable name or qualified variable.
 *
 * qualified variable
 * - GlobalVariable
 * - LocalVariable
 * - Parameter
 */
public class AccessPath implements Comparable<AccessPath> {
  private static final Function<PathSegment, String> PATH_SEGMENT_TO_NAME =
      new Function<PathSegment, String>() {
        @Override
        public String apply(PathSegment ps) {
          return ps.getName();
        }
      };


  // function name + identifier
  private final String qualifiedName;
  // owner of the object
  private final CSimpleDeclaration declaration;
  // access path
  private final LinkedList<PathSegment> path;

  private static Pattern MALLOC_NAME = Pattern.compile("[\\w]+_ID[\\d]+_Line:[\\d]+");

  public AccessPath(CSimpleDeclaration pDeclaration) {
    qualifiedName = pDeclaration.getQualifiedName();
    declaration = pDeclaration;
    path = new LinkedList<>();
    path.add(new DeclarationSegment(declaration));
  }

  private AccessPath(CSimpleDeclaration pDeclaration, LinkedList<PathSegment> pPath) {
    qualifiedName = pDeclaration.getQualifiedName();
    declaration = pDeclaration;
    path = new LinkedList<>(pPath);
  }

  public static AccessPath createDummyAccessPath(CType type) {
    return new AccessPath(CVariableDeclaration.DUMMY(type));
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public CType getType() {
    return declaration.getType();
  }

  public boolean isGlobal() {
    return declaration instanceof CDeclaration && ((CDeclaration) declaration).isGlobal();
  }

  public boolean isParameter() {
    return declaration instanceof CParameterDeclaration;
  }

  public boolean isReturnValue() {
    return declaration.getName().equals(VariableClassificationBuilder.FUNCTION_RETURN_VARIABLE);
  }

  public static AccessPath copyOf(AccessPath path) {
    return new AccessPath(path.declaration, path.path);
  }

  /**
   * Check if the current access path is a single declaration path.
   */
  public boolean isDeclarationPath() {
    return path.size() == 1;
  }

  /**
   * This function checks whether current access path contains CPA-specific segments. CPA-specific
   * segments include {@link org.sosy_lab.cpachecker.cpa.range.ArrayUncertainIndexSegment} and
   * others.
   *
   * @return whether current access path is canonical
   */
  public boolean isCanonicalAccessPath() {
    for (PathSegment segment : path) {
      if (!(segment instanceof ArrayAccessSegment) && !(segment instanceof ArrayConstIndexSegment)
          &&
          !(segment instanceof DeclarationSegment) && !(segment instanceof FieldAccessSegment) &&
          !(segment instanceof PointerDereferenceSegment)) {
        return false;
      }
    }
    return true;
  }

  /**
   * If an access path contains undetermined array index or pointer manipulation, such path is
   * undetermined.
   *
   * @return determinedness of the given access path
   */
  public boolean isDeterminedAccessPath() {
    for (PathSegment segment : path) {
      if (!(segment instanceof DeclarationSegment) && !(segment instanceof ArrayConstIndexSegment)
          && !(segment instanceof FieldAccessSegment)) {
        return false;
      }
    }
    return true;
  }

  public boolean supportMemoryLocationRepresentation() {
    for (PathSegment segment : path) {
      if (!(segment instanceof ArrayConstIndexSegment) &&
          !(segment instanceof DeclarationSegment) && !(segment instanceof FieldAccessSegment) &&
          !(segment instanceof PointerDereferenceSegment)) {
        return false;
      }
    }
    return true;
  }

  public boolean isActualPath() {
    boolean isMalloc = false;
    CType currentType = declaration.getType();
    Matcher matcher = MALLOC_NAME.matcher(qualifiedName);
    if (matcher.matches()) {
      isMalloc = true;
    }
    if (path.size() == 1) {
      return true;
    }
    for (int i = 1; i < path.size(); i++) {
      PathSegment seg = path.get(i);
      if (seg instanceof PointerDereferenceSegment) {
        return false;
      } else if (seg instanceof FieldAccessSegment) {
        String fieldName = seg.getName();
        currentType = Types.getFieldType(currentType, fieldName);
        if (currentType == null) {
          // if we reached here, there is something wrong with access summary
          // TODO: access summary bug
          return false;
        }
      } else if (seg instanceof ArrayConstIndexSegment ||
          seg instanceof ArrayAccessSegment) {
        CPointerType pointerType = Types.extractPointerType(currentType);
        CArrayType arrayType = Types.extractArrayType(currentType);
        if (pointerType != null) {
          if (isMalloc) {
            currentType = pointerType.getType();
            isMalloc = false;
          } else {
            return false;
          }
        } else if (arrayType != null) {
          currentType = arrayType.getType();
        }
      } else {
        return false;
      }
      if (i == 1) {
        // reset the malloc status flag
        isMalloc = false;
      }
    }
    // pass every segment
    return true;
  }

  /**
   * This function is to filter expressions that contains pointer dereference.
   * For internal use only.
   *
   * @return whether current access path contains pointer dereference operation(s)
   */
  public boolean containsPointerOperation() {
    for (PathSegment segment : path) {
      if (segment instanceof PointerDereferenceSegment ||
          segment instanceof AddressingSegment) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return declaration.hashCode() * 17 + path.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof AccessPath)) {
      return false;
    }
    AccessPath that = (AccessPath) obj;
    return this.declaration.equals(that.declaration) && Objects.equal(this.path, that.path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (PathSegment seg : path) {
      if (!first) {
        sb.append('.');
      } else {
        first = false;
      }
      sb.append(seg.getName());
    }
    return sb.toString();
  }

  @Override
  public int compareTo(AccessPath that) {
    int result = ComparisonChain.start()
        .compare(this.qualifiedName, that.qualifiedName).result();
    if (result != 0) {
      return result;
    }
    Iterator<PathSegment> psi1 = this.path.iterator();
    Iterator<PathSegment> psi2 = that.path.iterator();
    while (psi1.hasNext() && psi2.hasNext()) {
      String psn1 = psi1.next().getName();
      String psn2 = psi2.next().getName();
      int r = psn1.compareTo(psn2);
      if (r != 0) {
        return r;
      }
    }
    if (psi1.hasNext() && !psi2.hasNext()) {
      return 1;
    } else if (!psi1.hasNext() && psi2.hasNext()) {
      return -1;
    } else {
      return 0;
    }
  }

  /**
   * Return null on failure
   */
  public PathSegment getLastSegment() {
    if (path.size() == 0) {
      return null;
    } else {
      return path.getLast();
    }
  }

  public void removeLastSegment() {
    path.removeLast();
  }

  public void appendSegment(PathSegment segment) {
    path.addLast(segment);
  }

  /**
   * A string representation of the path
   * qualified name is the first element
   */
  public List<PathSegment> path() {
//    // NOTE: will this be a performance issue?
//    // does 'copyOf' actually perform copying?
//    return ImmutableList.copyOf(path);
    return Collections.unmodifiableList(path);
  }

  /**
   * Return the path segments besides the first (declaration) one.
   */
  public List<PathSegment> afterFirstPath() {
    ImmutableList.Builder<PathSegment> builder = ImmutableList.builder();
    for (int i = 1; i < path.size(); i++) {
      builder.add(path.get(i));
    }
    return builder.build();
  }

  /**
   * Restore the type information for access path segments.
   * Produced type list is either empty or full (i.e. the length equals to the number of segments).
   *
   * @return the list of types corresponding to segments of access path
   */
  public List<CType> parseTypeList() {
    List<CType> types = new ArrayList<>();
    CType currentType = declaration.getType();
    types.add(currentType);
    for (int i = 1; i < path.size(); i++) {
      PathSegment segment = path.get(i);
      if (segment instanceof ArrayConstIndexSegment || segment instanceof
          ArrayUncertainIndexSegment) {
        CArrayType arrayType = Types.extractArrayType(currentType);
        if (arrayType != null) {
          currentType = arrayType.getType();
          types.add(currentType);
          continue;
        }
        CPointerType pointerType = Types.extractPointerType(currentType);
        if (pointerType != null) {
          currentType = pointerType.getType();
          types.add(currentType);
          continue;
        }
        // if we reach here, we cannot obtain the type of array element
        return Lists.newArrayList();
      } else if (segment instanceof FieldAccessSegment) {
        String name = segment.getName();
        CCompositeType compositeType = Types.extractCompositeType(currentType);
        if (compositeType != null) {
          for (CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
            if (member.getName().equals(name)) {
              currentType = member.getType();
              types.add(currentType);
              break;
            }
          }
        } else {
          throw new AssertionError("Field access segment relies on composite type");
        }
      } else if (segment instanceof PointerDereferenceSegment) {
        currentType = Types.dereferenceType(currentType);
        types.add(currentType);
      } else {
        throw new AssertionError("Illegal access path segment " + segment);
      }
    }
    // sanity check
    if (types.size() == path.size()) {
      return types;
    } else {
      // return an empty list of sanity check fails
      throw new AssertionError("Inconsistent type list");
    }
  }

  /**
   * Parse access path from type definition
   * Do not expand pointer, e.g.:
   *
   * struct A {
   * int x;
   * struct B {
   * int y;
   * struct *p;
   * } b;
   * } a
   *
   * produces
   *
   * a.x
   * a.b.y
   * a.b.p
   *
   * Elaborated type is not expanded, either
   */
  public static PathCopyingPersistentTree<String, CType> parse(CType type) {
    if (type instanceof CArrayType) {
      CArrayType at = (CArrayType) type;
      PathCopyingPersistentTree<String, CType> tree = PathCopyingPersistentTree.of();
      return tree.setElementAndCopy(Collections.singletonList("*"), at.getType());
    } else if (type instanceof CPointerType) {
      CPointerType pt = (CPointerType) type;
      PathCopyingPersistentTree<String, CType> tree = PathCopyingPersistentTree.of();
      return tree.setElementAndCopy(Collections.singletonList("*"), pt.getType());
    } else if (type instanceof CCompositeType) {
      CCompositeType ct = (CCompositeType) type;
      PathCopyingPersistentTree<String, CType> tree = PathCopyingPersistentTree.of();
      for (CCompositeTypeMemberDeclaration f : ct.getMembers()) {
        tree = tree.setSubtreeAndCopy(Collections.singletonList(f.getName()), parse(f.getType()));
      }
      return tree;
    } else {
      PathCopyingPersistentTree<String, CType> tree = PathCopyingPersistentTree.of();
      return tree.setSubtreeAndCopy(new ArrayList<String>(), type);
    }
  }

  public static List<String> toStrList(AccessPath ap) {
    return FluentIterable.from(ap.path()).transform(PATH_SEGMENT_TO_NAME).toList();
  }

  private static final Pattern identifier = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

  public static Pair<String, List<PathSegment>> reverseParse(List<String> strList) {
    if (strList.size() == 0) {
      throw new IllegalArgumentException("Qualified name required");
    }
    String qualifiedName = strList.get(0);
    List<PathSegment> remSegments = new ArrayList<>();
    for (int i = 1; i < strList.size(); i++) {
      String segmentName = strList.get(i);
      if (segmentName.charAt(0) == '[' && segmentName.charAt(segmentName.length() - 1) == ']') {
        // array index segment
        String indexStr = segmentName.substring(1, segmentName.length() - 1);
        Long index = Long.valueOf(indexStr);
        // if the parsing fails, exception will be thrown
        remSegments.add(new ArrayConstIndexSegment(index));
      } else {
        if (!identifier.matcher(segmentName).matches()) {
          throw new IllegalArgumentException("Invalid field name");
        }
        remSegments.add(new FieldAccessSegment(segmentName));
      }
    }
    return Pair.of(qualifiedName, remSegments);
  }

  /**
   * Whether the access path starts from a global memory location
   *
   * @return as above
   */
  public boolean startFromGlobal() {
    if (declaration instanceof CParameterDeclaration) {
      return false;
    } else if (declaration instanceof CDeclaration) {
      return ((CDeclaration) declaration).isGlobal();
    } else {
      throw new IllegalStateException("Invalid declaration type for access path: " + declaration);
    }
  }

}
