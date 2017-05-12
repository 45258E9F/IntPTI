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
package org.sosy_lab.cpachecker.core.bugfix;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A simple structure for manipulating AST, especially for code refactoring.
 *
 * Basically, a mutable AST is built based on wrapped CDT AST. However, when CDT ASTs are
 * overlapped, we simply discard gap text between them.
 */
public final class MutableASTForFix {

  private IASTNode astNode;

  private MutableASTForFix parent;
  private List<MutableASTForFix> children;

  // template for serialization
  // Invariant: the size of template should be always one more than the size of children
  private List<String> template;

  private MutableASTForFix(IASTNode pNode) {
    astNode = pNode;
    parent = null;
    children = new ArrayList<>();
    template = new ArrayList<>();
  }

  private void makeLeaf() {
    template.add(astNode.getRawSignature());
  }

  /**
   * The content of the AST node changes only if the current node is a leaf node.
   */
  public void writeToLeaf(String newContent) {
    if (children.isEmpty()) {
      template.set(0, newContent);
    }
  }

  public void writeToMarginalText(String newContent) {
    template.set(0, newContent);
  }

  public void writeToTailText(String newContent) {
    template.set(template.size() - 1, newContent);
  }

  /**
   * Clean the template text (i.e. set the target content as empty) recursively.
   */
  public void cleanText() {
    for (int i = 0; i < template.size(); i++) {
      template.set(i, "");
    }
    for (MutableASTForFix child : children) {
      child.cleanText();
    }
  }

  public void setPrecedentText(String content) {
    Integer order = getOrderOfChild();
    if (order != null) {
      parent.template.set(order, content);
    }
  }

  public void setSuccessorText(String content) {
    Integer order = getOrderOfChild();
    if (order != null) {
      parent.template.set(order + 1, content);
    }
  }

  /**
   * Add children AST nodes.
   * For each AST node, this method could be called for only once.
   *
   * @return true if everything works fine, otherwise children overlaps
   */
  private boolean addChildren(List<MutableASTForFix> pChildren) {
    if (pChildren.size() == 0) {
      makeLeaf();
      return true;
    }
    String rawString = astNode.getRawSignature();
    IASTFileLocation parentLoc = astNode.getFileLocation();
    int baseOffset = parentLoc.getNodeOffset();

    // relative offset at the start/end of AST node
    int startPos = 0, endPos;
    for (MutableASTForFix child : pChildren) {
      IASTFileLocation subLoc = child.astNode.getFileLocation();
      endPos = subLoc.getNodeOffset() - baseOffset;

      if (startPos > endPos) {
        // such case occurs when two CDT ASTs are overlapped
        return false;
      }
      // extract the gap text and update the template
      String gapText = rawString.substring(startPos, endPos);

      template.add(gapText);

      startPos = endPos + subLoc.getNodeLength();
      children.add(child);
      child.parent = this;
    }
    endPos = parentLoc.getNodeLength();
    String lastGapText = rawString.substring(startPos, endPos);
    template.add(lastGapText);
    assert (template.size() == children.size() + 1) : "Invariant on template is violated";
    return true;
  }

  /**
   * Compute the k-level parent of an AST node.
   *
   * @param level If level <= 1, we get the direct parent of current node, otherwise, we get the
   *              k-level parent of the current node.
   */
  @Nullable
  public MutableASTForFix getParent(int level) {
    MutableASTForFix result = this;
    while (level > 1) {
      if (result == null) {
        return null;
      }
      result = result.parent;
      level--;
    }

    if (result == null) {
      return null;
    }
    return result.parent;
  }

  public String getMarginalText() {
    // the size of template is no less than 1
    return template.get(0);
  }

  public String getTailText() {
    return template.get(template.size() - 1);
  }

  public String getSuccessorText() {
    Integer order = getOrderOfChild();
    if (order != null) {
      return parent.template.get(order + 1);
    }
    return "";
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  @Nullable
  private Integer getOrderOfChild() {
    if (parent == null) {
      return null;
    }
    int hit;
    for (hit = 0; hit < parent.children.size(); hit++) {
      if (parent.children.get(hit) == this) {
        break;
      }
    }
    assert (hit < parent.children.size());
    return hit;
  }

  public String synthesize() {
    StringBuilder builder = new StringBuilder();
    builder.append(template.get(0));
    for (int i = 0; i < children.size(); i++) {
      builder.append(children.get(i).synthesize());
      builder.append(template.get(i + 1));
    }
    return builder.toString();
  }

  public IASTNode getWrappedNode() {
    return astNode;
  }

  public List<MutableASTForFix> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public MutableASTForFix getOnlyLeaf() {
    MutableASTForFix result = this;
    while (result.children.size() > 0) {
      result = result.children.get(0);
    }
    return result;
  }

  public static MutableASTForFix createMutableASTFromTranslationUnit(IASTNode pNode) {
    MutableASTForFix node = new MutableASTForFix(pNode);
    IASTNode[] children = pNode.getChildren();
    if (children.length == 0) {
      node.makeLeaf();
      return node;
    }
    List<MutableASTForFix> childNodes = new ArrayList<>();
    for (IASTNode child : children) {
      if (child.getFileLocation() != null && child.getFileLocation().getNodeLength() > 0) {
        MutableASTForFix childNode = createMutableASTFromTranslationUnit(child);
        childNodes.add(childNode);
      }
      // otherwise, we think such node should not exist in the AST
    }
    boolean normal = node.addChildren(childNodes);
    if (normal) {
      return node;
    } else {
      // overlapping happens
      // we discard all modifications on the original mutable AST
      node = new MutableASTForFix(pNode);
      node.makeLeaf();
      return node;
    }
  }

}
