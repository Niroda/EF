package encapsulationofef.lambdaparser;
import static com.trigersoft.jaque.expression.ExpressionType.Equal;
import static com.trigersoft.jaque.expression.ExpressionType.LogicalAnd;
import static com.trigersoft.jaque.expression.ExpressionType.LogicalOr;
import static com.trigersoft.jaque.expression.ExpressionType.LessThanOrEqual;
import static com.trigersoft.jaque.expression.ExpressionType.GreaterThanOrEqual;
import static com.trigersoft.jaque.expression.ExpressionType.GreaterThan;
import static com.trigersoft.jaque.expression.ExpressionType.LessThan;
import com.trigersoft.jaque.expression.*;
import encapsulationofef.helpers.TypeHelper;
import encapsulationofef.helpers.ValidationHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * This code has been created just for fun
 * Doesn't support CHAR and BYTE at all!
 * You can't use BOOLEAN in lambda expression with any operator.
 * BOOLEAN usage: { user -> user.getIsAdmin() OR user -> !user.getIsAdmin() } , this code'll work perfectly
 * BOOLEAN usage: { user -> user.getIsAdmin() == true OR user -> user.getIsAdmin() == false } , this code won't work and will throw an exception
 * @author Ali
 */

public class ConvertToSql implements ExpressionVisitor<StringBuilder> {

    static class stack {

        int top = -1;
        char items[] = new char[100];

        void push(char x) {
            if (top == 99) {
                System.out.println("Stack full");
            } else {
                items[++top] = x;
            }
        }

        char pop() {
            if (top == -1) {
                System.out.println("Underflow error");
                return '\0';
            } else {
                char element = items[top];
                top--;
                return element;
            }
        }

        boolean isEmpty() {
            return (top == -1) ? true : false;
        }
    }

    private StringBuilder sb = new StringBuilder();
    private Expression body;
    private int counter = 0;
    private final String[] supportedStringFunctions = {
        "startswith", "endswith", "length", "contains", "equals"
    };
    private List<String> stringFunctionsIngivenLambda;
    private String usedGeneric;
    private Class<?> mainClass;

    @Override
    public StringBuilder visit(BinaryExpression e) {
        boolean quote = e != body && e.getExpressionType() == LogicalOr;
        if (quote) {
            sb.append('(');
        }
        
        e.getFirst().accept(this);
        sb.append(' ').append(toSqlOp(e.getExpressionType())).append(' ');
        e.getSecond().accept(this);
        if (quote) {
            sb.append(')');
        }
        return sb;
    }

    public static String toSqlOp(int expressionType) {
        switch (expressionType) {
            case LessThanOrEqual:
                return "<=";
            case GreaterThanOrEqual:
                return ">=";
            case LessThan:
                return "<";
            case GreaterThan:
                return ">";
            case Equal:
                return "=";
            case LogicalAnd:
                return "AND";
            case LogicalOr:
                return "OR";
        }
        return ExpressionType.toString(expressionType);
    }

    @Override
    public StringBuilder visit(ConstantExpression e) {
        return sb.append("startSetAsParamLater" + e.getValue() + "endSetAsParamLater");
    }

    @Override
    public StringBuilder visit(InvocationExpression e) {
        return e.getTarget().accept(this);
    }

    @Override
    public StringBuilder visit(LambdaExpression<?> e) {
        // stores the desired class name that has been used to fetch data from the database ..
        this.mainClass = e.getParameters().get(0).getResultType();
        this.usedGeneric = this.mainClass.getSimpleName().toLowerCase();
        this.body = e.getBody();
        return body.accept(this);
    }

    @Override
    public StringBuilder visit(ParameterExpression e) {
        return sb;
    }

    @Override
    public StringBuilder visit(UnaryExpression e) {
        sb.append(ExpressionType.toString(e.getExpressionType()));
        return e.getFirst().accept(this);
    }

    /* Returns true if character1 and character2
       are matching left and right Parenthesis */
    private boolean isMatchingPair(char character1, char character2) {
        if (character1 == '(' && character2 == ')') {
            return true;
        } else if (character1 == '{' && character2 == '}') {
            return true;
        } else if (character1 == '[' && character2 == ']') {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public StringBuilder visit(MemberExpression e) {
        String declaredClass = e.getMember().getDeclaringClass().getSimpleName().toLowerCase();
        String name = e.getMember().getName();
        name = name.replaceAll("^(get|is)", "").toLowerCase();
        String toAppend = "";
        Class<?> checkPremetives = null;
        try {
            if(!e.getMember().getDeclaringClass().toString().contains("class java.lang.String"))
                checkPremetives = e.getMember().getDeclaringClass().getMethod(e.getMember().getName(), null).getReturnType();
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ConvertToSql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ConvertToSql.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(checkPremetives != null && checkPremetives.isPrimitive() && (checkPremetives == Boolean.TYPE)) {
            String boolProperty = sb.toString().trim();
            if(boolProperty.endsWith("!")) {
                boolProperty = sb.replace(sb.lastIndexOf("!"), sb.lastIndexOf("!")+1, "").toString();
                boolProperty += "`" + declaredClass + "s`.`" + name + "` IS NULL";
            } else {
                boolProperty += "`" + declaredClass + "s`.`" + name + "` = ''";
            }
            sb = new StringBuilder(boolProperty);
        } else if (!declaredClass.equals("string")) {
            String _tempColumnName = e.toString().split("\\.")[1];
            _tempColumnName = _tempColumnName.replaceAll("get|set|\\(|\\)", "").toLowerCase();
            toAppend
                    = (this.usedGeneric.equals(declaredClass))
                    ? "`" + this.usedGeneric + "s`.`" + name + "`"
                    : "`" + this.usedGeneric + "s`.`" + _tempColumnName 
                    + "_id` in (SELECT `" + name + "` FROM `" + declaredClass 
                    + "s` WHERE `" + declaredClass + "s`.`" + name + "`";
        } else {
            String _tempColumnName = e.toString().split("\\.")[1].replaceAll("\\(|\\)", toAppend);
            String columnName = _tempColumnName.replaceAll("(^(get|is))", "").toLowerCase();
            if (ValidationHelper.isValidSetterOrGetter(_tempColumnName, this.mainClass)) {
                String _selectFromRelationalClass = "";
                if (ValidationHelper.isOneToMany(_tempColumnName, this.mainClass)) {
                    String _columnInRelationalClass = 
                            e.toString().split(_tempColumnName)
                                    [1].split("\\.")
                                    [1].replaceAll("(^(get|is))|\\(|\\)", "")
                                    .toLowerCase();
                    Object _relationalClass = TypeHelper.createNewInstanceByRelationalProperty(_tempColumnName, this.mainClass);
                    String _relationalClassName = _relationalClass.getClass().getSimpleName().toLowerCase();
                    _selectFromRelationalClass = "`" + _relationalClassName
                            + "s`.`" + _columnInRelationalClass + "`";
                }
                _selectFromRelationalClass = _selectFromRelationalClass.length() < 2 
                        ? " `" + this.usedGeneric + "s`.`" + columnName + "` " : _selectFromRelationalClass;
                if (name.equals("length")) {
                    toAppend = "LENGTH("+_selectFromRelationalClass+")";
                } else {
                    toAppend = _selectFromRelationalClass + manipulateStringFunctions(name);
                }
                if (!areParenthesisBalanced(toAppend.toCharArray())) {
                    toAppend += ")";
                }
            }
        }
        return sb.append(toAppend);
    }

    private void generateStringFunctionsFromLambdaExpression() {
        this.stringFunctionsIngivenLambda = new ArrayList();
        String[] expressions = this.body.toString().split("\\|\\||\\&\\&"); // convert lambda expression body to an array
        for (int i = 0; i < expressions.length; i++) {
            for (int j = 0; j < this.supportedStringFunctions.length; j++) {
                String _tempExp = expressions[i].toLowerCase();
                if (_tempExp.contains(this.supportedStringFunctions[j])) {
                    String val = _tempExp
                            .split(this.supportedStringFunctions[j])[1]
                            .replaceAll("[^A-Za-z0-9]", "");
                    this.stringFunctionsIngivenLambda.add("startSetAsParamLater" + val.trim() + "endSetAsParamLater");
                    break;
                }
            }
        }
    }

    private String manipulateStringFunctions(String lambdaBody) {
        // supported string functions: "startswith", "endswith", "length", "comparetoignorecase", "contains", "equals" 
        if(this.stringFunctionsIngivenLambda == null) {
            generateStringFunctionsFromLambdaExpression();
        }
        String toAppend = "";
        switch (lambdaBody) {
            case "startswith":
                toAppend = " LIKE " + this.stringFunctionsIngivenLambda.get(this.counter) + "endLikeFunc ";
                break;
            case "endswith":
                toAppend = " LIKE startLikeFunc" + this.stringFunctionsIngivenLambda.get(this.counter) + " ";
                break;
            case "contains":
                toAppend = " LIKE startLikeFunc" + this.stringFunctionsIngivenLambda.get(this.counter) + "endLikeFunc ";
                break;
            case "equals":
                toAppend = " = " + this.stringFunctionsIngivenLambda.get(this.counter) + " ";
                break;
            default:
                throw new IllegalArgumentException(lambdaBody + " isn't a valid function!");
        }
        this.counter++;
        return toAppend;
    }

    /* Return true if expression has balanced 
       Parenthesis */
    public boolean areParenthesisBalanced(char exp[]) {
        /* Declare an empty character stack */
        stack st = new stack();

        /* Traverse the given expression to 
          check matching parenthesis */
        for (int i = 0; i < exp.length; i++) {

            /*If the exp[i] is a starting 
            parenthesis then push it*/
            if (exp[i] == '{' || exp[i] == '(' || exp[i] == '[') {
                st.push(exp[i]);
            }

            /* If exp[i] is an ending parenthesis 
             then pop from stack and check if the 
             popped parenthesis is a matching pair*/
            if (exp[i] == '}' || exp[i] == ')' || exp[i] == ']') {

                /* If we see an ending parenthesis without 
                 a pair then return false*/
                if (st.isEmpty()) {
                    return false;
                } /* Pop the top element from stack, if 
                it is not a pair parenthesis of character 
                then there is a mismatch. This happens for 
                expressions like {(}) */ else if (!isMatchingPair(st.pop(), exp[i])) {
                    return false;
                }
            }

        }

        /* If there is something left in expression 
          then there is a starting parenthesis without 
          a closing parenthesis */
        if (st.isEmpty()) {
            return true;
            /*balanced*/
        } else {
            /*not balanced*/
            return false;
        }
    }
}
