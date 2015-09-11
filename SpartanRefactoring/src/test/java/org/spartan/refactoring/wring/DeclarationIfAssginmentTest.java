package org.spartan.refactoring.wring;

import org.spartan.refactoring.utils.Is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.spartan.hamcrest.CoreMatchers.is;
import static org.spartan.hamcrest.MatcherAssert.assertThat;
import static org.spartan.hamcrest.MatcherAssert.compressSpaces;
import static org.spartan.hamcrest.MatcherAssert.iz;
import static org.spartan.hamcrest.OrderingComparison.greaterThan;
import static org.spartan.refactoring.spartanizations.TESTUtils.assertSimilar;
import static org.spartan.refactoring.utils.Funcs.*;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.spartan.refactoring.spartanizations.TESTUtils;
import org.spartan.refactoring.spartanizations.Wrap;
import org.spartan.refactoring.utils.As;
import org.spartan.refactoring.utils.Extract;
import org.spartan.refactoring.utils.Search;
import org.spartan.refactoring.utils.Search.Of;
import org.spartan.refactoring.wring.AbstractWringTest.OutOfScope;
import org.spartan.refactoring.wring.Wring.VariableDeclarationFragementAndStatement;
import org.spartan.utils.Utils;

/**
 * @author Yossi Gil
 * @since 2014-07-13
 */
@SuppressWarnings({ "javadoc", "static-method" }) //
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
public class DeclarationIfAssginmentTest {
  static final DeclarationInitializerIfAssignment WRING = new DeclarationInitializerIfAssignment();
  @Test public void traceForbiddenSiblings() {
    assertNotNull(WRING);
    final String from = "int a = 2,b; if (b) a =3;";
    final String wrap = Wrap.Statement.on(from);
    final CompilationUnit u = (CompilationUnit) As.COMPILIATION_UNIT.ast(wrap);
    final VariableDeclarationFragment f = Extract.firstVariableDeclarationFragment(u);
    assertThat(f, notNullValue());
    assertThat(WRING.scopeIncludes(f), is(false));
  }
  @Test public void traceForbiddenSiblingsExpanded() {
    final String from = "int a = 2,b; if (a+b) a =3;";
    final String wrap = Wrap.Statement.on(from);
    final CompilationUnit u = (CompilationUnit) As.COMPILIATION_UNIT.ast(wrap);
    final VariableDeclarationFragment f = Extract.firstVariableDeclarationFragment(u);
    assertThat(f, notNullValue());
    final Expression initializer = f.getInitializer();
    assertNotNull(initializer);
    final IfStatement s = Extract.nextIfStatement(f);
    assertThat(s, is(Extract.firstIfStatement(u)));
    assertNotNull(s);
    assertThat(s, iz("if (a + b) a=3;"));
    assertTrue(Is.vacuousElse(s));
    final Assignment a = Extract.assignment(then(s));
    assertNotNull(a);
    assertTrue(same(left(a), f.getName()));
    assertThat(a.getOperator(), is(Assignment.Operator.ASSIGN));
    final List<VariableDeclarationFragment> x = VariableDeclarationFragementAndStatement.forbiddenSiblings(f);
    assertThat(x.size(), greaterThan(0));
    assertThat(x.size(), is(1));
    final VariableDeclarationFragment b = x.get(0);
    assertThat(b.toString(), is("b"));
    final Of of = Search.BOTH_SEMANTIC.of(b);
    assertNotNull(of);
    final Expression e = s.getExpression();
    assertNotNull(e);
    assertThat(e, iz("a + b"));
    final List<Expression> in = of.in(e);
    assertThat(in.size(), is(1));
    assertThat(!in.isEmpty(), is(true));
    assertThat(Search.BOTH_SEMANTIC.of(f).existIn(s.getExpression(), right(a)), is(true));
    assertThat(of.existIn(s.getExpression(), right(a)), is(true));
  }

  @RunWith(Parameterized.class) //
  public static class OutOfScope extends AbstractWringTest.OutOfScope.Declaration {
    static String[][] cases = Utils.asArray(//
        new String[] { "Vanilla", "int a; a =3;", }, //
        new String[] { "Not empty else", "int a; if (x) a = 3; else a++;", }, //
        new String[] { "Not plain assignment", "int a = 2; if (b) a +=a+2;", }, //
        new String[] { "Uses later variable", "int a = 2,b = true; if (b) a =3;", }, //
        null);
    /**
     * Generate test cases for this parameterized class.
     *
     * @return a collection of cases, where each case is an array of three
     *         objects, the test case name, the input, and the file.
     */
    @Parameters(name = DESCRIPTION) //
    public static Collection<Object[]> cases() {
      return collect(cases);
    }
    /** Instantiates the enclosing class ({@link OutOfScope}) */
    public OutOfScope() {
      super(WRING);
    }
  }

  @SuppressWarnings({ "javadoc" }) //
  @FixMethodOrder(MethodSorters.NAME_ASCENDING) //
  public class Wringed extends AbstractWringTest<VariableDeclarationFragment> {
    public Wringed() {
      super(WRING);
    }
    @Test public void newlineBug() throws MalformedTreeException, BadLocationException {
      final String from = "int a = 2;\n if (b) a =3;";
      final String expected = "int a = b ? 3 : 2;";
      final Document d = new Document(Wrap.Statement.on(from));
      final CompilationUnit u = (CompilationUnit) As.COMPILIATION_UNIT.ast(d);
      final VariableDeclarationFragment f = Extract.firstVariableDeclarationFragment(u);
      assertThat(f, notNullValue());
      final ASTRewrite r = new Trimmer().createRewrite(u, null);
      final TextEdit e = r.rewriteAST(d, null);
      assertThat(e.getChildrenSize(), greaterThan(0));
      final UndoEdit b = e.apply(d);
      assertThat(b, notNullValue());
      final String peeled = Wrap.Statement.off(d.get());
      if (expected.equals(peeled))
        return;
      if (from.equals(peeled))
        fail("Nothing done on " + from);
      if (compressSpaces(peeled).equals(compressSpaces(from)))
        assertNotEquals("Wringing of " + from + " amounts to mere reformatting", compressSpaces(peeled), compressSpaces(from));
      assertSimilar(expected, peeled);
      assertSimilar(Wrap.Statement.on(expected), d);
    }
    @Test public void nonNullWring() {
      assertNotNull(WRING);
    }
    @Test public void vanilla() throws MalformedTreeException, IllegalArgumentException {
      final String from = "int a = 2; if (b) a =3;";
      final String expected = "int a = b ? 3 : 2;";
      final Document d = new Document(Wrap.Statement.on(from));
      final CompilationUnit u = (CompilationUnit) As.COMPILIATION_UNIT.ast(d);
      final Document actual = TESTUtils.rewrite(new Trimmer(), u, d);
      final String peeled = Wrap.Statement.off(actual.get());
      if (expected.equals(peeled))
        return;
      if (from.equals(peeled))
        fail("Nothing done on " + from);
      if (compressSpaces(peeled).equals(compressSpaces(from)))
        assertNotEquals("Wringing of " + from + " amounts to mere reformatting", compressSpaces(peeled), compressSpaces(from));
      assertSimilar(expected, peeled);
      assertSimilar(Wrap.Statement.on(expected), actual);
    }
  }
}
