package encapsulationofef;

import encapsulationofef.helpers.*;
import encapsulationofef.lambdaparser.LambdaConverter;
import java.lang.reflect.Field;
import java.sql.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.UnsupportedDataTypeException;

/**
 * A generic class used to deal with the type T 
 * where T is a model represents a table in the database
 * @author Ali
 * @param <T> 
 */
public class DbSet<T> {

    private final Class<T> type; // stores class type of passed generic
    private final String tableName; // table name which equals class name + s
    private PreparedStatement stm; // stores SQL Query statement
    private Connection con; // stores connection to SQL server
    private ResultSet rs = null;
    private List<Method> _methods = null; // stores getter and setters method in given generic class
    private List<String> toInclude = null;
    private String order = "";
    private String limit = null;
    private String offset = null;
    private String sqlQuery = "";

    /**
     * Each instance of T represents each individual table in the database where
     * each table is a class
     *
     * @param type is class type, Example: Person.class
     * @param connection is the connection to the database provided in DbContext
     */
    public DbSet(Class<T> type, Connection connection) {
        this.type = type;
        // sets table name where table name is class name in lower case + s
        this.tableName = this.type.getSimpleName().toLowerCase() + "s";
        // check if connection is not null
        if (connection == null) {
            return;
        }
        this.con = connection;
    }

    public DbSet<T> include(Function<T, Object> desiredTable) {
        if (this.con == null) {
            return null;
        }
        String _tempSqlQuery = LambdaConverter.convertFuncToSqlForInclusion(desiredTable);
        if (!_tempSqlQuery.equals("ERROR")) {
            if (this.toInclude == null) {
                this.toInclude = new ArrayList();
            }
            this.toInclude.add(_tempSqlQuery);
        } else {
            this.toInclude = null;
        }
        return this;
    }

    /**
     * This method collects all rows from current table ..
     *
     * @return List<T>
     */
    public List<T> getItems() {
        if (this.con == null) {
            return null;
        }
        List<T> result = new ArrayList();
        try {
            this.stm = con.prepareStatement(SelectQueriesHelper.selectStatement(type, this.toInclude, null, this.order, this.limit, this.offset));
            this.rs = stm.executeQuery();
            result = _fetchTheResults(this.rs);
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (this.rs != null && !this.rs.isClosed()) {
                    this.rs.close();
                }
                if (this.stm != null && !this.stm.isClosed()) {
                    this.stm.close();
                }
                if (this.con != null && !this.con.isClosed()) {
                    this.con.close();
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    /**
     * Where function is used to add conditions to SQL QUERIES
     * @param pre : desired condition
     *     Example: db.getUsers().where(u -> u.getName().contains("a"));
     * @return List<T>
     */
    public List<T> where(Predicate<T> pre) {
        if (this.con == null) {
            return null;
        }
        List<T> _tempArr = new ArrayList();
        String _tempSqlQuery = LambdaConverter.convertPredicateToSql(pre);
        String regex = "startSetAsParamLater([\\s\\S]*?)endSetAsParamLater";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(_tempSqlQuery);
        List<String> matchs = new ArrayList<String>();
        while (matcher.find()) {
            String toBeReplaced = matcher.group(0);
            for (int i = 1; i <= matcher.groupCount(); i++) {
                matchs.add(matcher.group(i));
            }
            _tempSqlQuery = _tempSqlQuery.replaceAll(toBeReplaced, "?");
        }

        try {
            String _sqlQuery = SelectQueriesHelper.selectStatement(type, this.toInclude, " WHERE " + _tempSqlQuery, this.order, this.limit, this.offset);
            this.stm = this.con.prepareStatement(_sqlQuery);
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = 1; i <= matchs.size(); i++) {
            try {
                this.stm.setString(i, matchs.get(i - 1));
            } catch (SQLException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        _tempSqlQuery = this.stm.toString();
        if (_tempSqlQuery.contains("endLikeFunc") || _tempSqlQuery.contains("startLikeFunc")) {
            _tempSqlQuery = _tempSqlQuery.substring(_tempSqlQuery.indexOf(": ") + 2, _tempSqlQuery.length());
            _tempSqlQuery = _tempSqlQuery.replaceAll("'endLikeFunc", "%'").replaceAll("startLikeFunc'", "'%");
            try {
                this.stm = this.con.prepareStatement(_tempSqlQuery);
            } catch (SQLException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            this.rs = this.stm.executeQuery();
            _tempArr = this._fetchTheResults(this.rs);
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (this.rs != null && !this.rs.isClosed()) {
                    this.rs.close();
                }
                if (this.stm != null && !this.stm.isClosed()) {
                    this.stm.close();
                }
                if (this.con != null && !this.con.isClosed()) {
                    this.con.close();
                }
            } catch (Exception ex) {
            }
        }
        return _tempArr;
    }

    /**
     * This method is used to create a special SQL Query
     * @param sql : desired SQL Query
     *     Example: SELECT * FROM `users` where `id`=? ...
     * @param values values to be used in PreparedStatement
     * @return  ResultSet
     * @deprecated  use it on your own risk!
     */
    @Deprecated
    public ResultSet sqlQuery(String sql, Object[] values) {
        if (this.con == null) {
            return null;
        }
        try {
            this.stm = this.con.prepareStatement(sql);
            for (int i = 1; i <= values.length; i++) {
                this.stm.setString(i, values[i - 1].toString());
            }
            this.rs = this.stm.executeQuery();
            return rs;
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (this.rs != null && !this.rs.isClosed()) {
                    this.rs.close();
                }
                if (this.stm != null && !this.stm.isClosed()) {
                    this.stm.close();
                }
                if (this.con != null && !this.con.isClosed()) {
                    this.con.close();
                }
            } catch (Exception ex) {
            }
        }
        return null;
    }

    /**
     * Used to set a limit on the created SQL Query
     * @param count: how many rows you want
     *      Example: db.getUsers().limit(2).getItems();
     * @return:  current object of DbSet<T> to allow you to use another method
     */
    public DbSet<T> limit(int count) {
        if (this.con == null) {
            return null;
        }
        this.limit = " LIMIT " + count;
        return this;
    }

    /**
     * Used to skip rows
     * @param count: how many rows you want to skip
     *      Example: db.getUsers().skip(3).where(u -> u.getId() > 10 && u.getId() < 20);
     * @return: current object of DbSet<T> to allow you to use another method
     */
    public DbSet<T> skip(int count) {
        if (this.con == null) {
            return null;
        }
        if (this.limit == null || this.limit.length() < 1) {
            this.limit = " LIMIT 18446744073709551615 ";
        }
        this.offset = " OFFSET " + count;
        return this;
    }

    /**
     * Used to get first match if there is any result otherwise null
     * @param predicate: condition to be added to the SQL Query
     *         Example : db.getUsers().first(x -> x.getName().length() > 7);
     * @return first match or null
     */
    public T first(Predicate<T> predicate) {
        if (this.con == null) {
            return null;
        }
        this.limit = " LIMIT 1 ";
        List<T> _result = this.where(predicate);
        return _result != null && _result.size() > 0 ? _result.get(0) : null;
    }

    /**
     * to order by ascending
     * @param desiredField: order by desired field, if you use a field that returns object or list this will be ignored!
     *             Example: db.getUsers().orderByAscending(u -> u.getIsAdmin()).getItems();
     * @return current object of DbSet<T> to allow you to use another method
     */
    public DbSet<T> orderByAscending(Function<T, Object> desiredField) {
        if (this.con == null) {
            return null;
        }
        String _tempSqlQuery = LambdaConverter.convertFuncToSqlForOrdering(desiredField);
        if (_tempSqlQuery != null && !_tempSqlQuery.equals("ERROR")) {
            this.order = _tempSqlQuery + " ASC";
        }
        return this;
    }

    /**
     * to order by descending
     * @param desiredField: order by desired field, if you use a field that returns object or list this will be ignored!
     *             Example: db.getUsers().orderByDescending(u -> u.getId()).getItems();
     * @return current object of DbSet<T> to allow you to use another method
     */
    public DbSet<T> orderByDescending(Function<T, Object> desiredField) {
        if (this.con == null) {
            return null;
        }
        String _tempSqlQuery = LambdaConverter.convertFuncToSqlForOrdering(desiredField);
        if (_tempSqlQuery != null && !_tempSqlQuery.equals("ERROR")) {
            this.order = _tempSqlQuery + " DESC";
        }
        return this;
    }

    /**
     * Used to add new row to the database
     * @param item: is new object of type T 
     * @return  number of affected rows
     * @throws SQLException 
     */
    public int add(T item) throws SQLException {
        if (this.con == null) {
            return 0;
        }
        int affected = 0;
        try {
            this._methods = TypeHelper.getGetterMethods(this.type, false);
            this.sqlQuery += "INSERT INTO `" + this.tableName + "`";
            String values = "VALUES(";
            String _manyToOneInsert = "";
            String _manyToManyInsert = "";
            for (int i = 0; i < _methods.size(); i++) {
                if (this._methods.get(i).getName().equals("getId")) {
                    continue;
                }
                Object _tempVal = this._methods.get(i).invoke(item, null);
                String columnName = this._methods.get(i).getName().replace("get", "").toLowerCase();
                boolean isOneToMany = ValidationHelper.isOneToMany(this._methods.get(i).getName(), type);
                boolean isManyToOne = ValidationHelper.isManyToOne(this._methods.get(i).getName(), type);
                boolean isManyToMany = ValidationHelper.isManyToMany(this._methods.get(i).getName(), type);
                this.sqlQuery += (i == 0 ? " (" : "");
                if (!isOneToMany && !isManyToMany && !isManyToOne) {
                    this.sqlQuery += "`" + columnName + "`,";
                    values += _tempVal instanceof Boolean ? Boolean.valueOf(_tempVal.toString()) ? "''," : "NULL," : "'" + _tempVal + "',";
                } else if (isOneToMany && !isManyToMany && !isManyToOne) {
                    if (_tempVal != null) {
                        Object _getId = _tempVal.getClass().getDeclaredMethod("getId", null).invoke(_tempVal, null);
                        if (_getId == null || (_getId instanceof Integer && Integer.valueOf(_getId.toString()) < 1)) {
                            throw new SQLException(_getId + " is invalid id!");
                        }
                        this.sqlQuery += "`" + columnName + "_id`,";
                        values += "startSetAsParamLater" + _getId + "endSetAsParamLater,";
                    }
                } else if (!isOneToMany && isManyToMany && !isManyToOne) {
                    List<Object> _manyToManyValue = (List<Object>) _tempVal;
                    if (_manyToManyValue != null && _manyToManyValue.size() > 0) {
                        String _relationalClass
                                = TypeHelper.getClassName(this._methods.get(i).getName(), type).toLowerCase();
                        String associationTable
                                = _relationalClass.compareTo(this.tableName) < 0
                                ? _relationalClass + "s_" + this.tableName + ""
                                : this.tableName + "_" + _relationalClass + "s";
                        _manyToManyInsert += (_manyToManyInsert.length() < 2 ? "INSERT INTO `" + associationTable + "` (" : "")
                                + "`" + _relationalClass + "_id`, `" + this.tableName.substring(0, this.tableName.length() - 1) + "_id`) VALUES";
                        for (int j = 0; j < _manyToManyValue.size(); j++) {
                            Object _relationalKey = _manyToManyValue.get(j).getClass()
                                    .getDeclaredMethod("getId", null).invoke(_manyToManyValue.get(j), null);
                            _manyToManyInsert += "(startSetAsParamLater" + _relationalKey + "endSetAsParamLater,'MANYTOMANYKEY'),";
                        }
                        _manyToManyInsert = _manyToManyInsert.substring(0, _manyToManyInsert.length() - 2) + ");";
                    }
                } else if (!isOneToMany && !isManyToMany && isManyToOne) {
                    List<Object> _manyToOneValue = (List<Object>) _tempVal;
                    if (_manyToOneValue != null && _manyToOneValue.size() > 0) {
                        String _relationalClass
                                = TypeHelper.getClassName(this._methods.get(i).getName(), type);
                        String _relationalColumnName = TypeHelper.getForeignKey(_relationalClass, type);
                        for (int j = 0; j < _manyToOneValue.size(); j++) {
                            Object _relationalKey = _manyToOneValue.get(j).getClass()
                                    .getDeclaredMethod("getId", null).invoke(_manyToOneValue.get(j), null);
                            _manyToOneInsert += "UPDATE `" + _relationalClass.toLowerCase() + "s` SET `"
                                    + _relationalColumnName + "_id`='MANYTOONEKEY' WHERE `" + _relationalClass.toLowerCase()
                                    + "s`.`id`='" + _relationalKey + "';\n";

                        }
                    }
                }
            }
            values = values.trim().substring(0, values.length() - 1) + ")";
            this.sqlQuery = this.sqlQuery.trim().substring(0, this.sqlQuery.length() - 1) + ") " + values + ";";
            // LIMITATION: IF WE SIMULTANEOUSLY ADD MORE THAN ONE OBJECT, THE LAST ONE ID WILL BE USED!
            // "LAST_INSERT_ID()" IF WE ADD ANOTHER OBJECT TO ANOTHER TABLE, THAT ONE WILL BE USED, WHICH IS TOTALLY INCORRECT!
            // MAYBE THERE IS ANOTHER WAY TO SOLVE THIS ISSUE, I HAVE TO ASK! :)
            String _selectItemId = "(SELECT `" + this.tableName + "`.`id` FROM `" + this.tableName + "` ORDER BY `" + this.tableName + "`.`id` DESC LIMIT 1)";
            if (_manyToManyInsert.length() > 5) {
                this.sqlQuery += "\n" + _manyToManyInsert.replaceAll("'MANYTOMANYKEY'", _selectItemId);
            }
            if (_manyToOneInsert.length() > 5) {
                this.sqlQuery += "\n" + _manyToOneInsert.replaceAll("'MANYTOONEKEY'", _selectItemId);
            }

            String regex = "startSetAsParamLater([\\s\\S]*?)endSetAsParamLater";
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(this.sqlQuery);
            List<String> matchs = new ArrayList<String>();
            while (matcher.find()) {
                String toBeReplaced = matcher.group(0);
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    matchs.add(matcher.group(i));
                }
                this.sqlQuery = this.sqlQuery.replaceAll(toBeReplaced, "?");
            }
            String[] queries = this.sqlQuery.replaceAll("\n", "").split(";");
            for (int i = 0; i < queries.length; i++) {
                this.stm = this.con.prepareStatement(queries[i]);
                if (queries[i].contains("?")) {
                    for (int j = 0; j < matchs.size(); j++) {
                        this.stm.setString((j + 1), matchs.get(j));
                    }
                }
                affected += this.stm.executeUpdate();
                this.stm.close();
            }
            return affected;
        } catch (IllegalAccessException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (this.rs != null && !this.rs.isClosed()) {
                    this.rs.close();
                }
                if (this.stm != null && !this.stm.isClosed()) {
                    this.stm.close();
                }
                if (this.con != null && !this.con.isClosed()) {
                    this.con.close();
                }
            } catch (Exception ex) {
            }
        }
        return affected;
    }

    /**
     * Used to update a specific row by id. You can select a row first and store it in a variable.
     * Edit that row by using setters and finally use this method to passed edited row as parameter.
     * NOTE: this method uses a field called as same as your field name plus "IsDirty" which it has private getter only
     *       this field and method will be added by "Migration.jar" tool, SO DON'T DELETE THEM!.
     * @param item edited object
     * @return true in case new value has been updated in the database, otherwise false
     */
    public boolean update(T item) {
        if (this.con == null) {
            return false;
        }
        this._methods = TypeHelper.getGetterMethods(type, false);
        Object _key = null;
        String _manyToOne = "";
        String _manyToMany = "";
        for (Method m : this._methods) {
            String checkIsDirtyMethod = m.getName() + "IsDirty";
            try {
                if (_key == null && m.getName().equals("getId")) {
                    _key = m.invoke(item, null);
                }
                Method isDirty = type.getDeclaredMethod(checkIsDirtyMethod);
                isDirty.setAccessible(true);
                Object _val = isDirty.invoke(item, null);
                boolean _resultOfIsDirty = Boolean.valueOf(_val.toString());
                if (_resultOfIsDirty && !m.getName().equals("getId")) {
                    String fieldName = checkIsDirtyMethod.replaceAll("IsDirty|get", "").toLowerCase();
                    Object _newValue = m.invoke(item, null);
                    if (ValidationHelper.isPremitive(m.getName(), type)) {
                        if (m.getReturnType() == Boolean.TYPE) {
                            boolean _convertVal = Boolean.valueOf(_newValue.toString());
                            this.sqlQuery += "`" + fieldName
                                    + (_convertVal ? "`=''," : "`=NULL,");
                        } else {
                            this.sqlQuery += "`" + fieldName + "`='" + _newValue + "',";
                        }
                    } else if (ValidationHelper.isOneToMany(m.getName(), type)) {
                        if (_newValue != null) {
                            Method relationKeyMethod = TypeHelper.getGetterMethod("getId", _newValue.getClass());
                            Object relationKeyValue = relationKeyMethod.invoke(_newValue, null);
                            if ((relationKeyValue != null && relationKeyValue instanceof Integer
                                    && Integer.valueOf(relationKeyValue.toString()) > 0)
                                    || (relationKeyValue != null && relationKeyValue instanceof String)) {
                                this.sqlQuery += "`" + fieldName + "_id`='" + relationKeyValue + "',";
                            }
                        }
                    } else if (ValidationHelper.isManyToOne(m.getName(), type)) {
                        if (_newValue != null) {
                            String _relationalTable = TypeHelper.getClassName(m.getName(), type);
                            String _relationField = TypeHelper.getForeignKey(_relationalTable, type);
                            _manyToOne += "UPDATE `" + _relationalTable.toLowerCase() + "s` SET `"
                                    + _relationField + "_id`=NULL WHERE `" + _relationField + "_id`='" + _key + "';\n";
                            List<Object> _list = (List<Object>) _newValue;
                            for (Object o : _list) {
                                Method _relationKeyMethod = o.getClass().getDeclaredMethod("getId", null);
                                Object _relationKey = _relationKeyMethod.invoke(o, null);
                                if ((_relationKey != null && _relationKey instanceof Integer
                                        && Integer.valueOf(_relationKey.toString()) > 0)
                                        || (_relationKey != null && _relationKey instanceof String)) {
                                    _manyToOne += "UPDATE `" + _relationalTable.toLowerCase() + "s` SET `"
                                            + _relationField + "_id`='" + _key + "' WHERE `id`='" + _relationKey + "';\n";
                                }
                            }
                        }
                    } else if (ValidationHelper.isManyToMany(m.getName(), type)) {
                        if (_newValue != null) {
                            String _relationalTable = TypeHelper.getClassName(m.getName(), type);
                            String associationTable = this.tableName.compareTo(_relationalTable.toLowerCase() + "s") < 0
                                    ? this.tableName + "_" + _relationalTable.toLowerCase() + "s"
                                    : _relationalTable.toLowerCase() + "s_" + this.tableName;
                            _manyToMany += "DELETE FROM `" + associationTable + "` WHERE `"
                                    + this.tableName.substring(0, this.tableName.length() - 1) + "_id`='" + _key + "';\n";
                            List<Object> _list = (List<Object>) _newValue;
                            if (_list.size() > 0) {
                                _manyToMany += "INSERT INTO `" + associationTable + "` (`"
                                        + this.tableName.substring(0, this.tableName.length() - 1) + "_id`, `"
                                        + _relationalTable.toLowerCase() + "_id`) VALUES";
                            }
                            for (Object o : _list) {
                                Method _relationKeyMethod = o.getClass().getDeclaredMethod("getId", null);
                                Object _relationKey = _relationKeyMethod.invoke(o, null);
                                if ((_relationKey != null && _relationKey instanceof Integer
                                        && Integer.valueOf(_relationKey.toString()) > 0)
                                        || (_relationKey != null && _relationKey instanceof String)) {
                                    _manyToMany += "('" + _key + "', '" + _relationKey + "'),";
                                }
                            }
                            if (_manyToMany.endsWith(",")) {
                                _manyToMany = _manyToMany.substring(0, _manyToMany.length() - 1) + ";\n";
                            }
                        }
                    }
                }
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        String[] statements;
        if (this.sqlQuery.length() > 5) {
            this.sqlQuery = this.sqlQuery.trim().substring(0, this.sqlQuery.trim().length() - 1);
            this.sqlQuery = "UPDATE `" + this.tableName + "` SET " + this.sqlQuery;
            this.sqlQuery += " WHERE `id`='" + _key + "';\n";
            statements = this.sqlQuery.split("\n");
        } else {
            this.sqlQuery += _manyToOne + "\n" + _manyToMany;
            statements = this.sqlQuery.split("\n");
        }
        boolean aborted = false;
        for (String statement : statements) {
            try {
                if (statement == null || statement.length() < 5) {
                    continue;
                }
                this.stm = this.con.prepareStatement(statement);
                this.stm.executeUpdate();
            } catch (SQLException ex) {
                Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
                aborted = true;
                break;
            }
        }
        if (!aborted) {
            return true;
        }
        return false;
    }

    /**
     * Used to delete a row/s from database depending on a condition.
     * @param predicate: used condition to delete row/s
     *          Example: db.getUsers().remove(u -> u.getId() == 3);
     */
    public void remove(Predicate<T> predicate) {
        if (this.con == null) {
            return;
        }
        try {
            String _lambda = LambdaConverter.convertPredicateToSql(predicate);
            String regex = "startSetAsParamLater([\\s\\S]*?)endSetAsParamLater";
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(_lambda);
            List<String> matchs = new ArrayList<String>();
            while (matcher.find()) {
                String toBeReplaced = matcher.group(0);
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    matchs.add(matcher.group(i));
                }
                _lambda = _lambda.replaceAll(toBeReplaced, "?");
            }
            this.sqlQuery = "DELETE FROM `" + this.tableName + "` WHERE " + _lambda;
            this.stm = this.con.prepareStatement(this.sqlQuery);
            for (int i = 1; i <= matchs.size(); i++) {
                this.stm.setString(i, matchs.get(i - 1));
            }
            _lambda = this.stm.toString();
            if (_lambda.contains("endLikeFunc") || _lambda.contains("startLikeFunc")) {
                _lambda = _lambda.substring(_lambda.indexOf(": ") + 2, _lambda.length());
                _lambda = _lambda.replaceAll("'endLikeFunc", "%'").replaceAll("startLikeFunc'", "'%");
                this.stm = this.con.prepareStatement(_lambda);
            }
            this.stm.execute();
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (this.rs != null && !this.rs.isClosed()) {
                    this.rs.close();
                }
                if (this.stm != null && !this.stm.isClosed()) {
                    this.stm.close();
                }
                if (this.con != null && !this.con.isClosed()) {
                    this.con.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    /**
     * this method used by following methods (first, where and getItems) to parse ResultSet into List of type T
     * @param rs ResultSet of executed SqlQuery
     * @return List<T>
     */
    private List<T> _fetchTheResults(ResultSet rs) {
        if (this._methods == null) {
            this._methods = TypeHelper.getSetterMethods(this.type, false);
        }
        List<T> _temp = new ArrayList(); // stores end results
        try {
            T _t = null;
            while (rs.next()) {
                try {
                    _t = this.type.newInstance();
                    for (int i = 0; i < this._methods.size(); i++) {
                        String _methodName = this._methods.get(i).getName();
                        String _columnName = _methodName.replaceAll("set", "").toLowerCase();
                        Method _method = TypeHelper.getSetterMethod(_methodName, type);
                        // check if current field isn't relational key
                        boolean isOneToMany = ValidationHelper.isOneToMany(_methodName, type);
                        boolean isManyToOne = ValidationHelper.isManyToOne(_methodName, type);
                        boolean isManyToMany = ValidationHelper.isManyToMany(_methodName, type);
                        if (this.toInclude == null && !isOneToMany && !isManyToOne && !isManyToMany) {
                            Object _valueFromCurrentRow = rs.getObject(this.tableName + "_" + _columnName);
                            if (_valueFromCurrentRow == "" || _valueFromCurrentRow == null) {
                                _method.invoke(_t, _valueFromCurrentRow == null ? false : true);
                            } else {
                                _method.invoke(_t, _valueFromCurrentRow);
                            }
                        } else {
                            if (this.toInclude != null && !isOneToMany && !isManyToOne && !isManyToMany) {
                                Object _valueFromCurrentRow = rs.getObject(this.tableName + "_" + _columnName);
                                if (_valueFromCurrentRow == "" || _valueFromCurrentRow == null) {
                                    _method.invoke(_t, _valueFromCurrentRow == null ? false : true);
                                } else {
                                    _method.invoke(_t, _valueFromCurrentRow);
                                }
                            } else {
                                if (isOneToMany && this.toInclude != null) {
                                    Object _relationalValue = TypeHelper.createNewInstanceByRelationalProperty(_methodName, type);
                                    Object checkIfIdIsNull = rs.getObject(_relationalValue.getClass().getSimpleName().toLowerCase()
                                            + "s_id");
                                    if (checkIfIdIsNull != null) {
                                        List<Method> _relationalClassMethods = TypeHelper.getSetterMethods(_relationalValue.getClass(), false);// _relationalValue.getClass().getDeclaredMethods();
                                        for (Method m : _relationalClassMethods) {
                                            if (m.getName().startsWith("set")
                                                    && ValidationHelper.isValidSetterOrGetter(m.getName(), _relationalValue.getClass())) {
                                                if (!ValidationHelper.isOneToMany(m.getName(), _relationalValue.getClass())
                                                        && !ValidationHelper.isManyToOne(m.getName(), _relationalValue.getClass())
                                                        && !ValidationHelper.isManyToMany(m.getName(), _relationalValue.getClass())) {
                                                    Object _valueFromCurrentRow
                                                            = rs.getObject(_relationalValue.getClass().getSimpleName().toLowerCase()
                                                                    + "s_" + m.getName().replace("set", "").toLowerCase());
                                                    if (_valueFromCurrentRow == "" || _valueFromCurrentRow == null) {
                                                        m.invoke(_relationalValue, _valueFromCurrentRow == null ? false : true);
                                                    } else {
                                                        m.invoke(_relationalValue, _valueFromCurrentRow);
                                                    }
                                                }
                                            }
                                        }

                                        Method _getSetterForCurrentRelationalObject = TypeHelper.getSetterMethod(_methodName, type);
                                        _getSetterForCurrentRelationalObject.invoke(_t, _relationalValue);
                                    }
                                } else if ((isManyToOne || isManyToMany) && this.toInclude != null
                                        && this.toInclude.contains(_methodName.replaceAll("set", ""))) {
                                    String _readRowAsField = (String) rs.getObject(
                                            TypeHelper.getClassName(_methodName, type)
                                            + "s_values"
                                    );
                                    if (_readRowAsField == null || _readRowAsField.length() < 1) {
                                        continue;
                                    }
                                    String[] _splieValues = _readRowAsField.split("\\[START_ROWSEPERATOR_END\\]");
                                    Method collectionSetter = TypeHelper.getSetterMethod(_methodName, type);
                                    List<Object> resultOfTheCollection = new ArrayList();
                                    for (String row : _splieValues) {
                                        Object _relationalClassInstance
                                                = TypeHelper.createNewInstanceByRelationalProperty(_methodName, type);
                                        Method[] _methodsInRelationalClass = _relationalClassInstance.getClass().getDeclaredMethods();
                                        for (Method m : _methodsInRelationalClass) {
                                            if (m.getName().startsWith("set")
                                                    && ValidationHelper.isValidSetterOrGetter(m.getName(), _relationalClassInstance.getClass())
                                                    && !ValidationHelper.isOneToMany(m.getName(), _relationalClassInstance.getClass())
                                                    && !ValidationHelper.isManyToOne(m.getName(), _relationalClassInstance.getClass())
                                                    && !ValidationHelper.isManyToMany(m.getName(), _relationalClassInstance.getClass())) {
                                                String[] _fieldsInCurrentRow = row.split("\\[START_" + m.getName().replace("set", "") + "_END\\]");
                                                String _tempVal = _fieldsInCurrentRow[0];
                                                String _value;
                                                if (_tempVal.contains("_END]")) {
                                                    String[] _splitTempValue = _tempVal.split("_END\\]");
                                                    _value = _splitTempValue[_splitTempValue.length - 1];
                                                } else {
                                                    _value = _tempVal;
                                                }
                                                PrimitiveTypes _resultType = TypeHelper.getMethodType(m);
                                                switch (_resultType) {
                                                    case Int:
                                                        m.invoke(_relationalClassInstance, Integer.valueOf(_value));
                                                        break;
                                                    case Long:
                                                        m.invoke(_relationalClassInstance, Long.valueOf(_value));
                                                        break;
                                                    case Short:
                                                        m.invoke(_relationalClassInstance, Short.valueOf(_value));
                                                        break;
                                                    case String:
                                                        m.invoke(_relationalClassInstance, _value);
                                                        break;
                                                    case Double:
                                                        m.invoke(_relationalClassInstance, Double.valueOf(_value));
                                                        break;
                                                    case Float:
                                                        m.invoke(_relationalClassInstance, Float.valueOf(_value));
                                                        break;
                                                    case Boolean:
                                                        m.invoke(_relationalClassInstance, _value.equals("false") ? false : true);
                                                        break;
                                                }
                                            }
                                        }
                                        resultOfTheCollection.add(_relationalClassInstance);
                                    }
                                    collectionSetter.invoke(_t, resultOfTheCollection);
                                }
                            }
                        }
                    }
                    Field[] _fields = type.getDeclaredFields();
                    for (Field f : _fields) {
                        if (f.getName().endsWith("IsDirty") && f.getType() == Boolean.TYPE) {
                            f.setAccessible(true);
                            f.setBoolean(_t, false);
                        }
                    }
                    _temp.add(_t);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedDataTypeException ex) {
                    Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (!this.con.isClosed()) {
                this.con.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DbSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (this.rs != null && !this.rs.isClosed()) {
                    this.rs.close();
                }
                if (this.stm != null && !this.stm.isClosed()) {
                    this.stm.close();
                }
                if (this.con != null && !this.con.isClosed()) {
                    this.con.close();
                }
            } catch (Exception ex) {
            }
        }
        return _temp;
    }

}
