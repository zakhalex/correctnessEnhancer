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


import com.google.common.collect.Sets;
import mujava.MutationControl;
import mujava.MutationSystem;
import mujava.test.TestResult;
import mujava.util.ConfigurationItem;
import mujava.util.DatabaseCalls;
import mujava.util.DecisionEngine;
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
            regularProperties=parseArgs(args);
            /*
             * Several key parameters:
             * mode: mutate||test||all
             * classOps: list of class operators
             * traditionalOps: list of traditional operators
             */
        } else {
//            regularProperties.put("mode", "all");
//            regularProperties.put("mode", "mutate");
//			regularProperties.put("mode", "test");
//            regularProperties.put("mode", "testcleanup");
//            regularProperties.put("mode", "list");
            regularProperties.put("mode", "analyze");
            regularProperties.put("configurationmode", "file");
            regularProperties.put("configurationpath", "mujava.config");
//            regularProperties.put("testfilter", "org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests");
//            regularProperties.put("mutationfilter", "org.jfree.chart.annotations.XYBoxAnnotation");
            regularProperties.put("datafilter", "Chart_1b");
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

        try {
            MutationSystem.recordInheritanceRelation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        modeSelector(mode,regularProperties);
    }

    /**
     * A generic reader of incoming configurations.
     * @param args - an array of properties in the format key=value
     */
    private static HashMap<String, String>  parseArgs(String[] args) {
        HashMap<String, String> regularProperties = new HashMap<>();
        boolean argsFound = false;
        for (String arg : args) {
            if (arg.contains("=")) {
                String[] localArgs = arg.split("=");
                if (localArgs.length == 2) {
                    argsFound = true;
                    regularProperties.put(localArgs[0].toLowerCase(), localArgs[1]);
//                    System.out.println(localArgs[0].toLowerCase()+"|"+localArgs[1]);
                }
            }
        }
//        System.exit(0);
        if (!argsFound) {
            System.out.println("Arguments not provided");
        }
        return regularProperties;
    }

    /**
     * The mapping between the actions the program will perform and the mode that was selected
     * @param mode
     * @param regularProperties
     */
    public static void modeSelector(String mode,Map<String, String> regularProperties)
    {
        if (mode.equalsIgnoreCase("create")) {
            try {
                DriverManager.registerDriver(new org.apache.derby.jdbc.ClientDriver());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            DatabaseCalls.createResultTable();
            DatabaseCalls.createControlTable();
        }
        if (mode.equalsIgnoreCase("list")) {
            listModeTriggered();
        }
        if (mode.equalsIgnoreCase("all") || mode.equalsIgnoreCase("mutate")) {
            mutationModeTriggered(regularProperties);
        }
        if (mode.equalsIgnoreCase("all") || mode.equalsIgnoreCase("test") || mode.equalsIgnoreCase("pretest"))// not optimal, should be re-done through loop with list
        {
            validationModeTriggered(regularProperties,mode.equalsIgnoreCase("pretest"));
        }
        if (mode.equalsIgnoreCase("controlcleanup")) {
            DatabaseCalls.clearControlConfig(regularProperties.get("dbcontrol"));
        }
        if (mode.equalsIgnoreCase("analyze")) {
            DatabaseCalls.getData(regularProperties.get("datafilter"));
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
        if(mode.equalsIgnoreCase("autoguide"))
        {
            autoGuideModeTriggered(regularProperties);
        }
    }

    /**
     * System pre-heat. Once run on a new project generates basic information about it that is used in further operations.
     */
    public static void listModeTriggered()
    {
        HashMap<String,Set<String>> modeTypes=new HashMap<>();

        String listTargetMutationFiles = MutationSystem.listTargetMutationFiles;
        if ((listTargetMutationFiles != null) && (!listTargetMutationFiles.isEmpty())) {
            Collection<String> file_list = MutationSystem.getNewTragetFiles();
            modeTypes.put(MutationControl.Inputs.MUTANTS.getLabel(),new HashSet<String>(file_list));
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
            modeTypes.put(MutationControl.Inputs.TESTS.getLabel(),new HashSet<String>(Arrays.asList(file_list)));
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

        try {
            if ((MutationSystem.testOutputMode.equalsIgnoreCase("database"))||(MutationSystem.testJdbcURL != null)) {
                try {
                    DriverManager.registerDriver(new org.apache.derby.jdbc.ClientDriver());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                DatabaseCalls.createResultTable();
                DatabaseCalls.createControlTable();
                ArrayList<ConfigurationItem> localList = new ArrayList<>();


                if(modeTypes.size()>0)
                {
                    //We need to leave a marker on db records
                    if(!modeTypes.containsKey(MutationControl.Inputs.FILES.getLabel())) {
                        HashSet<String> fileSet=new HashSet<>();
                        fileSet.add(MutationSystem.databaseMarker);
                        modeTypes.put(MutationControl.Inputs.FILES.getLabel(), fileSet);
                    }
                    MutationControl.Inputs[] valuesArray = MutationControl.Inputs.values();
                    List<Set<String>> cartesianInput=new ArrayList<>();
                    for(MutationControl.Inputs s:valuesArray)
                    {
                        if(!modeTypes.containsKey(s.getLabel()))
                        {
                            HashSet<String> fillerList=new HashSet<String>();
                            fillerList.add("");
                            modeTypes.put(s.getLabel(),fillerList);
                            cartesianInput.add(fillerList);
                        }
                        else
                        {
                            cartesianInput.add(modeTypes.get(s.getLabel()));
                        }

                    }
                    Set<List<String>> product=Sets.cartesianProduct(cartesianInput);

                    for (List<String> entry : product) {
                        HashMap<String, String> property=new HashMap<>();
                        for(int i=0;i<entry.size();i++) {
                            property.put(valuesArray[i].getLabel(),entry.get(i));
                        }
                        localList.add(new ConfigurationItem(property));
                    }
                    if (!localList.isEmpty()) {
                        DatabaseCalls.insertConfiguration(localList);
                    }
                }

            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * The generate portion of generate and validate
     * @param regularProperties - list of input parameters
     */
    public static void mutationModeTriggered(Map<String, String> regularProperties)
    {
        MutationControl m = new MutationControl(MutationSystem.numberOfMutationThreads);
        Collection<String> file_list = MutationSystem.getNewTragetFiles();
        String dbControl = regularProperties.get("dbcontrol");
        String filterOn=null;
        if (dbControl==null) {
            filterOn = regularProperties.get("mutationfilter");
        }
        else
        {
            //we have the process fully controlled from the database table
            filterOn = DatabaseCalls.readConfiguration(Integer.valueOf(dbControl)).getClassName();
        }
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

    /**
     * The validation mode
     * @param regularProperties - list of input parameters
     * @param pretest - whether this is the original test run or full test run
     */
    public static void validationModeTriggered(Map<String, String> regularProperties,boolean pretest)
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
        String filterOn = null;
        String dbControl = regularProperties.get("dbcontrol");
        ConfigurationItem item=null;
        if (dbControl==null) {
            filterOn  = regularProperties.get("testfilter");
        }
        else
        {
            System.out.println("Launch will be controlled by the database." +
                    " Extracting id "+dbControl+" for marker "+MutationSystem.databaseMarker);
            //we have the process fully controlled from the database table
            if(item==null)
            {
                item=DatabaseCalls.readConfiguration(Integer.valueOf(dbControl));
            }
            filterOn = item.getTestName();
        }
        System.out.println("FilterOn is "+filterOn);
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

        ArrayList<String> targetClassSet = null;
        String filterOnClass = regularProperties.get("mutationfilter");
        if (dbControl==null) {
            filterOnClass  = regularProperties.get("mutationfilter");
        }
        else
        {
            //we have the process fully controlled from the database table
            if(item==null)
            {
                item=DatabaseCalls.readConfiguration(Integer.valueOf(dbControl));
            }
            filterOnClass = item.getClassName();
        }
        if (filterOnClass != null)
        {
            targetClassSet=new ArrayList<>();
            String filteredClass=MutationControl.classNameFromFile(filterOnClass);
            targetClassSet.add(filteredClass);
            if(MutationSystem.debugOutputEnabled)
            {
                System.out.println("Activating filter on "+filteredClass);
            }
        }
        ArrayList<TestResult> result = null;
        if( pretest)
        {
            result=localExecutor.executeTests(targetClassSet, testSet, "All method", 3, 10000);
        }
        else
        {
            result=localExecutor.executeTests(targetClassSet, testSet, "All method", 10000);
        }

        String fileName = MutationSystem.resultsOutput;
        /**
         * Console/File/Database
         */
        if ((MutationSystem.testOutputMode.equalsIgnoreCase("database"))||(MutationSystem.testJdbcURL != null)) {
            //Initial versions used to insert results into the database here, however, this is not needed anymore
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

    /**
     * Automatically go through mutations and validations
     * @param regularProperties - list of input parameters
     */
    public static void autoGuideModeTriggered(Map<String, String> regularProperties)
    {
        DecisionEngine.annealingControl(MutationSystem.SYSTEM_HOME,regularProperties);
    }
}
