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
package org.sosy_lab.cpachecker.util.globalinfo;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.location.LocationState.LocationStateFactory;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.callgraph.CallGraph;
import org.sosy_lab.cpachecker.util.callgraph.CallGraphBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;


public class CFAInfo {

  private final Map<Integer, CFANode> nodeNumberToNode;
  private LocationStateFactory locationStateFactory;
  // in order to simplify the convention on CPA phase, we add a member to directly
  private final CFA cfa;
  // call graph
  private final CallGraph callGraph;
  // store function definitions
  private final Map<String, CFunctionDeclaration> name2Fun;
  // mapping from qualified name to declared type
  private final Map<String, CType> var2Type;

  CFAInfo(CFA cfa) {
    this.cfa = cfa;
    // initialize node number to node map
    HashMap<Integer, CFANode> nodeNumberToNode = new HashMap<>();
    for (CFANode node : cfa.getAllNodes()) {
      nodeNumberToNode.put(node.getNodeNumber(), node);
    }
    this.nodeNumberToNode = nodeNumberToNode;
    // function declarations
    name2Fun = Collections.unmodifiableMap(collectFunctionDeclarations(cfa));
    // variable declarations
    var2Type = Collections.unmodifiableMap(collectVariableDeclarations(cfa));
    // build call graph
    callGraph = CallGraphBuilder.getInstance().buildCallGraph(cfa);
  }

  private Map<String, CFunctionDeclaration> collectFunctionDeclarations(CFA cfa) {
    final Map<String, CFunctionDeclaration> decls = new HashMap<>();
    CFAVisitor visitor = new CFAVisitor() {
      @Override
      public TraversalProcess visitEdge(CFAEdge pEdge) {
        if (pEdge instanceof CDeclarationEdge) {
          // definition is in the form of CDeclaration
          CDeclaration decl = ((CDeclarationEdge) pEdge).getDeclaration();
          if (decl instanceof CFunctionDeclaration) {
            CFunctionDeclaration funDecl = (CFunctionDeclaration) decl;
            decls.put(funDecl.getName(), funDecl);
          }
          return TraversalProcess.CONTINUE;
        } else if (pEdge instanceof BlankEdge) {
          return TraversalProcess.CONTINUE;
        } else {
          return TraversalProcess.ABORT;
        }
      }

      @Override
      public TraversalProcess visitNode(CFANode pNode) {
        return TraversalProcess.CONTINUE;
      }
    };
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), visitor);
    return decls;
  }

  private Map<String, CType> collectVariableDeclarations(CFA cfa) {
    final Map<String, CType> vars = new HashMap<>();
    CFAVisitor visitor = new CFAVisitor() {
      @Override
      public TraversalProcess visitEdge(CFAEdge edge) {
        if (edge instanceof CDeclarationEdge) {
          CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
          if(decl instanceof CVariableDeclaration) {
            CVariableDeclaration varDecl = (CVariableDeclaration) decl;
            vars.put(varDecl.getQualifiedName(), varDecl.getType());
          }
        } else if (edge instanceof CFunctionCallEdge) {
          CFunctionEntryNode entryNode = ((CFunctionCallEdge) edge).getSuccessor();
          List<CParameterDeclaration> parameters = entryNode.getFunctionParameters();
          for(CParameterDeclaration parameter : parameters) {
            vars.put(parameter.getQualifiedName(), parameter.getType());
          }
          Optional<CVariableDeclaration> returnVar = entryNode.getReturnVariable();
          if (returnVar.isPresent()) {
            CVariableDeclaration returnDecl = returnVar.get();
            vars.put(returnDecl.getQualifiedName(), returnDecl.getType());
          }
        }
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitNode(CFANode node) {
        return TraversalProcess.CONTINUE;
      }
    };
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), visitor);
    return vars;
  }

  public CFANode getNodeByNodeNumber(int nodeNumber) {
    return nodeNumberToNode.get(nodeNumber);
  }

  public void storeLocationStateFactory(LocationStateFactory pElementFactory) {
    locationStateFactory = pElementFactory;
  }

  public LocationStateFactory getLocationStateFactory() {
    return locationStateFactory;
  }

  public CFA getCFA() {
    return cfa;
  }

  /**
   * Get all function DECLARATION
   *
   * note that, cfa.getAllFunctionNames returns a list of DEFINED functions.
   *
   * @return unmodifiable map.
   */
  public Map<String, CFunctionDeclaration> getFunctionDecls() {
    return name2Fun;
  }

  @Nullable
  public CType getType(String qualifiedName) {
    return var2Type.get(qualifiedName);
  }

  /**
   * Function Call Graph
   */
  public CallGraph getCallGraph() {
    return callGraph;
  }

}
