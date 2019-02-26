package mujava.util;

import mujava.MutationSystem;
import mujava.test.TestResult;
import org.apache.derby.drda.NetworkServerControl;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class DatabaseCalls {

    private static Set<String> tables=new HashSet<String>();
    public final static String insertResultSql = "INSERT INTO TESTRESULTS (PROGRAM_LOCATION,MUTATED_CLASS," +
            "TEST_NAME,MUTATION_TYPE,ORIGINAL_CORRECTNESS_INDEX,CORRECTNESS_ENHANCED," +
            "MUTATED_CORRECTNESS_INDEX,LAST_UPDATED,COMMENT) " +
            "VALUES(?,?,?,?,?,?,?,?,?)";

    public final static String updateResultSql = "UPDATE TESTRESULTS SET ORIGINAL_CORRECTNESS_INDEX = ?,CORRECTNESS_ENHANCED = ?," +
            "MUTATED_CORRECTNESS_INDEX = ?,LAST_UPDATED = ?,COMMENT = ? WHERE PROGRAM_LOCATION = ? AND MUTATED_CLASS = ? AND TEST_NAME = ? AND MUTATION_TYPE = ?";

    public final static String createResultTableSql = "CREATE TABLE TESTRESULTS ("
            + "	PROGRAM_LOCATION VARCHAR(1024),"
            + "	MUTATED_CLASS VARCHAR(1024),"
            + "	TEST_NAME VARCHAR(1024) NOT NULL,"
            + "	MUTATION_TYPE VARCHAR(1024),"
            + "	ORIGINAL_CORRECTNESS_INDEX INTEGER,"
            + "	CORRECTNESS_ENHANCED BOOLEAN,"
            + "	MUTATED_CORRECTNESS_INDEX INTEGER,"
            + "	LAST_UPDATED TIMESTAMP,"
            + "	COMMENT VARCHAR(1024),"
            + " PRIMARY KEY (PROGRAM_LOCATION, MUTATED_CLASS, TEST_NAME, MUTATION_TYPE)"
            + ")";

    public final static String truncateResultTableSql = "TRUNCATE TABLE TESTRESULTS";

    public static List<Integer> insertResult(List<TestResult> list)
    {
        ArrayList<Integer> result=new ArrayList<>();
        for (TestResult tr:list)
        {
            result.add(insertResult(tr));
        }
        return result;
    }

    public static int insertResult(TestResult tr)
    {
        int result=-1;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);

            PreparedStatement pstmt = conn.prepareStatement(insertResultSql)) {
            System.out.println("MARKER: "+tr.getTestSetName());
            for (Map.Entry<String, Integer> entry:tr.mutation_results.entrySet()) {
                Integer originalResult=tr.getOriginalResult();//tr.originalResults.get(entry.getKey());
                pstmt.setString(1, tr.getProgramLocation());
                pstmt.setString(2, tr.getTargetMutant());
                pstmt.setString(3, tr.getTestSetName());
                pstmt.setString(4, entry.getKey());
                pstmt.setInt(5, originalResult);//==null?-1:originalResult);//original
                pstmt.setBoolean(6, originalResult<entry.getValue());
                pstmt.setInt(7, entry.getValue());

                pstmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
                pstmt.setString(9, "");//Comment to be inserted

                result = pstmt.executeUpdate();
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    public static boolean createResultTable()
    {
        boolean result=false;
         /*
            Creating the database to write the test results to.
             */

        // SQL statement for creating a new table


        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             Statement stmt = conn.createStatement()) {
            // create a new table
            resetSchemaCache(conn);
            if(!tables.contains("TESTRESULTS".toLowerCase())) {
                result = stmt.execute(createResultTableSql);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    public static void startServer() throws Exception
    {
        PrintWriter pw=new PrintWriter("database.log");
        NetworkServerControl server = new NetworkServerControl();
        server.start (pw);
    }

    private static Set<String> getDBTables(Connection targetDBConn) throws SQLException
    {
        Set<String> set = new HashSet<String>();
        DatabaseMetaData dbmeta = targetDBConn.getMetaData();
        readDBTable(set, dbmeta, "TABLE", null);
        readDBTable(set, dbmeta, "VIEW", null);
        return set;
    }

    private static void readDBTable(Set<String> set, DatabaseMetaData dbmeta, String searchCriteria, String schema)
            throws SQLException
    {
        ResultSet rs = dbmeta.getTables(null, schema, null, new String[]
                { searchCriteria });
        while (rs.next())
        {
            set.add(rs.getString("TABLE_NAME").toLowerCase());
        }
    }

    public static void resetSchemaCache()
    {
        resetSchemaCache(null);
    }

    public static void resetSchemaCache(Connection connection)
    {
        tables.clear();
        if(connection!=null)
        {
            try {
                tables = getDBTables(connection);
            }catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        else {
            try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL)) {
                tables = getDBTables(conn);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
