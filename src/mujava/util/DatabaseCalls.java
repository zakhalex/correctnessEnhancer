package mujava.util;

import mujava.MutationControl;
import mujava.MutationSystem;
import mujava.test.TestResult;
import org.apache.derby.drda.NetworkServerControl;
import org.junit.runner.notification.Failure;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class DatabaseCalls {

    private static Set<String> tables=new HashSet<String>();

    public final static String selectConfig = "SELECT * FROM CONFIGURATIONS WHERE ID=? AND FILE_NAME=?";

    public final static String insertResultSql = "INSERT INTO TESTRESULTS (PROGRAM_LOCATION,MUTATED_CLASS," +
            "TEST_NAME,MUTATION_TYPE,ORIGINAL_CORRECTNESS_INDEX,CORRECTNESS_ENHANCED,RELATIVELY_MORE_CORRECT," +
            "MUTATED_CORRECTNESS_INDEX,LAST_UPDATED,COMMENT) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?)";

    public final static String insertControlSql = "INSERT INTO CONFIGURATIONS (" +
            "ID, " +
            "FILE_NAME, " +
            "CLASS_NAME, " +
            "METHOD_NAME, " +
            "TEST_NAME, " +
            "LAST_UPDATED) " +
            "VALUES(?,?,?,?,?,?)";
            //"VALUES(?,?,?,?,?)";

    public final static String updateResultSql = "UPDATE TESTRESULTS SET ORIGINAL_CORRECTNESS_INDEX = ?,CORRECTNESS_ENHANCED = ?,RELATIVELY_MORE_CORRECT = ?," +
            "MUTATED_CORRECTNESS_INDEX = ?,LAST_UPDATED = ?,COMMENT = ? WHERE PROGRAM_LOCATION = ? AND MUTATED_CLASS = ? AND TEST_NAME = ? AND MUTATION_TYPE = ?";

    public final static String createResultTableSql = "CREATE TABLE TESTRESULTS ("
            + "	PROGRAM_LOCATION VARCHAR(1024),"
            + "	MUTATED_CLASS VARCHAR(1024),"
            + "	TEST_NAME VARCHAR(1024) NOT NULL,"
            + "	MUTATION_TYPE VARCHAR(1024),"
            + "	ORIGINAL_CORRECTNESS_INDEX INTEGER,"
            + "	CORRECTNESS_ENHANCED BOOLEAN,"
            + "	RELATIVELY_MORE_CORRECT BOOLEAN,"
            + "	MUTATED_CORRECTNESS_INDEX INTEGER,"
            + "	LAST_UPDATED TIMESTAMP,"
            + "	COMMENT VARCHAR(1024),"
            + " PRIMARY KEY (PROGRAM_LOCATION, MUTATED_CLASS, TEST_NAME, MUTATION_TYPE)"
            + ")";

    public final static String createControlTableSql = "CREATE TABLE CONFIGURATIONS ("
            + "	ID INTEGER NOT NULL,"// GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
            + "	FILE_NAME VARCHAR(1024),"
            + "	CLASS_NAME VARCHAR(4096),"
            + "	METHOD_NAME VARCHAR(4096) DEFAULT '',"
            + "	TEST_NAME VARCHAR(4096) DEFAULT '',"
            + "	LAST_UPDATED TIMESTAMP,"
            + " PRIMARY KEY (FILE_NAME, CLASS_NAME, METHOD_NAME, TEST_NAME)"
            + ")";

    public final static String truncateResultTableSql = "TRUNCATE TABLE TESTRESULTS";
    public final static String truncateControlTableSql = "TRUNCATE TABLE CONFIGURATIONS";
    public final static String deleteControlSql = "DELETE FROM CONFIGURATIONS WHERE FILE_NAME=?";
    public final static String dropControlTableSql = "DROP TABLE CONFIGURATIONS";

    public static List<Integer> insertResult(List<TestResult> list)
    {
        ArrayList<Integer> result=new ArrayList<>();
        for (TestResult tr:list)
        {
            result.add(insertResult(tr));
        }
        return result;
    }

    private static boolean containsSet(List<Failure> list, List<Failure> sublist) {
        if(sublist.size()>list.size())
        {
            return false;
        }
        if(sublist.isEmpty())
        {
            return true;
        }
        HashSet<String> set=new HashSet<String>();
        HashSet<String> subSet=new HashSet<String>();
        for(Failure f:list)
        {
            set.add(f.getTestHeader());
        }
        for(Failure f:sublist)
        {
            subSet.add(f.getTestHeader());
        }
        if(set.containsAll(subSet)) {
            return true;
        }
        return false;
        //return Collections.indexOfSubList(list, sublist) != -1;
    }

    public static ConfigurationItem readConfiguration(Integer id)
    {
        ConfigurationItem config=null;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(selectConfig)) {
            System.out.println("Preparing to read configuration information");
            pstmt.setInt(1, id);
            pstmt.setString(2, MutationSystem.databaseMarker);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {

                String fileName=result.getString    ("FILE_NAME");
                String className=result.getString    ("CLASS_NAME");
                String methodName=result.getString    ("METHOD_NAME");
                String testName=result.getString    ("TEST_NAME");
                int identifier=result.getInt       ("ID");
                System.out.println(id+","+identifier+","+fileName+","+className+","+methodName+","+testName+","+result.getTimestamp("LAST_UPDATED"));
                HashMap<String, String> properties=new HashMap<>();
                properties.put(MutationControl.Inputs.FILES.getLabel(),fileName);
                properties.put(MutationControl.Inputs.MUTANTS.getLabel(),className);
                properties.put(MutationControl.Inputs.METHODS.getLabel(),methodName);
                properties.put(MutationControl.Inputs.TESTS.getLabel(),testName);
                config=new ConfigurationItem(properties);

                // etc.
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return config;
    }

    public static void clearControlConfig(String dbControl)
    {
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
        Statement stmt = conn.createStatement();) {
            if(dbControl!=null && dbControl.equalsIgnoreCase("hard")) {
                stmt.execute(dropControlTableSql);
            }
            else
            {
                stmt.execute(truncateControlTableSql);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This function inserts the task-splitting configurations into the database.
     * @param configurations - the list of configurations
     * @return
     */
    public static int insertConfiguration(List<ConfigurationItem> configurations)
    {
        int result=0;
        int deleteResult=-1;
        //It does a scanning delete of the previous results
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(deleteControlSql)) {
            pstmt.setString(1, MutationSystem.databaseMarker);
            deleteResult=pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("We have removed "+deleteResult+" lines.");

        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
            PreparedStatement pstmt = conn.prepareStatement(insertControlSql)) {
            System.out.println("Preparing to record configuration information");
            int sequence=1;
            //If we control identity from here
            for (ConfigurationItem config: configurations)
            {
                pstmt.setInt(1, sequence);//==null?-1:originalResult);//original
                pstmt.setString(2, config.getFileName());
                pstmt.setString(3, config.getClassName());
                pstmt.setString(4, config.getMethodName());
                pstmt.setString(5, config.getTestName());
                pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                result+=pstmt.executeUpdate();
                sequence++;
            }
//            //If the table controls the identity
//            for (ConfigurationItem config: configurations)
//            {
//                pstmt.setString(1, config.getFileName());
//                pstmt.setString(2, config.getClassName());
//                pstmt.setString(3, config.getMethodName());
//                pstmt.setString(4, config.getTestName());
//                pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
//                result = pstmt.executeUpdate();
//            }
//            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        if(MutationSystem.databaseCount!=null)
        {
            try(PrintWriter pw=new PrintWriter(MutationSystem.databaseCount))
            {
                pw.println(result);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static int insertResult(TestResult tr)
    {
        int result=-1;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);

             PreparedStatement pstmt = conn.prepareStatement(insertResultSql)) {
            System.out.println("MARKER: "+tr.getTestSetName());
            for (Map.Entry<String, OriginalTestResult> entry:tr.mutation_results.entrySet()) {
                OriginalTestResult originalResult=tr.getOriginalResult();//tr.originalResults.get(entry.getKey());
                int originalScore=originalResult.getResultScore().intValue();
                int mutantScore=entry.getValue().getResultScore().intValue();
                boolean isRelativelyCorrect=containsSet(originalResult.getFailure(),entry.getValue().getFailure());
                boolean isCorrectnessEnhanced=originalScore<mutantScore;

//                if((!isRelativelyCorrect)&&(mutantScore>0)&&(mutantScore>=originalScore))
//                {
//                    System.err.println("We have a candidate to analyze.");
//                    System.err.println("Original score is: "+originalScore);
//                    System.err.println("Mutant score is: "+mutantScore);
//                    System.err.println("Original Failures: ");
//                    for (Failure failure : originalResult.getFailure()) {
//                        System.err.println(failure.getTestHeader().substring(0, failure.getTestHeader().indexOf("(")));
//                    }
//                    System.err.println("Mutant Failures: ");
//                    for (Failure failure : entry.getValue().getFailure()) {
//                        System.err.println(failure.getTestHeader().substring(0, failure.getTestHeader().indexOf("(")));
//                    }
//                    System.exit(0);
//                }
                pstmt.setString(1, tr.getProgramLocation());
                pstmt.setString(2, tr.getTargetMutant());
                pstmt.setString(3, tr.getTestSetName());
                pstmt.setString(4, entry.getKey());
                pstmt.setInt(5, originalScore);//==null?-1:originalResult);//original
                pstmt.setBoolean(6, isCorrectnessEnhanced);
                pstmt.setBoolean(7, isRelativelyCorrect);
                pstmt.setInt(8, mutantScore);

                pstmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
                pstmt.setString(10, "");//Comment to be inserted

                result = pstmt.executeUpdate();
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    /**
     * Creates the table to store the experiment results.
     * @return true if the table was successfully created or false otherwise.
     */
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

    public static boolean createControlTable()
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
            if(!tables.contains("CONFIGURATIONS".toLowerCase())) {
                result = stmt.execute(createControlTableSql);
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
