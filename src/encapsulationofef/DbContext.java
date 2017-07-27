package encapsulationofef;

import encapsulationofef.helpers.SqlType;
import encapsulationofef.lambdaparser.LambdaConverter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * An abstract class which is used to load the driver.
 * Your DbConfiguration file must derive from this class!
 * @author Ali
 */
public abstract class DbContext {

    private String dbUrl;
    private String user;
    private String pass;
    private SqlType databaseType;
    private Connection connection;
    private boolean isSupported = true;
    /**
     * a constructor is used to select database type and setting up properties
     * @param dbType:   databse type, use "SqlType" which is enum type
     *         Example: SqlType.MySqlDB
     * @param dbUrl:    path to your database
     *         Example: jdbc:mysql://localhost/myDb
     * @param username: username to connect to the SQL Server
     * @param password  password to connect to the SQL Server
     */
    public DbContext(SqlType dbType, String dbUrl, String username, String password) {
        try {
            this.dbUrl = dbUrl;
            this.user = username;
            this.pass = password;
            this.databaseType = dbType;
            if (this.databaseType == SqlType.MySQLDB) {
                Class.forName("com.mysql.jdbc.Driver");
            } else if (this.databaseType == SqlType.OracleDB) {
                // NOT IMPLEMENTED YET!
                System.err.println("This framework doesn't support Oracle DB, maybe one day :)");
                isSupported = false;
                return;
                //Class.forName("oracle.jdbc.driver.OracleDriver");
            }
            LambdaConverter.loader();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public Connection getConnection() {
        if(!this.isSupported)
            return null;
        try {
            this.connection = DriverManager.getConnection(this.dbUrl, this.user, this.pass);
            return this.connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
