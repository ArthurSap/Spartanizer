package il.org.spartan.refactoring.wring;

import static org.eclipse.jdt.core.dom.InfixExpression.Operator.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.refactoring.utils.*;

/**
 * Evaluate the multiplication of integer numbers :
 * <pre>
 * 5-4
 * </pre>
 * to:
 * <pre>
 * 1
 * </pre>
 * @author Dor Ma'ayan 
 * @since 2016
 */
public class EvaluateSubstractionInt extends Wring.ReplaceCurrentNode<InfixExpression> implements Kind.NoImpact {

  @Override public String description() {
      return "Evaluate substraction of int numbers";
  }

  @Override String description(@SuppressWarnings("unused") InfixExpression e) {
    return "Evaluate substraction of int numbers";
  }

  private static boolean isInt(Expression e){
    if(!(e instanceof NumberLiteral))
      return false;
    return ((NumberLiteral) e).getToken().matches("[0-9]+");
  }

  @Override ASTNode replacement(InfixExpression e) {
    return e.getOperator() != MINUS ? null : replacement(extract.allOperands(e),e);
  }
  
  private static ASTNode replacement(final List<Expression> es, InfixExpression e) {
    if(es.isEmpty())
      return null;
    if (!(es.get(0) instanceof NumberLiteral && isInt(es.get(0))))
      return null;
    int sub = Integer.parseInt(((NumberLiteral) (es.get(0))).getToken());
    int index=0;
    for (final Expression ¢ : es){
      if (!(¢ instanceof NumberLiteral && isInt(¢)))
        return null;
      if(index!=0)
        sub = sub - Integer.parseInt(((NumberLiteral) ¢).getToken());
      index++;
    }  
    return e.getAST().newNumberLiteral(Integer.toString(sub));
  }
}