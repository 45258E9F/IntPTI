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

import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.access.AccessAnalysisState;
import org.sosy_lab.cpachecker.cpa.access.AccessPathVisitor;
import org.sosy_lab.cpachecker.cpa.access.AccessSummaryProvider;
import org.sosy_lab.cpachecker.cpa.access.summary.AccessSummaryPathVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.collections.preliminary.Presence;
import org.sosy_lab.cpachecker.util.collections.tree.PathCopyingPersistentTree;
import org.sosy_lab.cpachecker.util.collections.tree.TreeVisitor;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class AccessSummaryUtil {

  /**
   * Attach src.srcPath to dst.dstPath:
   * read tree:  attach entire subtree
   * write tree: attach only those path with a * segment// tomgu, we attch entire subtree
   * here, we check in transferRelation
   *
   * Used in: Assignment or FunctionCall
   *
   * E.g., func(x.a, y.b.c);
   *
   * attach summary.$0 to x.a
   * attach summary.$1 to y.b.c
   *
   * Note:
   * 1. when there is recursive function call, we have to check whether it has been read/write
   * E.g fA(a){a.i = 1;fB(a.ap)} fB(b){b.j = 1;fA(b.bp)}
   *
   * @param dst     targetState
   * @param dstPath target path
   * @param src     source state
   * @param srcPath source path
   * @param read    attach read tree
   * @param write   attach write tree
   * @return the modified dst (immutable)
   */
  public static AccessAnalysisState attach(
      AccessAnalysisState dst,
      List<String> dstPath,
      AccessAnalysisState src,
      List<String> srcPath,
      boolean read, boolean write) {

    PathCopyingPersistentTree<String, Presence> readTree = PathCopyingPersistentTree.of();
    PathCopyingPersistentTree<String, Presence> writeTree = PathCopyingPersistentTree.of();
    // attach read tree
    if (read) {
      readTree = src.readTree.getSubtree(srcPath);
      readTree = updatePersistentTree(readTree, dstPath);
    }
    // attach write tree
    if (write) {
      writeTree = src.writeTree.getSubtree(srcPath);
      writeTree = updatePersistentTree(writeTree, dstPath);
    }
    return dst.joinSubState(dstPath, new AccessAnalysisState(readTree, writeTree));
  }

  /**
   * apply the source tree to target tree
   * 1. find all the leaf node's path
   * 2. set new tree by the paths
   *
   * @param tree    sub of source tree
   * @param dstPath target path prefix
   */
  private static PathCopyingPersistentTree<String, Presence> updatePersistentTree(
      PathCopyingPersistentTree<String, Presence> tree,
      List<String> dstPath) {
    final List<List<String>> paths = new ArrayList<>();
    tree.traverse(new TreeVisitor<String, Presence>() {
      @Override
      public TreeVisitStrategy visit(Stack<String> path, Presence element, boolean isLeaf) {
        // we do not need the last *, see  writeTree.setElementAndCopy(wp, Presence.INSTANCE);
        if (isLeaf) {// add path and skip to continue other branches
          List<String> p = new ArrayList<>(path);
          paths.add(p);
          return TreeVisitStrategy.SKIP;
        } else {
          return TreeVisitStrategy.CONTINUE;
        }
      }
    });

    PathCopyingPersistentTree<String, Presence> resultTree = PathCopyingPersistentTree.of();
    List<String> subDst;
    for (List<String> wp : paths) {
      // for dstPath in the subtree, we ignore: a = p->a; This occurs in linked data structure
      if (dstPath.size() == 1) { // dst.size == 1 , funName::a, we have to remove funName::
        subDst = new ArrayList<>();
        subDst.add(getNameFromQualifiedName(dstPath.get(0)));
      } else {// for recursive function call, we do not record multiple times
        subDst = dstPath.subList(1, dstPath.size());
      }
      if (wp.containsAll(subDst)) {
        continue;
      }
      resultTree = resultTree.setElementAndCopy(wp, Presence.INSTANCE);
    }
    return resultTree;
  }

  /**
   * get name from qualifiedName
   * For example: return a in funName::a
   */
  private static String getNameFromQualifiedName(String qName) {
    String[] results = qName.split("::", 2);
    if (results.length == 1) {
      return results[0];
    } else {
      return results[1];
    }
  }

  /**
   * Calculate the pre-condition of a function invocation
   * Note: in function call, the read as usual. For write, only the value of pointer and array will
   * be written. Other types is called by value.
   * x = f(a1, a2, ..., an)
   * {postState}
   *
   * @param functionName name of the function
   * @param summaryMap   provides a summary map
   * @param arguments    actual arguments
   * @param postState    post condition
   * @return precondition
   * @throws UnrecognizedCCodeException when unrecongnized code encountered.
   */
  public static AccessAnalysisState apply(
      String functionName,
      Map<String, AccessAnalysisState> summaryMap,
      List<CExpression> arguments,
      AccessAnalysisState postState) throws UnrecognizedCCodeException {
    // both funDecl and summary may be null if function definition is not found
    if (!(GlobalInfo.getInstance().getCFAInfo().isPresent())) {
      return postState;
    }
    CFunctionDeclaration funDecl =
        GlobalInfo.getInstance().getCFAInfo().get().getFunctionDecls().get(functionName);
    // without declaration
    if (funDecl == null) {
      return postState;
    }

    AccessAnalysisState summary = summaryMap.get(functionName);
    if (summary == null) {
      // no definition, assume all parameters are read
      // if parameter is a pointer, its value should be written
      AccessAnalysisState preState = postState;
      // For variadic function, we compute the fixed parameters
      for (int i = 0; i < funDecl.getParameters().size(); i++) {
        // 1.get access path
        CExpression exp = arguments.get(i);
        AccessSummaryPathVisitor visitor = new AccessSummaryPathVisitor(preState, summaryMap);
        AccessPath argumentAP = exp.accept(visitor);
        preState = visitor.getState();
        if (argumentAP != null) {
          // 2. make read
          List<String> dstReadPath = AccessPath.toStrList(argumentAP); // set read path
          List<String> dstWritePath;
          preState = preState.read(dstReadPath);
          // 3. make write: only the pointer or array will be written.
          if (exp.getExpressionType() instanceof CPointerType) {
            // 3.1 int *p; write(p) -> p* will be written
            argumentAP.appendSegment(new PointerDereferenceSegment());
            dstWritePath = AccessPath.toStrList(argumentAP);
            preState = preState.write(dstWritePath);
          } else if (exp.getExpressionType() instanceof CArrayType) {
            // 3.2 int a[]; a[0] = x; a will be written
            dstWritePath = AccessPath.toStrList(argumentAP);
            preState = preState.write(dstWritePath);
          } else {
            // we do not write
          }
        }
      }
      return preState;
    } else {
      // apply the summary to arguments
      // we have to check the arguments type here, it is used in multiple class,
      //
      if (arguments.size() >= funDecl.getParameters().size()) {
        AccessAnalysisState preState = postState;
        // for the variadic function
        for (int i = 0; i < funDecl.getParameters().size(); i++) {
          // we check the type first
          CParameterDeclaration parameterDeclaration = funDecl.getParameters().get(i);
          if (!AccessSummaryUtil
              .typeEqual(arguments.get(i), parameterDeclaration)) {
            continue;
          }
          AccessSummaryPathVisitor visitor = new AccessSummaryPathVisitor(preState, summaryMap);
          AccessPath ap = arguments.get(i).accept(visitor);
          preState = visitor.getState();
          if (ap != null) {
            List<String> dstPath = AccessPath.toStrList(ap);
            List<String> srcPath;
            srcPath = Collections.singletonList(funDecl.getParameters().get(i).getQualifiedName());
            // Read access to actual parameters are already handled.
            // do not need to load state from visitor
            // TODO : check type, we can process this in summarize
            CType parameterDeclarationType = parameterDeclaration.getType();
            // pointer or array
//            if(parameterDeclarationType instanceof CPointerType || parameterDeclarationType instanceof CArrayType){
//              preState = attach(preState, dstPath, summary, srcPath, true, true);
//            }else{
//              preState = attach(preState, dstPath, summary, srcPath, true, false);
//            }
            preState = attach(preState, dstPath, summary, srcPath, true, true);
          }
        }
        return preState;
      } else {
        // var args we do not process it
        // throw new RuntimeException("Var args not supported.");
        return postState;
      }
    }
  }

  /**
   * check whether the argument's type equals parameter's type
   * 1. cast expression as the cast type
   * 2. we ignore all the function type -> we treat them not equal
   * 3. we treat void type as TOP
   *
   * @param pCExpression           argument
   * @param pCParameterDeclaration parameter
   * @return equal or not
   */
  private static boolean typeEqual(
      CExpression pCExpression,
      CParameterDeclaration pCParameterDeclaration) {
    AccessTypeVisitor visitor = AccessTypeVisitor.getInstance();
    try {
      // we treat CCastExpression as cast type
      List<String> expressionType;
      if (pCExpression instanceof CCastExpression) {
        expressionType = ((CCastExpression) pCExpression).getCastType().accept(visitor);
      } else {
        expressionType = pCExpression.getExpressionType().accept(visitor);
      }
      List<String> parameterType = pCParameterDeclaration.getType().accept(visitor);
      // we ignore all the function type
      if (expressionType.contains(AccessTypeVisitor.TypeKind.FUNCTION.toASTString()) ||
          parameterType.contains(AccessTypeVisitor.TypeKind.FUNCTION.toASTString())) {
        return false;
      }

      if (expressionType.size() != parameterType.size()) {
        // we treat void pointer as true;
        if (parameterType.contains(AccessTypeVisitor.TypeKind.VOID.toASTString())) {
          int index = parameterType.lastIndexOf(AccessTypeVisitor.TypeKind.VOID.toASTString());
          if (parameterType.size() >= index + 1) {
            // should be pointer
            String str = parameterType.get(index + 1);
            if (str.equals(AccessTypeVisitor.TypeKind.POINTER.toASTString())) {
              return true;
            } else {
              return false;
            }
          } else {
            return false;
          }
        }
        return false;
      } else {
        // we treat void as any
        for (int i = 0; i < expressionType.size(); i++) {
          if (expressionType.get(i).equals(AccessTypeVisitor.TypeKind.VOID.toASTString()) ||
              parameterType.get(i).equals(AccessTypeVisitor.TypeKind.VOID.toASTString())) {
            continue;
          } else if (expressionType.get(i).equals(AccessTypeVisitor.TypeKind.ANYNAME.toASTString())
              ||
              parameterType.get(i).equals(AccessTypeVisitor.TypeKind.ANYNAME.toASTString())) {
            continue;
          } else if (expressionType.get(i).equals(parameterType.get(i))) {
            continue;
          } else {
            return false;
          }
        }
        return true;
      }
    } catch (UnrecognizedCCodeException e) {
      return false;
    }
  }

  // check a type is a function type
  public static boolean isFunctionType(CType type) {
    if (type instanceof CArrayType) {
      return isFunctionType(((CArrayType) type).getType());
    } else if (type instanceof CFunctionType) {
      return true;
    } else if (type instanceof CPointerType) {
      return isFunctionType(((CPointerType) type).getType());
    } else if (type instanceof CProblemType) {
      return true;
    } else if (type instanceof CSimpleType) {
      return false;
    } else if (type instanceof CTypedefType) {
      return isFunctionType(((CTypedefType) type).getRealType());
    } else if (type instanceof CVoidType) {
      return false;
    } else {// for complexType -> enum struct union
      return false;
    }
  }

  // From now: the old version
  public static AccessAnalysisState attach(
      AccessAnalysisState dst,
      List<String> dstPath,
      AccessAnalysisState src,
      List<String> srcPath) {
    return attach(dst, dstPath, src, srcPath, true, true);
  }

  public static AccessAnalysisState mostGeneralSummary(CFunctionDeclaration funDecl) {
    AccessAnalysisState state = new AccessAnalysisState();
    for (CParameterDeclaration pd : funDecl.getParameters()) {
//      System.out.println("Parameter " + pd.getName());
//      String paramPrefix = "$" + i;
      String paramPrefix = pd.getQualifiedName();
      state = AccessAnalysisState.markRead(Collections.singletonList(paramPrefix), state);
      // write paths
      final List<List<String>> readPaths = new ArrayList<>();
      final List<List<String>> writePaths = new ArrayList<>();
      // collect write paths
      // 1. build parameter tree
      // 2. collect path with a '*' segment
      PathCopyingPersistentTree<String, CType> tree = PathCopyingPersistentTree.wrap(
          Collections.singletonList(paramPrefix),
          AccessPath.parse(pd.getType())
      );
      tree.traverse(new TreeVisitor<String, CType>() {
        @Override
        public TreeVisitStrategy visit(Stack<String> path, CType element, boolean isLeaf) {
          if (!path.isEmpty() && path.peek().equals("*")) {
            List<String> p = new ArrayList<>(path);
            readPaths.add(p);
            writePaths.add(p);
            return TreeVisitStrategy.SKIP;
          } else {
            if (isLeaf) {
              readPaths.add(new ArrayList<>(path));
            }
            return TreeVisitStrategy.CONTINUE;
          }
        }
      });
      for (List<String> rp : readPaths) {
        // System.out.println("reading " + rp);
        state = state.read(rp); // tomgu it should be read
      }
      for (List<String> wp : writePaths) {
        // System.out.println("writing " + wp);
        state = state.write(wp);
      }
    }
    return state;
  }

  /**
   * Calculate the pre-condition of a function invocation
   *
   * {???}
   * x = f(a1, a2, ..., an)
   * {postState}
   *
   * @param functionName name of the function
   * @param provider     provides a summary manager
   * @param arguments    actual arguments
   * @param postState    post condition
   * @return precondition
   * @throws UnrecognizedCCodeException when unrecongnized code encountered.
   */
  public static AccessAnalysisState apply(
      String functionName,
      AccessSummaryProvider provider,
      List<CExpression> arguments,
      AccessAnalysisState postState) throws UnrecognizedCCodeException {
    // both funDecl and summary may be null if function definition is not found
    CFunctionDeclaration funDecl = GlobalInfo.getInstance()
        .getCFAInfo().get()
        .getFunctionDecls()
        .get(functionName);
    // without declaration
    if (funDecl == null) {
      return postState;
    }

    AccessAnalysisState summary = provider.provide().getSummary(functionName);
    if (summary == null) {
      // no definition, assume all parameters are both read and written
      // if parameter is a reference, its value should be write
      AccessAnalysisState preState = postState;
      // For variadic function, we compute the fixed parameters
      for (int i = 0; i < funDecl.getParameters().size(); i++) {
        CExpression exp = arguments.get(i);
        AccessPathVisitor visitor = new AccessPathVisitor(preState, provider);
        AccessPath ap = exp.accept(visitor);
        preState = visitor.getState();
        if (ap != null) {
          List<String> dstReadPath = AccessPath.toStrList(ap);
          List<String> dstWritePath = AccessPath.toStrList(ap);
          // last is &, write should be a|&|*
          if (ap.getLastSegment() instanceof AddressingSegment) {
            ap.appendSegment(new PointerDereferenceSegment());
            dstWritePath = AccessPath.toStrList(ap);
          }
          preState = preState.read(dstReadPath).write(dstWritePath);
        }
      }
      return preState;
    } else {
      // apply the summary to arguments
      if (arguments.size() >= funDecl.getParameters().size()) {
        AccessAnalysisState preState = postState;
        // for the variadic function
        for (int i = 0; i < funDecl.getParameters().size(); i++) {
          // we check the type first
          if (!AccessSummaryUtil.typeEqual(arguments.get(i), funDecl.getParameters().get(i))) {
            continue;
          }

          AccessPathVisitor visitor = new AccessPathVisitor(preState, provider);
          AccessPath ap = arguments.get(i).accept(visitor);
          preState = visitor.getState();
          if (ap != null) {
            List<String> dstPath = AccessPath.toStrList(ap);
            List<String> srcPath;
            // summary has prefix in the form of '$0', '$1', ...
            //srcPath = Collections.singletonList("$" + i);

            // for function with definition, it not use $0, but paramter's name

            srcPath = Collections.singletonList(funDecl.getParameters().get(i).getQualifiedName());
            // Read access to actual parameters are already handled.
            // do not need to load state from visitor
            preState = attach(preState, dstPath, summary, srcPath);
          }
        }
        return preState;
      } else {
        // var args we do not process it
        // throw new RuntimeException("Var args not supported.");
        return postState;
      }
    }
  }
}
