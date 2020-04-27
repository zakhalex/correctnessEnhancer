package mujava.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DerbyUtils {

    public static boolean tableAlreadyExists(SQLException e) {
        boolean exists;
        if (e.getSQLState().equals("X0Y32")) {
            exists = true;
        } else {
            exists = false;
        }
        return exists;
    }

    public static ResultSet getAllTableNames(Connection con, String Schema_Name) throws SQLException {
        return con.getMetaData().getTables(null, Schema_Name, "%", null);//Default schema name is "APP"
    }
}
