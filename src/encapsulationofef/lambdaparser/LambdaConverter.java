package encapsulationofef.lambdaparser;

import static java.nio.file.Files.createTempDirectory;
import java.io.IOException;
import java.util.function.Predicate;
import com.trigersoft.jaque.expression.LambdaExpression;
import encapsulationofef.helpers.ValidationHelper;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LambdaConverter {

    private static final String DUMP_CLASSES_PROP = "jdk.internal.lambda.dumpProxyClasses";

    public static void loader() {
        try {
            if (System.getProperty(DUMP_CLASSES_PROP) == null) {
                System.setProperty(DUMP_CLASSES_PROP, createTempDirectory("lambda").toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> String convertPredicateToSql(Predicate<T> predicate) {
        LambdaExpression<Predicate<T>> lambdaExpression = LambdaExpression.parse(predicate);
        ConvertToSql csql = new ConvertToSql();
        String _result = lambdaExpression.accept(csql).toString();
        if (!csql.areParenthesisBalanced(_result.toCharArray())) {
            _result += ")";
        }
        return _result;
    }

    public static <T> String convertFuncToSqlForInclusion(Function<T, Object> func) {
        LambdaExpression<Function<T, Object>> lambda = LambdaExpression.parse(func);
        String _resultType = lambda.getBody().getResultType().toString();
        if (ValidationHelper.isPrimitiveFunctional(lambda.getBody().toString())
                || _resultType.contains("java.lang.String")) {
            return "ERROR";
        }
        return lambda.getBody().toString().replaceAll("([^\\n\\r]*)(get|set)", "").replaceAll("(\\(\\))", "");
    }

    public static <T> String convertFuncToSqlForOrdering(Function<T, Object> func) {
        LambdaExpression<Function<T, Object>> lambda = LambdaExpression.parse(func);
        if(!ValidationHelper.isPrimitiveFunctional(lambda.getBody().getResultType().toString()))
            return "ERROR";
        String _memberType;
        if (lambda.getBody().toString().split("\\.").length > 2) {
            _memberType = lambda.getBody().toString().split("\\)")[1].split("\\.")[1].replaceAll("\\(", "");
        } else {
            _memberType = lambda.getBody().toString().split("\\)")[0].split("\\.")[1].replaceAll("\\(", "");
        }
        Class<?> _mainClass = lambda.getParameters().get(0).getResultType();
        if (!ValidationHelper.isValidSetterOrGetter(_memberType, _mainClass)) {
            return "ERROR";
        }
        String _relationalField = lambda.getBody().toString().replaceAll("([^\\n\\r]*)(get|set)", "").replaceAll("(\\(\\))", "").toLowerCase();
        if (ValidationHelper.isOneToMany(_memberType, _mainClass)) {
            try {
                Class<?> _relationalClass = _mainClass.getDeclaredMethod(_memberType, null).getReturnType();
                return " ORDER BY `" + _relationalClass.getSimpleName().toLowerCase() + "s`.`" + _relationalField + "`";
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(LambdaConverter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(LambdaConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            return " ORDER BY `" + _mainClass.getSimpleName().toLowerCase() + "s`.`" + _relationalField + "`";
        }
        return null;
    }
}
