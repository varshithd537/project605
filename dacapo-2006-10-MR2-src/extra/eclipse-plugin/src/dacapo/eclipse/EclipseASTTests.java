/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package dacapo.eclipse;

import java.util.List;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests, and
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceASTTests
 */
public class EclipseASTTests extends EclipseTests {
  
  void doTests(int level) throws JavaModelException {
    domAstCreationJLS3();
    if (level > 0) wkspDomAstCreationJLS3();
  }
  
  /**
   * Removed as there's no reference to compare with
   * TODO (frederic) put back post 3.1
   */
  private void domAstCreationJLS3() throws JavaModelException {
    ICompilationUnit unit =	getCompilationUnit("org.eclipse.jdt.core", "org.eclipse.jdt.internal.compiler.parser", "Parser.java");
    System.out.println("\tAST creation: org.eclipse.jdt.internal.compiler.parser");
    createAST(unit, AST.JLS3);
  }
  
  /**
   * Removed as there's no reference to compare with
   * TODO (frederic) put back post 3.1
   */
  private void wkspDomAstCreationJLS3() throws JavaModelException {
    System.out.println("\tAST creation: whole workspace");
    runAstCreation(AST.JLS3);
  }
  
  private void createAST(ICompilationUnit unit, int astLevel) throws JavaModelException {	
    ASTNode result = null;
    ASTParser parser = ASTParser.newParser(astLevel);
    parser.setSource(unit);
    parser.setResolveBindings(false);
    result = parser.createAST(null);
//  assertEquals("Wrong type for node"+result, result.getNodeType(), ASTNode.COMPILATION_UNIT);
    CompilationUnit compilationUnit = (CompilationUnit) result;
    CommentMapperASTVisitor visitor = new CommentMapperASTVisitor(compilationUnit);
    compilationUnit.accept(visitor);
  }
  private int runAstCreation(int astLevel) throws JavaModelException {
    int unitsCount = 0;
    if (DEBUG) System.out.println("Creating AST hierarchy for all units of projects:");
    for (int i = 0; i < ALL_PROJECTS.length; i++) {
      // Get project compilation units
      if (DEBUG) System.out.print("\t- "+ALL_PROJECTS[i].getElementName());
      List units = getProjectCompilationUnits(ALL_PROJECTS[i]);
      int size = units.size();
      if (size == 0) {
        if (DEBUG) System.out.println(": empty!");
        continue;
      }
      unitsCount += size;
      List unitsArrays = splitListInSmallArrays(units, 20);
      int n = unitsArrays.size();
      if (DEBUG)
        if (n==1)
          System.out.print(": "+size+" units to proceed ("+n+" step): ");
        else
          System.out.print(": "+size+" units to proceed ("+n+" steps): ");
      while (unitsArrays.size() > 0) {
        ICompilationUnit[] unitsArray = (ICompilationUnit[]) unitsArrays.remove(0);
        if (DEBUG) System.out.print('.');
        int length = unitsArray.length;
        CompilationUnit[] compilationUnits = new CompilationUnit[length];
        // Create AST tree
        for (int ptr=0; ptr<length; ptr++) {
          ICompilationUnit unit = unitsArray[ptr];
          unitsArray[ptr] = null; // release memory handle
          ASTParser parser = ASTParser.newParser(astLevel);
          parser.setSource(unit);
          parser.setResolveBindings(false);
          ASTNode result = parser.createAST(null);
//        assertEquals("Wrong type for node"+result, result.getNodeType(), ASTNode.COMPILATION_UNIT);
          compilationUnits[ptr] = (CompilationUnit) result;
        }
      }
      if (DEBUG) System.out.println(" done!");
    }
    return unitsCount;
  }
  
  /**
   * Comment Mapper visitor
   */
  class CommentMapperASTVisitor extends ASTVisitor {
    CompilationUnit compilationUnit;
    int nodes = 0;
    int extendedStartPositions = 0;
    int extendedEndPositions = 0;
    
    public CommentMapperASTVisitor(CompilationUnit unit) {
      this.compilationUnit = unit;
    }
    protected boolean visitNode(ASTNode node) {
      // get node positions and extended positions
      int nodeStart = node.getStartPosition();
      int nodeEnd = node.getLength() - 1 - nodeStart;
      int extendedStart = this.compilationUnit.getExtendedStartPosition(node);
      int extendedEnd = this.compilationUnit.getExtendedLength(node) - 1 - extendedStart;
      // update counters
      if (extendedStart < nodeStart) this.extendedStartPositions++;
      if (extendedEnd > nodeEnd) this.extendedEndPositions++;
      this.nodes++;
      return true;
    }
    protected void endVisitNode(ASTNode node) {
      // do nothing
    }
    public boolean visit(AnonymousClassDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(ArrayAccess node) {
      return visitNode(node);
    }
    public boolean visit(ArrayCreation node) {
      return visitNode(node);
    }
    public boolean visit(ArrayInitializer node) {
      return visitNode(node);
    }
    public boolean visit(ArrayType node) {
      visitNode(node);
      return false;
    }
    public boolean visit(AssertStatement node) {
      return visitNode(node);
    }
    public boolean visit(Assignment node) {
      return visitNode(node);
    }
    public boolean visit(Block node) {
      return visitNode(node);
    }
    public boolean visit(BooleanLiteral node) {
      return visitNode(node);
    }
    public boolean visit(BreakStatement node) {
      return visitNode(node);
    }
    public boolean visit(CastExpression node) {
      return visitNode(node);
    }
    public boolean visit(CatchClause node) {
      return visitNode(node);
    }
    public boolean visit(CharacterLiteral node) {
      return visitNode(node);
    }
    public boolean visit(ClassInstanceCreation node) {
      return visitNode(node);
    }
    public boolean visit(CompilationUnit node) {
      return visitNode(node);
    }
    public boolean visit(ConditionalExpression node) {
      return visitNode(node);
    }
    public boolean visit(ConstructorInvocation node) {
      return visitNode(node);
    }
    public boolean visit(ContinueStatement node) {
      return visitNode(node);
    }
    public boolean visit(DoStatement node) {
      return visitNode(node);
    }
    public boolean visit(EmptyStatement node) {
      return visitNode(node);
    }
    public boolean visit(ExpressionStatement node) {
      return visitNode(node);
    }
    public boolean visit(FieldAccess node) {
      return visitNode(node);
    }
    public boolean visit(FieldDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(ForStatement node) {
      return visitNode(node);
    }
    public boolean visit(IfStatement node) {
      return visitNode(node);
    }
    public boolean visit(ImportDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(InfixExpression node) {
      return visitNode(node);
    }
    public boolean visit(InstanceofExpression node) {
      return visitNode(node);
    }
    public boolean visit(Initializer node) {
      return visitNode(node);
    }
    public boolean visit(Javadoc node) {
      // do not visit Javadoc tags by default. Use constructor with
      // boolean to enable.
      if (super.visit(node)) { return visitNode(node); }
      return false;
    }
    public boolean visit(LabeledStatement node) {
      return visitNode(node);
    }
    public boolean visit(MethodDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(MethodInvocation node) {
      return visitNode(node);
    }
    public boolean visit(NullLiteral node) {
      return visitNode(node);
    }
    public boolean visit(NumberLiteral node) {
      return visitNode(node);
    }
    public boolean visit(PackageDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(ParenthesizedExpression node) {
      return visitNode(node);
    }
    public boolean visit(PostfixExpression node) {
      return visitNode(node);
    }
    public boolean visit(PrefixExpression node) {
      return visitNode(node);
    }
    public boolean visit(PrimitiveType node) {
      return visitNode(node);
    }
    public boolean visit(QualifiedName node) {
      return visitNode(node);
    }
    public boolean visit(ReturnStatement node) {
      return visitNode(node);
    }
    public boolean visit(SimpleName node) {
      return visitNode(node);
    }
    public boolean visit(SimpleType node) {
      return visitNode(node);
    }
    public boolean visit(StringLiteral node) {
      return visitNode(node);
    }
    public boolean visit(SuperConstructorInvocation node) {
      return visitNode(node);
    }
    public boolean visit(SuperFieldAccess node) {
      return visitNode(node);
    }
    public boolean visit(SuperMethodInvocation node) {
      return visitNode(node);
    }
    public boolean visit(SwitchCase node) {
      return visitNode(node);
    }
    public boolean visit(SwitchStatement node) {
      return visitNode(node);
    }
    public boolean visit(SynchronizedStatement node) {
      return visitNode(node);
    }
    public boolean visit(ThisExpression node) {
      return visitNode(node);
    }
    public boolean visit(ThrowStatement node) {
      return visitNode(node);
    }
    public boolean visit(TryStatement node) {
      return visitNode(node);
    }
    public boolean visit(TypeDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(TypeDeclarationStatement node) {
      return visitNode(node);
    }
    public boolean visit(TypeLiteral node) {
      return visitNode(node);
    }
    public boolean visit(SingleVariableDeclaration node) {
      return visitNode(node);
    }
    public boolean visit(VariableDeclarationExpression node) {
      return visitNode(node);
    }
    public boolean visit(VariableDeclarationStatement node) {
      return visitNode(node);
    }
    public boolean visit(VariableDeclarationFragment node) {
      return visitNode(node);
    }
    public boolean visit(WhileStatement node) {
      return visitNode(node);
    }
    /* since 3.0 */
    public boolean visit(BlockComment node) {
      return visitNode(node);
    }
    public boolean visit(LineComment node) {
      return visitNode(node);
    }
    public boolean visit(MemberRef node) {
      return visitNode(node);
    }
    public boolean visit(MethodRef node) {
      return visitNode(node);
    }
    public boolean visit(MethodRefParameter node) {
      return visitNode(node);
    }
    public boolean visit(TagElement node) {
      return visitNode(node);
    }
    public boolean visit(TextElement node) {
      return visitNode(node);
    }
    public void endVisit(AnonymousClassDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(ArrayAccess node) {
      endVisitNode(node);
    }
    public void endVisit(ArrayCreation node) {
      endVisitNode(node);
    }
    public void endVisit(ArrayInitializer node) {
      endVisitNode(node);
    }
    public void endVisit(ArrayType node) {
      endVisitNode(node);
    }
    public void endVisit(AssertStatement node) {
      endVisitNode(node);
    }
    public void endVisit(Assignment node) {
      endVisitNode(node);
    }
    public void endVisit(Block node) {
      endVisitNode(node);
    }
    public void endVisit(BooleanLiteral node) {
      endVisitNode(node);
    }
    public void endVisit(BreakStatement node) {
      endVisitNode(node);
    }
    public void endVisit(CastExpression node) {
      endVisitNode(node);
    }
    public void endVisit(CatchClause node) {
      endVisitNode(node);
    }
    public void endVisit(CharacterLiteral node) {
      endVisitNode(node);
    }
    public void endVisit(ClassInstanceCreation node) {
      endVisitNode(node);
    }
    public void endVisit(CompilationUnit node) {
      endVisitNode(node);
    }
    public void endVisit(ConditionalExpression node) {
      endVisitNode(node);
    }
    public void endVisit(ConstructorInvocation node) {
      endVisitNode(node);
    }
    public void endVisit(ContinueStatement node) {
      endVisitNode(node);
    }
    public void endVisit(DoStatement node) {
      endVisitNode(node);
    }
    public void endVisit(EmptyStatement node) {
      endVisitNode(node);
    }
    public void endVisit(ExpressionStatement node) {
      endVisitNode(node);
    }
    public void endVisit(FieldAccess node) {
      endVisitNode(node);
    }
    public void endVisit(FieldDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(ForStatement node) {
      endVisitNode(node);
    }
    public void endVisit(IfStatement node) {
      endVisitNode(node);
    }
    public void endVisit(ImportDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(InfixExpression node) {
      endVisitNode(node);
    }
    public void endVisit(InstanceofExpression node) {
      endVisitNode(node);
    }
    public void endVisit(Initializer node) {
      endVisitNode(node);
    }
    public void endVisit(Javadoc node) {
      endVisitNode(node);
    }
    public void endVisit(LabeledStatement node) {
      endVisitNode(node);
    }
    public void endVisit(MethodDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(MethodInvocation node) {
      endVisitNode(node);
    }
    public void endVisit(NullLiteral node) {
      endVisitNode(node);
    }
    public void endVisit(NumberLiteral node) {
      endVisitNode(node);
    }
    public void endVisit(PackageDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(ParenthesizedExpression node) {
      endVisitNode(node);
    }
    public void endVisit(PostfixExpression node) {
      endVisitNode(node);
    }
    public void endVisit(PrefixExpression node) {
      endVisitNode(node);
    }
    public void endVisit(PrimitiveType node) {
      endVisitNode(node);
    }
    public void endVisit(QualifiedName node) {
      endVisitNode(node);
    }
    public void endVisit(ReturnStatement node) {
      endVisitNode(node);
    }
    public void endVisit(SimpleName node) {
      endVisitNode(node);
    }
    public void endVisit(SimpleType node) {
      endVisitNode(node);
    }
    public void endVisit(StringLiteral node) {
      endVisitNode(node);
    }
    public void endVisit(SuperConstructorInvocation node) {
      endVisitNode(node);
    }
    public void endVisit(SuperFieldAccess node) {
      endVisitNode(node);
    }
    public void endVisit(SuperMethodInvocation node) {
      endVisitNode(node);
    }
    public void endVisit(SwitchCase node) {
      endVisitNode(node);
    }
    public void endVisit(SwitchStatement node) {
      endVisitNode(node);
    }
    public void endVisit(SynchronizedStatement node) {
      endVisitNode(node);
    }
    public void endVisit(ThisExpression node) {
      endVisitNode(node);
    }
    public void endVisit(ThrowStatement node) {
      endVisitNode(node);
    }
    public void endVisit(TryStatement node) {
      endVisitNode(node);
    }
    public void endVisit(TypeDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(TypeDeclarationStatement node) {
      endVisitNode(node);
    }
    public void endVisit(TypeLiteral node) {
      endVisitNode(node);
    }
    public void endVisit(SingleVariableDeclaration node) {
      endVisitNode(node);
    }
    public void endVisit(VariableDeclarationExpression node) {
      endVisitNode(node);
    }
    public void endVisit(VariableDeclarationStatement node) {
      endVisitNode(node);
    }
    public void endVisit(VariableDeclarationFragment node) {
      endVisitNode(node);
    }
    public void endVisit(WhileStatement node) {
      endVisitNode(node);
    }
    /* since 3.0 */
    public void endVisit(BlockComment node) {
      endVisitNode(node);
    }
    public void endVisit(LineComment node) {
      endVisitNode(node);
    }
    public void endVisit(MemberRef node) {
      endVisitNode(node);
    }
    public void endVisit(MethodRef node) {
      endVisitNode(node);
    }
    public void endVisit(MethodRefParameter node) {
      endVisitNode(node);
    }
    public void endVisit(TagElement node) {
      endVisitNode(node);
    }
    public void endVisit(TextElement node) {
      endVisitNode(node);
    }
  }
  
}
