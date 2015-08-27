package org.spartan.refactoring.utils;

import static org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_AND;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_OR;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER_EQUALS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.spartan.hamcrest.CoreMatchers.is;
import static org.spartan.hamcrest.MatcherAssert.assertThat;
import static org.spartan.refactoring.utils.ExpressionComparator.countNonWhites;
import static org.spartan.refactoring.utils.Funcs.asComparison;
import static org.spartan.refactoring.utils.Funcs.right;
import static org.spartan.refactoring.utils.Into.e;
import static org.spartan.refactoring.utils.Into.i;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * A test suite for class {@link Funcs}
 *
 * @author Yossi Gil
 * @since 2015-07-18
 * @see Funcs
 */
@SuppressWarnings({ "static-method", "javadoc" }) //
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
public class FuncsTest {
  @Test public void chainComparison() {
    final InfixExpression e = i("a == true == b == c");
    assertEquals("c", right(e).toString());
  }
  @Test public void asComparisonPrefixlExpression() {
    final PrefixExpression p = mock(PrefixExpression.class);
    doReturn(PrefixExpression.Operator.NOT).when(p).getOperator();
    assertNull(asComparison(p));
  }
  @Test public void asComparisonTypicalExpression() {
    final InfixExpression i = mock(InfixExpression.class);
    doReturn(GREATER).when(i).getOperator();
    assertNotNull(asComparison(i));
  }
  @Test public void asComparisonTypicalExpressionFalse() {
    final InfixExpression i = mock(InfixExpression.class);
    doReturn(CONDITIONAL_OR).when(i).getOperator();
    assertNull(asComparison(i));
  }
  @Test public void asComparisonTypicalInfixFalse() {
    final InfixExpression i = mock(InfixExpression.class);
    doReturn(CONDITIONAL_AND).when(i).getOperator();
    assertNull(asComparison(i));
  }
  @Test public void asComparisonTypicalInfixIsCorrect() {
    final InfixExpression i = mock(InfixExpression.class);
    doReturn(GREATER).when(i).getOperator();
    assertEquals(i, asComparison(i));
  }
  @Test public void asComparisonTypicalInfixIsNotNull() {
    final InfixExpression e = mock(InfixExpression.class);
    doReturn(GREATER).when(e).getOperator();
    assertNotNull(asComparison(e));
  }
  @Test public void countNonWhiteCharacters() {
    assertThat(countNonWhites(e("1 + 23     *456 + \n /* aa */ 7890")), is(13));
  }
  @Test public void isDeMorganAND() {
    assertTrue(Is.deMorgan(CONDITIONAL_AND));
  }
  @Test public void isDeMorganGreater() {
    assertFalse(Is.deMorgan(GREATER));
  }
  @Test public void isDeMorganGreaterEuals() {
    assertFalse(Is.deMorgan(GREATER_EQUALS));
  }
  @Test public void isDeMorganOR() {
    assertTrue(Is.deMorgan(CONDITIONAL_OR));
  }
  @Test public void sameOfTwoExpressionsIdentical() {
    assertTrue(Funcs.same(e("a+b"), e("a+b")));
  }
  @Test public void sameOfTwoExpressionsNotSame() {
    assertFalse(Funcs.same(e("a+b+c"), e("a+b")));
  }
  @Test public void sameOfNulls() {
    final ASTNode n1 = null;
    final ASTNode n2 = null;
    assertTrue(Funcs.same(n1, n2));
  }
  @Test public void sameOfNullAndSomething() {
    final ASTNode n1 = null;
    final ASTNode n2 = e("a");
    assertFalse(Funcs.same(n1, n2));
  }
  @Test public void sameOfSomethingAndNull() {
    final ASTNode n1 = e("a");
    final ASTNode n2 = null;
    assertFalse(Funcs.same(n1, n2));
  }
}
