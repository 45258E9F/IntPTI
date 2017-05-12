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
package org.sosy_lab.cpachecker.util.access;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

import java.util.List;
import java.util.Map;

public class AccessPathBuilder {
  private static AccessPathBuilder builder;

  // to store qualifiedName -> declaration; we need this to build AccessPath from list of string
  private Map<String, CSimpleDeclaration> declarationMap;

  private AccessPathBuilder() {
    declarationMap = Maps.newHashMap();
  }

  public static AccessPathBuilder getInstance() {
    if (builder == null) {
      builder = new AccessPathBuilder();
    }
    return builder;
  }

  public void addDeclaration(String name, CSimpleDeclaration del) {
    declarationMap.put(name, del);
  }

  public CSimpleDeclaration getDeclaration(String name) {
    return declarationMap.get(name);
  }

  public AccessPath buildAccessPath(List<String> paths) {
    AccessPath ap = null;
    // clean paths: remove *& , &*
    paths = clean(paths);
    if (!declarationMap.containsKey(paths.get(0))) {
      return null;
    }
    ap = new AccessPath(declarationMap.get(paths.get(0)));
    for (int i = 1; i < paths.size(); i++) {
      PathSegment ps = buildPathSegmentFromString(paths.get(i));
      // we meet unknown path
      if (ps == null) {
        return null;
      }
      ap.appendSegment(ps);
    }
    return ap;
  }

  private List<String> clean(List<String> pPaths) {
    List<String> temList = Lists.newArrayList();
    for (int i = 0; i < pPaths.size(); i++) {
      // for & *
      if (pPaths.get(i).equals(AddressingSegment.INSTANCE.getName())) {
        if ((i + 1) < pPaths.size() && pPaths.get(i + 1).equals(
            (new PointerDereferenceSegment()).getName())) {
          i = i + 1;
          continue;
        }
      }
      // for *&
      if (pPaths.get(i).equals((new PointerDereferenceSegment()).getName())) {
        if ((i + 1) < pPaths.size() && pPaths.get(i + 1).equals(
            AddressingSegment.INSTANCE.getName())) {
          i = i + 1;
          continue;
        }
      }
      temList.add(pPaths.get(i));

    }
    return temList;
  }

  // FIXME me, currently, we only can build the accesspath from string.
  // it may result bugs. How can we ensure, string and segment is one:one mapping?
  private PathSegment buildPathSegmentFromString(String pString) {

    if (pString.equals(AddressingSegment.INSTANCE.getName())) {
      return AddressingSegment.INSTANCE;
    }
    // ArrayAccessSegment not used in Access summary
    if (pString.equals(ArrayAccessSegment.INSTANCE.getName())) {
      return ArrayAccessSegment.INSTANCE;
    }
    // ArrayConstIndexSegment not used in Access summary
    if (pString.startsWith("[") && pString.endsWith("]") && checkIfNumber(
        pString.substring(1, pString.length() - 1))) {
      return new ArrayConstIndexSegment(Long.parseLong(pString.substring(1, pString.length() - 1)));
    }
    // PointerDereferenceSegment
    if (pString.equals("*")) {
      return new PointerDereferenceSegment();
    }

    return new FieldAccessSegment(pString);
  }

  public boolean checkIfNumber(String in) {

    try {

      Long.parseLong(in);

    } catch (NumberFormatException ex) {
      return false;
    }

    return true;
  }
}
