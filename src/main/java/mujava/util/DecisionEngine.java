package mujava.util;

import mujava.MutationSystem;
import mujava.cli.ConsoleController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class DecisionEngine {

    private static Random rand = new Random();

    public static void annealingControl(String baseDir, PropertiesDictionary basePd, Map<String, String> regularProperties) {
        LinkedHashMap<String,PropertiesDictionary> baseDirs = new LinkedHashMap<>();
        baseDirs.put(baseDir,basePd);
        main_loop:
        while (!baseDirs.isEmpty()) {
            Iterator<Map.Entry<String, PropertiesDictionary>> it=baseDirs.entrySet().iterator();
            Map.Entry<String, PropertiesDictionary> localEntry=it.next();
            if(!localEntry.getKey().equalsIgnoreCase(baseDir))
            {
                //time to reload
                System.out.println("INFO Rebasing from "+baseDir+" to "+localEntry.getKey());
                MutationSystem.getDictionary().clearDictionary();
                MutationSystem.setJMutationStructure(localEntry.getValue().getPropertiesMap());
            }
            baseDir = localEntry.getKey();
            it.remove();
            internal_loop:
            while (true) {
                ProgramCandidate pp;
                if (MutationSystem.annealing > 0) {
                    //Annealing is on. With the probability provided in settings it will pull a random element
                    double chance = rand.nextDouble();
                    if (chance * 100 < MutationSystem.annealing) {
                        int count = DatabaseCalls.countCandidates(baseDir);
                        Double position = count * chance;
                        pp = DatabaseCalls.retrieveNthProgramCandidate(baseDir, position.intValue() + 1);//Because rownum starts from 1
                    } else {
                        pp = DatabaseCalls.retrieveProgramCandidate(baseDir);
                    }

                } else {
                    //annealing is off - just take the first value from the table
                    pp = DatabaseCalls.retrieveProgramCandidate(baseDir);
                }
                if (pp != null) {
                    PropertiesDictionary oldPd = new PropertiesDictionary();
                    oldPd.parseProperties(MutationSystem.getDictionary().getPropertiesMap());
                    try {
                        int updateCount = DatabaseCalls.updateChainInfo(baseDir, pp);
                        if (updateCount <= 0) {
                            throw new Exception("Concurrent modification detected. Reevaluating.");
                        }
                        //We have the candidate pp here.
                        if (MutationSystem.maxChainLength > 0 && pp.getChain().size() >= MutationSystem.maxChainLength) {
                            //Chain length limit reached - ignore the candidate
                            System.out.println("Chain length of "+pp.getChain().size()+" exceeds the limit. Skipping the candidate.");
                            continue;
                        } else if (MutationSystem.stopOnAbsolutelyCorrect && pp.getOverallIndex() == 100100100) {
                            //Candidate is absolutely correct
                            System.out.println("An absolutely correct candidate has been identified");
                            break main_loop;
                        }
                        // Now it can be sent for processing.
                        setMutationLayerDirs(MutationSystem.CHAIN_PATH, pp.getChain().size() + pp.getMutationChain(), pp);
                        PropertiesDictionary newPd = new PropertiesDictionary();
                        newPd.parseProperties(MutationSystem.getDictionary().getPropertiesMap());
                        baseDirs.put(MutationSystem.SYSTEM_HOME,newPd);//System is reset for next layer - adding it to the list
                        ConsoleController.listModeTriggered();
                        ConsoleController.modeSelector("all", regularProperties);
                        Map<String, Integer> indexMap = DatabaseCalls.retrieveOverallIndex(pp.getBaseDir());
                        recordMutants(MutationSystem.SYSTEM_HOME, MutationSystem.MUTANT_HOME, indexMap, pp.getChain());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        //reverting to the old ways
                        MutationSystem.getDictionary().clearDictionary();
                        MutationSystem.setJMutationStructure(oldPd.getPropertiesMap());
                    }
                } else {
                    break;//no additional candidates found, abort
                }
            }
        }
    }

    public static void recordMutants(String baseDir, String mutantDir, Map<String, Integer> indexMap, LinkedHashMap<String,String> chain) throws Exception
    {
        File newBaseDir=new File(mutantDir);
        for(Map.Entry<String, Integer> entry:indexMap.entrySet())
        {
            //Record a new mutant
            LinkedHashMap<String,String> localMap;
            if(chain!=null) {
                localMap = new LinkedHashMap<>(chain);
            }
            else
            {
                localMap=new LinkedHashMap<>();
            }
            String absolutePath=findDir(newBaseDir,entry.getKey());
            if(absolutePath==null)
            {
                throw new Exception("CRITICAL Unable to find the described mutant. Potentially the storage is not available");
            }
            localMap.put(absolutePath,entry.getKey());
            DatabaseCalls.insertChainInfo(baseDir,entry.getValue(),localMap);
        }
    }

    private static void setMutationLayerDirs(String baseDir, String geneticMarker,ProgramCandidate pp) throws Exception{
        String SYSTEM_HOME = baseDir + File.separator + geneticMarker;
        String SRC_PATH = SYSTEM_HOME + File.separator + "src";
        String CLASS_PATH = SYSTEM_HOME + File.separator + "classes";
        String MUTANT_HOME = SYSTEM_HOME + File.separator + "result";
        String TESTSET_PATH = SYSTEM_HOME + File.separator + "testset";
        String CHAIN_PATH = SYSTEM_HOME + File.separator + "mutationchain";
        String listTargetMutationFiles = SYSTEM_HOME + File.separator + "mujavaMutation.txt";
        String listTargetTestFiles = SYSTEM_HOME + File.separator + "mujavaTest.txt";
        String databaseCount = SYSTEM_HOME + File.separator + "dbcount.txt";
        try {
            copyDirectory(MutationSystem.SRC_PATH, SRC_PATH);
            copyDirectory(MutationSystem.CLASS_PATH, CLASS_PATH);
            File f1 = new File(MUTANT_HOME);
            f1.mkdirs();
            //copyDirectory(MutationSystem.MUTANT_HOME, MUTANT_HOME);
            copyDirectory(MutationSystem.TESTSET_PATH, TESTSET_PATH);
            File f2 = new File(CHAIN_PATH);
            f2.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
            //This is a critical exception for this mode of operation
            throw new Exception("CRITICAL Unable to create file structure");
        }
        //While the entire history of mutants is maintained, only the last one is needed
        Iterator<Map.Entry<String, String>> it=pp.getChain().entrySet().iterator();
        Map.Entry<String, String> localEntry=null;
        while (it.hasNext()) {
            localEntry = it.next();
        }
        if(localEntry!=null)
        {
            File f=new File(localEntry.getKey());
            System.out.println("INFO Scanning "+f.getAbsolutePath());
            File[] sourceCodeToCopy=f.listFiles((dir, name) -> name.toLowerCase().endsWith(".java"));
            for(File f1:sourceCodeToCopy)
            {
                String classPath=f1.getAbsolutePath().substring(MutationSystem.MUTANT_HOME.length());
                classPath=SRC_PATH+File.separator+classPath.substring(0,classPath.indexOf(File.separator)).replace('.',File.separatorChar);
                Path destination=Paths.get(classPath+File.separator+f1.getName());
                Files.copy(f1.toPath(), destination, REPLACE_EXISTING);
            }
            File[] compiledCodeToCopy=f.listFiles((dir, name) -> name.toLowerCase().endsWith(".class"));
            for(File f1:compiledCodeToCopy)
            {
                String classPath=f1.getAbsolutePath().substring(MutationSystem.MUTANT_HOME.length());
                classPath=MUTANT_HOME+File.separator+classPath.substring(0,classPath.indexOf(File.separator)).replace('.',File.separatorChar);
                Path destination=Paths.get(classPath+File.separator+f1.getName());
                Files.copy(f1.toPath(), destination, REPLACE_EXISTING);
            }
        }
        MutationSystem.getDictionary().setProperty("MuJava_Home", SYSTEM_HOME);
        MutationSystem.getDictionary().setProperty("MuJava_src", SRC_PATH);
        MutationSystem.getDictionary().setProperty("MuJava_class", CLASS_PATH);
        MutationSystem.getDictionary().setProperty("MuJava_mutants", MUTANT_HOME);
        MutationSystem.getDictionary().setProperty("MuJava_tests", TESTSET_PATH);
        MutationSystem.getDictionary().setProperty("MuJava_chain", CHAIN_PATH);
        MutationSystem.getDictionary().setProperty("List_Target_Mutation_Files", listTargetMutationFiles);
        MutationSystem.getDictionary().setProperty("List_Target_Tests", listTargetTestFiles);
        MutationSystem.getDictionary().setProperty("database_count", databaseCount);
        MutationSystem.systemRefresh();
    }

    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.createDirectories(destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Files.copy(source, destination, REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static String findDir(File root, String name) {
        if (root.getName().equals(name)) {
            return root.getAbsolutePath();
        }

        File[] files = root.listFiles();

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    String firstMatch = findDir(f, name);
                    if (firstMatch != null) {
                        return firstMatch;
                    }
                }
            }
        }
        return null;
    }

    public static <T> T removeFirst(Collection<? extends T> c) {
        Iterator<? extends T> it = c.iterator();
        if (!it.hasNext()) { return null; }
        T removed = it.next();
        it.remove();
        return removed;
    }
}
