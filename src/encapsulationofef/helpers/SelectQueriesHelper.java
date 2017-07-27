/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encapsulationofef.helpers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ali
 */
public class SelectQueriesHelper {

    /**
     * Getting a list of accessible methods for a given class via refelction
     *
     * @param <T> is desired class to get its methods
     * @param type Example: fetchClassMethods(User.class);
     * @return a list of declared methods for given class
     */
    public static <T> Method[] fetchClassMethods(Class<?> type) {
        Method[] _methods = null;
        T _t;
        try {
            _t = (T) type.newInstance();
            _methods = _t.getClass().getDeclaredMethods();
        } catch (InstantiationException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return _methods;
    }

    /**
     * Checking if passed method name in given class type either is
     * primitive/predefined or a class lives in Models package Models package is
     * just arbitrary package name that contains your classes
     *
     * @param method setter or getter method as string, Example: getId
     * @param type Example: isRelationalProperty("getId", User.class);
     * @return false if it's primitive/String or any class lives outside Models
     * package, otherwise true
     */
    public static boolean isRelationalProperty(String method, Class<?> type) {
        try {
            method = method.startsWith("set") ? method.replace("set", "get") : (method.startsWith("get") ? method : "get" + method);
            Method getMethodName = type.getDeclaredMethod(method, null);
            Method _doubleCheckSetMethod = type.getDeclaredMethod(method.replace("get", "set"), getMethodName.getReturnType());
            return _doubleCheckSetMethod != null
                    && !getMethodName.getReturnType().toString().contains("class java.lang.String")
                    && !getMethodName.getReturnType().isPrimitive();
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Fetching desired class and creating a LEFT JOIN SQL statement in case
     * that class has a field of another type
     *
     * @param <T> is desired class to fetch its fields
     * @param type Example: checkRelationship(User.class);
     * @return SQL "LEFT JOIN" statement that can be used with any "SELECT"
     * statement
     */
    private static <T> String checkRelationship(Class<?> type) {
        String mainClass = type.getSimpleName().toLowerCase();
        Method[] _methods = fetchClassMethods(type);
        String sqlQuery = "";
        for (Method m : _methods) {
            Package packageName = (m.getReturnType()).getPackage();
            if (packageName != null
                    && packageName.toString().equals(type.getPackage().toString())
                    && !m.getReturnType().isPrimitive()) {
                String tableName = (m.getReturnType()).getSimpleName().toLowerCase();
                sqlQuery += " LEFT JOIN `" + tableName + "s` ON `"
                        + mainClass
                        + "s`.`" + m.getName().replaceAll("get", "").toLowerCase()
                        + "_id` = `" + tableName + "s`.`id` ";
            }
        }
        return sqlQuery;
    }

    /**
     * Checking if passed method name in given class type either is one-to-many
     * or many-to-many
     *
     * @param <T> is desired class to check
     * @param method setter or getter method as string, Example: getLocations
     * @param type Example: isOneToManyOrManyToMany("getLocations", User.class);
     * @return true if return type of the method is either List or Array,
     * otherwise false
     */
    public static <T> boolean isOneToManyOrManyToMany(String method, Class<?> type) {
        try {
            method = method.startsWith("set") ? method.replace("set", "get") : (method.startsWith("get") ? method : "get" + method);
            Method getMethodName = type.getDeclaredMethod(method, null);
            String returnType = getMethodName.getReturnType().toString();
            return returnType.contains("interface java.util.List") || returnType.contains("class [L");
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * THIS METHOD WILL BE DELETED LATER. IT'S MESSY CODE, WHEN I STARTED I NEEDED SOMETHING LIKE START POINT TO START WITH SO I CREATED THIS ONE
     * IT'S USED NOW IN SELECT STATEMENT TO CHECK IF GIVEN FIELD HAS ONE TO MANY RELATIONSHIP OR NOT.
     */
    private static <T> boolean isOneToMany(String method, Class<?> type) {
        try {
            Method _method = type.getDeclaredMethod(method);
            String _relationalPackageName = type.getPackage().getName();
            String[] _relationalClassNameArr = _method.getGenericReturnType().getTypeName().split(_relationalPackageName + ".");
            String relationalClass = _relationalClassNameArr[_relationalClassNameArr.length - 1].replaceAll("\\.|\\[|\\]|\\>|\\<", "");
            Method[] targetMethods = Class.forName(_relationalPackageName + "." + relationalClass).getDeclaredMethods();
            for (int i = 0; i < targetMethods.length; i++) {
                if (targetMethods[i].getName().startsWith("get")) {
                    Method m = targetMethods[i];
                    if (m.getGenericReturnType().getTypeName().contains(type.getSimpleName())
                            && (m.getGenericReturnType().getTypeName().contains("[]")
                            || m.getGenericReturnType().getTypeName().contains("java.util.List<"))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * To mirror database table and checking if given class matchs that table.
     * Table name is type name to lower case + s
     *
     * @param <T> is desired class to mirror
     * @param type Example: checkIfClassMatchsTable(User.class, tableColumns);
     * @param tableColumns Where "tableColumns" equals Array of column names in
     * table name "users"
     * @return true if "tableColumns" names match fields in given class,
     * otherwise false
     */
    public static <T> boolean checkIfClassMatchsTable(Class<?> type, String[] tableColumns) {
        Method[] _tempMethods = fetchClassMethods(type);
        if (tableColumns == null && _tempMethods == null) {
            throw new NullPointerException("Either array of columns is null or selected class has no public method!");
        }
        List<String> _methods = new ArrayList();
        for (int i = 0; i < _tempMethods.length; i++) {
            if (_tempMethods[i].getName().startsWith("get")) {
                Package packageName = (_tempMethods[i].getReturnType()).getPackage();
                if (packageName != null
                        && packageName.toString().equals(type.getPackage().toString())
                        && !_tempMethods[i].getReturnType().isPrimitive()) {
                    _methods.add(_tempMethods[i].getName().replaceAll("get", "").toLowerCase() + "_id");
                } else {
                    _methods.add(_tempMethods[i].getName().replaceAll("get", "").toLowerCase());
                }
            }
        }
        if (tableColumns.length != _methods.size()) {
            return false;
        }
        for (int i = 0; i < tableColumns.length; i++) {
            if (!_methods.contains(tableColumns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method is used to create SELECT SQL statement
     *
     * @param <T> is desired class
     * @param type Example: selectAllStatement(User.class);
     * @return SQL SELECT statement with JOIN statement if needed
     */
    public static <T> String selectStatement(Class<?> type, List<String> toInclude, String where, String order, String limit, String offset) {
        String className = type.getSimpleName().toLowerCase();
        String sqlQuery = "SELECT ";
        List<String> getters = new ArrayList();//" FROM `" + className + "s`"
        List<String> setters = new ArrayList();
        Method[] methods = type.getDeclaredMethods();
        for (Method m : methods) {
            if (isGetter(m)) {
                getters.add(m.getName());
            }
            if (isSetter(m)) {
                setters.add(m.getName());
            }
        }
        int index = getters.size();
        for (int i = 0; i < index; i++) {
            String fieldName = getters.get(i).replace("get", "set");
            if (setters.contains(fieldName)) {
                if (!isOneToManyOrManyToMany(fieldName, type)) {
                    sqlQuery
                            += isRelationalProperty(fieldName, type) && toInclude != null
                            ? "`" + className + "s`.`" + fieldName.replace("set", "").toLowerCase()
                            + "_id` AS " + className + "s_" + fieldName.replace("set", "").toLowerCase()
                            + ", " + getRelationalColumnNames(fieldName, type)
                            : (!isRelationalProperty(fieldName, type)
                            ? "`" + className + "s`.`" + fieldName.replace("set", "").toLowerCase()
                            + "` AS " + className + "s_" + fieldName.replace("set", "").toLowerCase()
                            : "")
                            + (i < index - 1 ? ", " : " ");
                }
            }
        }
        sqlQuery = sqlQuery.trim();
        sqlQuery = sqlQuery.endsWith(",") ? sqlQuery.substring(0, sqlQuery.length() - 1) : sqlQuery;
        if (toInclude != null) {
            String _joinStatement = "";
            for (int i = 0; i < toInclude.size(); i++) {
                try {
                    Type includeType = type.getDeclaredMethod("get" + toInclude.get(i), null).getGenericReturnType();
                    String _relationalPackageName = type.getPackage().getName();
                    String[] _relationalClassNameArr = includeType.getTypeName().split(_relationalPackageName + ".");
                    String _targetClassName = _relationalClassNameArr[_relationalClassNameArr.length - 1].replaceAll("\\.|\\[|\\]|\\>|\\<", "");
                    String relationalClass = _targetClassName.toLowerCase();
                    Class<?> _targetClassType = Class.forName(type.getPackage().getName() + "." + _targetClassName);
                    List<Method> _targetMethods = TypeHelper.getGetterMethods(_targetClassType, false);

                    if (includeType.getTypeName().startsWith("java.util.List<")
                            && isOneToMany("get" + toInclude.get(i), type)) {
                        String _groupConcat = "GROUP_CONCAT(DISTINCT";
                        for (int j = 0; j < _targetMethods.size(); j++) {
                            if (_targetMethods.get(j).getReturnType().isPrimitive()
                                    || _targetMethods.get(j).getReturnType().getName().contains("java.lang.String")) {
                                _groupConcat += " `" + relationalClass + "s`.`"
                                        + _targetMethods.get(j).getName().replaceAll("get|set", "").toLowerCase()
                                        + "`, '[START_" + _targetMethods.get(j).getName().replaceAll("get", "") + "_END]',";
                            }
                        }
                        _groupConcat = _groupConcat.substring(0, _groupConcat.length() - 1)
                                + " SEPARATOR '[START_ROWSEPERATOR_END]') AS " + relationalClass + "s_values, ";
                        sqlQuery = sqlQuery.replace("SELECT", "SELECT " + _groupConcat);
                        _joinStatement += " JOIN `" + relationalClass + "s` ON `"
                                + relationalClass + "s`.`"
                                + TypeHelper.getForeignKey(_targetClassName, type) + "_id`"
                                + " = `" + type.getSimpleName().toLowerCase() + "s`.`id`";
                    } else if (ValidationHelper.isManyToMany(toInclude.get(i), type)
                            || ValidationHelper.isManyToOne(toInclude.get(i), type)) {
                        String _groupConcat = "GROUP_CONCAT(DISTINCT";
                        for (int j = 0; j < _targetMethods.size(); j++) {
                            if (_targetMethods.get(j).getReturnType().isPrimitive()
                                    || _targetMethods.get(j).getReturnType().getName().contains("java.lang.String")) {
                                boolean _checkingBoolean = _targetMethods.get(j).getReturnType().getName().contains("boolean");
                                if(_checkingBoolean)
                                    _groupConcat += " CASE WHEN `" + relationalClass + "s`.`"
                                            + _targetMethods.get(j).getName().replaceAll("get|set", "").toLowerCase()
                                            + "`='' THEN 'true' ELSE 'false' END"
                                            + ", '[START_" + _targetMethods.get(j).getName().replaceAll("get", "") + "_END]',";
                                else
                                    _groupConcat += " `" + relationalClass + "s`.`"
                                            + _targetMethods.get(j).getName().replaceAll("get|set", "").toLowerCase()
                                            + "`, '[START_" + _targetMethods.get(j).getName().replaceAll("get", "") + "_END]',";
                            }
                        }
                        _groupConcat = _groupConcat.substring(0, _groupConcat.length() - 1)
                                + " SEPARATOR '[START_ROWSEPERATOR_END]') AS " + relationalClass + "s_values, ";
                        String associationTable = className.compareTo(relationalClass) < 0
                                ? className + "s_" + relationalClass + "s"
                                : relationalClass + "s_" + className + "s";
                        sqlQuery = sqlQuery.replace("SELECT", "SELECT " + _groupConcat);
                        _joinStatement += " LEFT OUTER JOIN `" + associationTable + "` ON `"
                                + associationTable + "`.`" + className + "_id`"
                                + " = `" + className + "s`.`id`"
                                + " LEFT OUTER JOIN `" + relationalClass + "s` ON `"
                                + relationalClass + "s`.`id` = `"
                                + associationTable + "`.`" + relationalClass + "_id`";
                    }
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            _joinStatement = _joinStatement.trim().endsWith(",") ? 
                    _joinStatement.trim().substring(0, _joinStatement.trim().length() - 1) + " " : _joinStatement;
            sqlQuery += " FROM `" + className + "s`" + (toInclude != null ? checkRelationship(type) : "");
            sqlQuery += _joinStatement + " " + (where != null ? where : "") + " GROUP BY `" + className + "s`.`id` " + (_joinStatement.length() > 1
                    ? (order != null && order.length() > 0 ? order : " ORDER BY `" + className + "s`.`id`")
                    : (order != null && order.length() > 0 ? order : ""));
        } else {
            sqlQuery = sqlQuery.trim().endsWith(",") ? 
                    sqlQuery.trim().substring(0, sqlQuery.trim().length() - 1) + " " : sqlQuery;
            sqlQuery += " FROM `" + className + "s`" + (where != null ? where : "");
        }
        if (limit != null && limit.length() > 3) {
            sqlQuery += limit + (offset != null && offset.length() > 3 ? offset : "");
        }
        return sqlQuery;
    }

    /**
     * This method is used to get all column names from relational table and
     * creates a SQL statement to be used in selectStatement method
     *
     * @param <T> is desired class to fetch
     * @param field is getter or setter for the field name of another type in
     * given class
     * @param type Example: getRelationalColumnNames("setCourse", User.class);
     * @return column names in the relational column as SQL
     */
    private static <T> String getRelationalColumnNames(String field, Class<?> type) {
        // TODO - THIS METHOD IS NOT EFFICIENT, needs to be edited later ..
        String sqlQuery = "";
        T className;
        Class<?> nestedType;
        try {
            field = field.startsWith("set") ? field.replace("set", "get") : (field.startsWith("get") ? field : "get" + field);
            nestedType = type.getDeclaredMethod(field).getReturnType();
            className = (T) (type.getDeclaredMethod(field).getReturnType()).newInstance();
            String tableName = className.getClass().getSimpleName();
            Method[] nestedClassMethods = className.getClass().getDeclaredMethods();
            List<String> getters = new ArrayList();
            List<String> setters = new ArrayList();
            for (Method m : nestedClassMethods) {
                if (isGetter(m)) {
                    getters.add(m.getName());
                }
                if (isSetter(m)) {
                    setters.add(m.getName());
                }
            }
            int index = getters.size();
            for (int i = 0; i < index; i++) {
                String fieldName = getters.get(i).replace("get", "set");
                if (setters.contains(fieldName)) {

                    sqlQuery += (!isRelationalProperty(fieldName, nestedType)
                            ? ("`" + tableName.toLowerCase() + "s`.`" + fieldName.replace("set", "").toLowerCase()
                            + "` AS " + tableName.toLowerCase() + "s_" + fieldName.replace("set", "").toLowerCase()
                            + (i < index - 1 ? ", " : " ")) : "");
                }
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sqlQuery;
    }

    /**
     * Checking either given method is getter or not
     *
     * @param method desired method to check
     * @return true if it's getter, otherwise false
     */
    private static boolean isGetter(Method method) {
        if (Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0) {
            if (method.getName().matches("^get[A-Z].*")
                    && !method.getReturnType().equals(void.class)) {
                return true;
            }
            if (method.getName().matches("^is[A-Z].*")
                    && method.getReturnType().equals(boolean.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checking either given method is setter or not
     *
     * @param method desired method to check
     * @return true if it's setter, otherwise false
     */
    private static boolean isSetter(Method method) {
        return Modifier.isPublic(method.getModifiers())
                && method.getReturnType().equals(void.class)
                && method.getParameterTypes().length == 1
                && method.getName().matches("^set[A-Z].*");
    }
}
