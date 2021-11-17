package mujava.util;

import mujava.MutationControl;
import mujava.MutationSystem;
import mujava.test.TestResult;
import org.apache.derby.drda.NetworkServerControl;
import org.junit.runner.notification.Failure;

import java.io.*;
import java.sql.*;
import java.util.*;

public class DatabaseCalls {

    private static Set<String> tables=new HashSet<String>();

    public final static String selectConfig = "SELECT * FROM CONFIGURATIONS WHERE ID=? AND FILE_NAME=?";

    public final static String selectNext = "SELECT cc.BASE_DIR, cc.MUTATION_CHAIN, cc.SERIALIZED_MUTATION_CHAIN, " +
            "cc.OVERALL_INDEX FROM APP.CHAINCONTROL cc " +
            "WHERE cc.LAST_UPDATED IS NULL AND cc.BASE_DIR=? " +
            "ORDER BY OVERALL_INDEX DESC FETCH FIRST ROW ONLY";

    public final static String selectFirstN = "SELECT cc.BASE_DIR, cc.MUTATION_CHAIN, cc.SERIALIZED_MUTATION_CHAIN, " +
            "cc.OVERALL_INDEX FROM APP.CHAINCONTROL cc " +
            "WHERE cc.LAST_UPDATED IS NULL AND cc.BASE_DIR=? " +
            "ORDER BY OVERALL_INDEX DESC FETCH FIRST ? ROWS ONLY";

    public final static String countCandidates = "SELECT COUNT(*) AS OVERALL FROM APP.CHAINCONTROL " +
            "WHERE cc.LAST_UPDATED IS NULL AND cc.BASE_DIR=?";

    public final static String selectOriginalResults = "SELECT * FROM ORIGINALTESTRESULTS WHERE BASE_DIR=? AND TEST_NAME=?";

    public final static String insertResultSql = "INSERT INTO TESTRESULTS (BASE_DIR,PROGRAM_LOCATION,MUTATED_CLASS," +
            "TEST_NAME,MUTATION_TYPE,ORIGINAL_CORRECTNESS_INDEX,CORRECTNESS_ENHANCED,RELATIVELY_MORE_CORRECT," +
            "MUTATED_CORRECTNESS_INDEX,ORIGINAL_RUN,MUTATED_CASES_RUN,NO_DROP_IN_TESTCASES,LAST_UPDATED,COMMENT) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public final static String insertOriginalResultSql = "INSERT INTO ORIGINALTESTRESULTS (BASE_DIR," +
            "TEST_NAME,ORIGINAL_CORRECTNESS_INDEX,SERIALIZED_DATA) " +
            "VALUES(?,?,?,?)";

    public final static String insertControlSql = "INSERT INTO CONFIGURATIONS (" +
            "ID, " +
            "FILE_NAME, " +
            "CLASS_NAME, " +
            "METHOD_NAME, " +
            "TEST_NAME, " +
            "LAST_UPDATED) " +
            "VALUES(?,?,?,?,?,?)";
            //"VALUES(?,?,?,?,?)";

    public final static String insertChainInfoSql = "INSERT INTO CHAINCONTROL (" +
            "BASE_DIR,MUTATION_CHAIN,OVERALL_INDEX,SERIALIZED_MUTATION_CHAIN) " +
            "VALUES(?,?,?,?)";

    public final static String updateChainControl = "UPDATE CHAINCONTROL SET LAST_UPDATED = ? WHERE BASE_DIR=? AND MUTATION_CHAIN=? AND LAST_UPDATED IS NULL";

    public final static String updateResultSql = "UPDATE TESTRESULTS SET ORIGINAL_CORRECTNESS_INDEX = ?,CORRECTNESS_ENHANCED = ?,RELATIVELY_MORE_CORRECT = ?," +
            "MUTATED_CORRECTNESS_INDEX = ?,LAST_UPDATED = ?,COMMENT = ? WHERE PROGRAM_LOCATION = ? AND MUTATED_CLASS = ? AND TEST_NAME = ? AND MUTATION_TYPE = ?";

    public final static String createResultTableSql = "CREATE TABLE TESTRESULTS ("
            + "	BASE_DIR VARCHAR(1024),"
            + "	PROGRAM_LOCATION VARCHAR(1024),"
            + "	MUTATED_CLASS VARCHAR(1024),"
            + "	TEST_NAME VARCHAR(1024) NOT NULL,"
            + "	MUTATION_TYPE VARCHAR(64),"
            + "	ORIGINAL_CORRECTNESS_INDEX INTEGER,"
            + "	CORRECTNESS_ENHANCED BOOLEAN,"
            + "	RELATIVELY_MORE_CORRECT BOOLEAN,"
            + "	MUTATED_CORRECTNESS_INDEX INTEGER,"
            + "	ORIGINAL_RUN INTEGER,"
            + "	MUTATED_CASES_RUN INTEGER,"
            + "	NO_DROP_IN_TESTCASES BOOLEAN,"
            + "	LAST_UPDATED TIMESTAMP,"
            + "	COMMENT VARCHAR(1024),"
            + " PRIMARY KEY (PROGRAM_LOCATION, MUTATED_CLASS, TEST_NAME, MUTATION_TYPE)"
            + ")";

    public final static String createOriginalResultTableSql = "CREATE TABLE ORIGINALTESTRESULTS ("
            + "	BASE_DIR VARCHAR(1024),"
            + "	TEST_NAME VARCHAR(1024) NOT NULL,"
            + "	ORIGINAL_CORRECTNESS_INDEX INTEGER,"
            + "	SERIALIZED_DATA BLOB,"
            + " PRIMARY KEY (BASE_DIR, TEST_NAME)"
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

    public final static String createChainControlTableSql = "CREATE TABLE CHAINCONTROL (" +
            "BASE_DIR VARCHAR(1024) NOT NULL," +
            "MUTATION_CHAIN VARCHAR(2048) NOT NULL," +
            "SERIALIZED_MUTATION_CHAIN BLOB," +
            "OVERALL_INDEX INTEGER DEFAULT 0," +
            "LAST_UPDATED TIMESTAMP," +
            "PRIMARY KEY (BASE_DIR, MUTATION_CHAIN)" +
            ")";
    public final static String createChainControlIndex="CREATE INDEX APP.chainPriorityIndex ON CHAINCONTROL (OVERALL_INDEX)";
    public final static String truncateResultTableSql = "TRUNCATE TABLE TESTRESULTS";
    public final static String truncateOriginalResultTableSql = "TRUNCATE TABLE ORIGINALTESTRESULTS";
    public final static String truncateControlTableSql = "TRUNCATE TABLE CONFIGURATIONS";
    public final static String truncateChainControlTableSql = "TRUNCATE TABLE CHAINCONTROL";
    public final static String deleteControlSql = "DELETE FROM CONFIGURATIONS WHERE FILE_NAME=?";
    public final static String dropControlTableSql = "DROP TABLE CONFIGURATIONS";
    public final static String dropChainControlTableSql = "DROP TABLE CHAINCONTROL";

    public final static String analyzeGroupingSql = "SELECT * FROM TESTRESULTS WHERE CORRECTNESS_ENHANCED=true AND RELATIVELY_MORE_CORRECT=true AND PROGRAM_LOCATION LIKE '%?%'";
    public final static String analytics = "SELECT" +
            " BASE_DIR," +
            " MUTATION_TYPE," +
            " SUM(CASE WHEN RELATIVELY_MORE_CORRECT THEN 1 ELSE 0 END) AS RELATIVELY_MORE_CORRECT_NUM," +
            " SUM(CASE WHEN CORRECTNESS_ENHANCED THEN 1 ELSE 0 END) AS CORRECTNESS_ENHANCED_NUM," +
            " SUM(CASE WHEN NO_DROP_IN_TESTCASES THEN 1 ELSE 0 END) AS NO_DROP_IN_TESTCASES_NUM," +
            " COUNT(*) AS OVERALL," +
            " SUM(CASE WHEN CORRECTNESS_ENHANCED THEN CASE WHEN RELATIVELY_MORE_CORRECT THEN CASE WHEN NO_DROP_IN_TESTCASES THEN 1 ELSE 0 END ELSE 0 END ELSE 0 END)," +
            " SUM(CASE WHEN MUTATED_CORRECTNESS_INDEX=100 THEN 1 ELSE CASE WHEN CORRECTNESS_ENHANCED THEN CASE WHEN RELATIVELY_MORE_CORRECT THEN CASE WHEN NO_DROP_IN_TESTCASES THEN 1 ELSE 0 END ELSE 0 END ELSE 0 END END)" +
            "FROM TESTRESULTS " +
            "GROUP BY BASE_DIR, MUTATION_TYPE";
    //Chain#3
    public final static String selectOverallIndex = "SELECT" +
            " MUTATION_TYPE," +
            " SUM(CASE WHEN RELATIVELY_MORE_CORRECT THEN 1000000 ELSE 0 END) AS RELATIVELY_MORE_CORRECT_NUM," +
            " SUM(CASE WHEN MUTATED_CORRECTNESS_INDEX=100 THEN 1000 ELSE CASE WHEN CORRECTNESS_ENHANCED THEN 1000 ELSE 0 END END) AS CORRECTNESS_ENHANCED_NUM," +
            " SUM(CASE WHEN NO_DROP_IN_TESTCASES THEN 1 ELSE 0 END) AS NO_DROP_IN_TESTCASES_NUM," +
            " COUNT(*) AS OVERALL " +
            "FROM TESTRESULTS WHERE BASE_DIR = ?" +
            "GROUP BY MUTATION_TYPE";
    public static List<Integer> insertResult(List<TestResult> list)
    {
        ArrayList<Integer> result=new ArrayList<>();
        for (TestResult tr:list)
        {
            result.add(insertResult(tr));
        }
        return result;
    }

    public static boolean containsSet(List<Failure> list, List<Failure> sublist) {
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

    public static int countCandidates(String baseDir)
    {
        int count=0;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(countCandidates)) {
            System.out.println("Preparing to retrieve the count");

            pstmt.setString(1, baseDir);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {
                count=result.getInt("OVERALL");
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return count;
    }

    public static Map<String,Integer> retrieveOverallIndex(String baseDir)
    {
        HashMap<String,Integer> resultMap=new HashMap<>();
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(selectOverallIndex)) {
            System.out.println("Preparing to retrieve the next candidate");

            pstmt.setString(1, baseDir);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {

                if(result.isLast()) {
                    Integer count=result.getInt("OVERALL");
                    Integer overallIndex = (100*result.getInt("RELATIVELY_MORE_CORRECT_NUM"))/count
                            +(100*result.getInt("CORRECTNESS_ENHANCED_NUM"))/count
                            +(100*result.getInt("NO_DROP_IN_TESTCASES_NUM"))/count;
                    String mutantName=result.getString("MUTATION_TYPE");
                    resultMap.put(mutantName,overallIndex);
                }
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultMap;
    }

    public static ProgramCandidate retrieveNthProgramCandidate(String baseDir,int n)
    {
        ProgramCandidate pp=null;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(selectFirstN)) {
            System.out.println("Preparing to retrieve the next candidate");

            pstmt.setString(1, baseDir);
            pstmt.setInt(2, n);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {

                if(result.isLast()) {
                    Blob fileName = result.getBlob("SERIALIZED_MUTATION_CHAIN");

                    try {
                        ObjectInputStream ois = new ObjectInputStream(fileName.getBinaryStream());
                        pp = (ProgramCandidate) ois.readObject();
                        ois.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return pp;
    }

    public static ProgramCandidate retrieveProgramCandidate(String baseDir)
    {
        ProgramCandidate pp=null;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(selectNext)) {
            System.out.println("Preparing to retrieve the next candidate");

            pstmt.setString(1, baseDir);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {


                Blob fileName=result.getBlob    ("SERIALIZED_MUTATION_CHAIN");

                try {
                    ObjectInputStream ois = new ObjectInputStream(fileName.getBinaryStream());
                    pp = (ProgramCandidate) ois.readObject();
                    ois.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return pp;
    }

    public static OriginalTestResult readOriginalTestResult(String baseDir, String testName)
    {
        OriginalTestResult origResult=null;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(selectOriginalResults)) {
            System.out.println("Preparing to read original test information for "+baseDir+" | "+testName);

            pstmt.setString(1, baseDir);
            pstmt.setString(2, testName);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {


                Blob fileName=result.getBlob    ("SERIALIZED_DATA");

                try {
                    ObjectInputStream ois = new ObjectInputStream(fileName.getBinaryStream());
                    origResult = (OriginalTestResult) ois.readObject();
                    ois.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return origResult;
    }

    public static void getData(String programLocation)
    {
        ConfigurationItem config=null;
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(selectConfig)) {
            System.out.println("Preparing to read data");
            pstmt.setString(1, programLocation);
            ResultSet result = pstmt.executeQuery();

            while(result.next()) {

                System.out.println(result);
            }
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void clearControlConfig(String dbControl)
    {
        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
        Statement stmt = conn.createStatement();) {
            if(dbControl!=null && dbControl.equalsIgnoreCase("hard")) {
                stmt.execute(dropControlTableSql);
                stmt.execute(dropChainControlTableSql);
            }
            else
            {
                stmt.execute(truncateControlTableSql);
                stmt.execute(truncateChainControlTableSql);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static int insertChainInfo(String baseDir, int overallIndex, List<String> previousChain) throws Exception {
        int result = -1;
        ProgramCandidate pp = new ProgramCandidate(previousChain, overallIndex, baseDir);

        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(insertChainInfoSql)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(pp);
            oos.flush();
            oos.close();
            System.out.println("Recording layer information");
            pstmt.setString(1, baseDir);
            pstmt.setString(2, pp.getMutationChain());
            pstmt.setInt(3, overallIndex);
            pstmt.setBlob(4, new ByteArrayInputStream(baos.toByteArray()));

            result = pstmt.executeUpdate();

            pstmt.close();
            conn.close();
        }
        return result;
    }

    public static int updateChainInfo(String baseDir, ProgramCandidate pp) throws Exception {
        int result = -1;

        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(updateChainControl)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(pp);
            oos.flush();
            oos.close();
            System.out.println("Recording layer information");
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, baseDir);
            pstmt.setString(3, pp.getMutationChain());
            result = pstmt.executeUpdate();

            pstmt.close();
            conn.close();
        }
        if(result<=0)
        {
            throw new SQLException("Database state changed. Execute again");
        }
        return result;
    }

    public static int updateChainInfo(String baseDir, int overallIndex, List<String> previousChain) throws Exception {
        int result = -1;
        ProgramCandidate pp = new ProgramCandidate(previousChain, overallIndex, baseDir);

        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
             PreparedStatement pstmt = conn.prepareStatement(updateChainControl)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(pp);
            oos.flush();
            oos.close();
            System.out.println("Recording layer information");
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, baseDir);
            pstmt.setString(3, pp.getMutationChain());
            result = pstmt.executeUpdate();

            pstmt.close();
            conn.close();
        }
        if(result<=0)
        {
            throw new SQLException("Database state changed. Execute again");
        }
        return result;
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

    public static int insertOriginalResult(String baseDir, OriginalTestResult originalResult)
    {
        int result=-1;



        try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);

             PreparedStatement pstmt = conn.prepareStatement(insertOriginalResultSql)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(originalResult);
            oos.flush();
            oos.close();
            int originalScore = originalResult.getResultScore().intValue();
            System.out.println("ORIGINALS' RAN/FAILED: " + originalResult.getRunCount() + "|" + originalResult.getFailure().size());
            pstmt.setString(1, baseDir);
            pstmt.setString(2, originalResult.getTestSetName());
            pstmt.setInt(3, originalScore);//==null?-1:originalResult);//original
            pstmt.setBlob(4, new ByteArrayInputStream(baos.toByteArray()));

            result = pstmt.executeUpdate();

            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }catch (Exception e) {
            System.out.println(e.getMessage());
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
                pstmt.setString(1, MutationSystem.SYSTEM_HOME);
                pstmt.setString(2, tr.getProgramLocation());
                pstmt.setString(3, tr.getTargetMutant());
                pstmt.setString(4, tr.getTestSetName());
                pstmt.setString(5, entry.getKey());
                pstmt.setInt(6, originalScore);//==null?-1:originalResult);//original
                pstmt.setBoolean(7, isCorrectnessEnhanced);
                pstmt.setBoolean(8, isRelativelyCorrect);
                pstmt.setInt(9, mutantScore);

                pstmt.setInt(10, originalResult.getRunCount());
                pstmt.setInt(11, entry.getValue().getRunCount());
                pstmt.setBoolean(12, entry.getValue().getRunCount()>=originalResult.getRunCount());

                pstmt.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
                pstmt.setString(14, tr.getComment().get(entry.getKey()));//Comment to be inserted

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
            if(!tables.contains("ORIGINALTESTRESULTS".toLowerCase())) {
                result = stmt.execute(createOriginalResultTableSql);
                System.out.println("SUCCESS in creating ORIGINALTESTRESULTS");
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
            if(!tables.contains("CHAINCONTROL".toLowerCase())) {
                result = stmt.execute(createChainControlTableSql);
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
