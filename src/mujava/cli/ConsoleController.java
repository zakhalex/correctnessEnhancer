/*
  Copyright (C) 2015 the original author or authors.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

/*
  <p>
  Description: Generating mutants API for command line version
  </p>

  @author Lin Deng
 * @version 1.0
 */

package mujava.cli;


import mujava.MutationControl;
import mujava.MutationSystem;
import mujava.test.TestResult;
import mujava.util.DatabaseCalls;
import mujava.util.TestExecutor;

import java.io.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class ConsoleController {
    //	private static HashMap<String, ArrayList<String>> listProperties;
    private static HashMap<String, String> regularProperties = new HashMap<String, String>();

    public static void main(String[] args) {
        if (args.length != 0) {
            parseArgs(args);
            /*
             * Several key parameters:
             * mode: mutate||test||all
             * classOps: list of class operators
             * traditionalOps: list of traditional operators
             */
        } else {
            regularProperties.put("mode", "all");
//            regularProperties.put("mode", "mutate");

//			regularProperties.put("mode", "test");
//            regularProperties.put("mode", "testcleanup");
//            regularProperties.put("mode", "list");
            regularProperties.put("configurationmode", "file");
            regularProperties.put("configurationpath", "mujava.config");
//            regularProperties.put("testfilter", "DistanceCorrectableCaseTest");
//            regularProperties.put("mutationfilter", "org.jfree.chart.annotations.XYBoxAnnotation");
        }
        String mode = regularProperties.get("mode");
        if (mode == null) {
            mode = "all";
        }
        String configurationMode = regularProperties.get("configurationmode");
        if (configurationMode == null) {
            configurationMode = "file";
        }
        switch (configurationMode) {
            case "file":
                String configFile = regularProperties.get("configurationpath");
                if (configFile == null) {
                    configFile = "mujava.config";
                }
                MutationSystem.setJMutationStructure(configFile);
                break;
            case "inline":
                MutationSystem.setJMutationStructure(regularProperties);
                break;
            default:
                System.out.println("Unrecognized configuration mode");
                return;
        }
//		try {
//
//			JUnitCore jCore = new JUnitCore();
//			OriginalLoader myLoader = new OriginalLoader();
////			System.out.println(testSet);
//
//			Class original_executer = myLoader.loadTestClass("DistanceTestSuite");
//			System.err.println(original_executer+"|"+original_executer.getAnnotations());
//
//			// result = jCore.runMain(new RealSystem(), "VMTEST1");
//			jCore.addListener(new TextListener(System.out));
//			Result result = jCore.run(original_executer);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//		finally {
//			System.exit(0);
//		}
        try {
            MutationSystem.recordInheritanceRelation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mode.equalsIgnoreCase("create")) {
           DatabaseCalls.createResultTable();
        }
        if (mode.equalsIgnoreCase("list")) {
            String listTargetMutationFiles = MutationSystem.listTargetMutationFiles;
            if ((listTargetMutationFiles != null) && (!listTargetMutationFiles.isEmpty())) {
                Collection<String> file_list = MutationSystem.getNewTragetFiles();
                File f=new File(listTargetMutationFiles);
                f.getParentFile().mkdirs();

                try (PrintWriter pw = new PrintWriter(listTargetMutationFiles)) {
                    for (String file : file_list) {
                        pw.println(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    for (String file : file_list) {
                        System.out.println(file);
                    }
                }
            }

            String listTargetTestFiles = MutationSystem.listTargetTestFiles;
            if ((listTargetTestFiles != null) && (!listTargetTestFiles.isEmpty())) {
                String[] file_list = MutationSystem.eraseExtension(MutationSystem.getTestSetNames(), "class");
                File f=new File(listTargetTestFiles);
                f.getParentFile().mkdirs();
                try (PrintWriter pw = new PrintWriter(listTargetTestFiles)) {
                    for (String file : file_list) {
                        pw.println(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    for (String file : file_list) {
                        System.out.println(file);
                    }
                }
            }
        }
        if (mode.equalsIgnoreCase("all") || mode.equalsIgnoreCase("mutate")) {
            MutationControl m = new MutationControl(MutationSystem.numberOfMutationThreads);
            Collection<String> file_list = MutationSystem.getNewTragetFiles();
            String filterOn = regularProperties.get("mutationfilter");
            if (filterOn != null) {
                if (file_list.contains(filterOn)) {
                    file_list.clear();
                    file_list.add(filterOn);
                }
            }
            if (file_list != null) {
                m.performMutation(file_list);
            } else {
                System.out.println("No files to mutate");
            }
        }
        if (mode.equalsIgnoreCase("all") || mode.equalsIgnoreCase("test"))// not optimal, should be re-done through loop with list
        {
            if ((MutationSystem.testOutputMode.equalsIgnoreCase("database"))||(MutationSystem.testJdbcURL != null)) {
                try {
                    DriverManager.registerDriver(new org.apache.derby.jdbc.ClientDriver());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                DatabaseCalls.createResultTable();
            }
            boolean filter = false;
            TestExecutor localExecutor = new TestExecutor(MutationSystem.numberOfTestingThreads);
            String filterOn = regularProperties.get("testfilter");
            TreeSet<String> testSet = null;
            if (filterOn != null) {
                testSet = new TreeSet<>();
                testSet.addAll(Arrays.asList(MutationSystem.eraseExtension(MutationSystem.getTestSetNames(), "class")));
                if (testSet.contains(filterOn)) {
                    testSet.clear();
                    testSet.add(filterOn);
                    filter = true;
                }
            }
            ArrayList<TestResult> result = localExecutor.executeTests(null, testSet, "All method", 3000);
            String fileName = MutationSystem.resultsOutput;
            /**
             * Console/File/Database
             */
            if ((MutationSystem.testOutputMode.equalsIgnoreCase("database"))||(MutationSystem.testJdbcURL != null)) {

////                String sql = "INSERT INTO TESTRESULTS (PROGRAM_LOCATION,MUTANT_NAME," +
////                        "TEST_NAME,ORIGINAL,ORIGINAL_CORRECTNESS_RANGE,MUTATED," +
////                        "MUTATED_CORRECTNESS_RANGE,LAST_UPDATED,COMMENT) " +
////                        "VALUES(?,?,?,?,?,?,?,?,?)";
//                for (TestResult tr : result) {
//                    insertResult(tr);
////                    try (Connection conn = DriverManager.getConnection(MutationSystem.testJdbcURL);
////                         PreparedStatement pstmt = conn.prepareStatement(sql)) {
////                        pstmt.setString(1, tr.getProgramLocation());
////                        pstmt.setString(1, tr.getTargetMutant());
////                        pstmt.setString(1, tr.getTestSetName());
////                        pstmt.setBoolean(1, false);
////                        pstmt.setString(1, "N/A");
////                        pstmt.setBoolean(1, false);
////                        pstmt.setString(1, "N/A");
////
////                        pstmt.setDate(2, new Date(12));
////                        pstmt.setString(1, "");//Comment to be inserted
////
////                        pstmt.executeUpdate();
////                    } catch (SQLException e) {
////                        System.out.println(e.getMessage());
////                    }
//                }
            }
            else if ((MutationSystem.testOutputMode.equalsIgnoreCase("file"))||(fileName != null)) {
                if (filter) {
                    fileName = fileName + "_" + filterOn.replaceAll("/[^A-Za-z0-9]/", "");
                }
                File f=new File(fileName);
                f.getParentFile().mkdirs();
                try (PrintWriter pw = new PrintWriter(fileName)) {
                    for (TestResult tr : result) {
                        pw.println("Class name: " + tr.getTargetMutant() + ". Target test: " + tr.getTestSetName() + ". The following mutants are alive: " + tr.live_mutants);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    for (TestResult tr : result) {
                        System.out.println("Class name: " + tr.getTargetMutant() + ". Target test: " + tr.getTestSetName() + ". The following mutants are alive: " + tr.live_mutants);
                    }
                }
            }
            else
            {
                for (TestResult tr : result) {
                    System.out.println("Class name: " + tr.getTargetMutant() + ". Target test: " + tr.getTestSetName() + ". The following mutants are alive: " + tr.live_mutants);
                }
            }
        }
        if (mode.equalsIgnoreCase("testcleanup")) {
            String fileName = MutationSystem.resultsOutput;
            File f = new File(fileName);
            f.getParentFile().mkdirs();
            String temp;
            HashSet<File> cleanup=new HashSet<>();
            try (PrintWriter pw = new PrintWriter(fileName)) {
                for (File s : f.getParentFile().listFiles()) {
                    try {
                        if(s.equals(f))
                        {
                            continue;
                        }
                        cleanup.add(s);
                        BufferedReader br = new BufferedReader(new FileReader(s));

                        while ((temp = br.readLine()) != null) {
                            pw.println(temp);
                        }
                        br.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for(File s: cleanup)
            {
                try
                {
                    s.delete();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void parseArgs(String[] args) {
        boolean argsFound = false;
        for (String arg : args) {
            if (arg.contains("=")) {
                String[] localArgs = arg.split("=");
                if (localArgs.length == 2) {
                    argsFound = true;
                    regularProperties.put(localArgs[0].toLowerCase(), localArgs[1]);
                }
            }
        }
        if (!argsFound) {
            System.out.println("Arguments not provided");
        }
    }

}
