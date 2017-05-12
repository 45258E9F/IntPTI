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
package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

import java.util.Iterator;
import java.util.Stack;

/**
 * A function is basically a stack of blocks, where the first element is the
 * outermost block of the function and the last element is the current block.
 */
class FunctionBody implements Iterable<BasicBlock> {

  private final Stack<BasicBlock> stack = new Stack<>();

  public FunctionBody(int pElementId, String pFunctionName) {
    stack.push(new BasicBlock(pElementId, pFunctionName));
  }

  public FunctionBody(FunctionBody oldStack) {
    stack.addAll(oldStack.stack);
  }

  public void enterBlock(int pElementId, CAssumeEdge pEdge, String pConditionString) {
    BasicBlock block = new BasicBlock(pElementId, pEdge, pConditionString);
    stack.peek().write(block); // register the inner block in its outer block
    stack.push(block);
  }

  public void leaveBlock() {
    stack.pop();
  }

  public BasicBlock getCurrentBlock() {
    return stack.peek();
  }

  public int size() {
    return stack.size();
  }

  @Override
  public Iterator<BasicBlock> iterator() {
    return stack.iterator();
  }

  public void write(String s) {
    stack.peek().write(s);
  }

  @Override
  public String toString() {
    // To write the C code, we need only the outermost block of the function.
    // It will print its nested blocks automatically as needed.
    return stack.get(0).getCode();
  }
}
