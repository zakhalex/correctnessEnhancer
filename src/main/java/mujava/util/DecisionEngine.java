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

    public static void annealingControl(String baseDir,Map<String, String> regularProperties)
    {
        while(true) {
            ProgramCandidate pp;
            if (MutationSystem.annealing > 0) {
                //Annealing is on. With the probability provided in settings it will pull a random element
                double chance=rand.nextDouble();
                if(chance*100 < MutationSystem.annealing) {
                    int count = DatabaseCalls.countCandidates(baseDir);
                    Double position = count*chance;
                    pp = DatabaseCalls.retrieveNthProgramCandidate(baseDir, position.intValue());
                }
                else
                {
                    pp=DatabaseCalls.retrieveProgramCandidate(baseDir);
                }

            } else {
                //annealing is off - just take the first value from the table
                pp=DatabaseCalls.retrieveProgramCandidate(baseDir);
            }
            if(pp!=null)
            {
                try {
                    int updateCount = DatabaseCalls.updateChainInfo(baseDir, pp);
                    if (updateCount<=0) {
                        throw new Exception("Concurrent modification detected. Reevaluating.");
                    }
                    //We have the candidate pp here.
                    if(MutationSystem.maxChainLength>0 && pp.getChain().size()>=MutationSystem.maxChainLength)
                    {
                        //Chain length limit reached - ignore the candidate
                        continue;
                    }
                    else if(MutationSystem.stopOnAbsolutelyCorrect&&pp.getOverallIndex()==100100100)
                    {
                        //Candidate is absolutely correct
                        break;
                    }
                    // Now it can be sent for processing.
                    PropertiesDictionary oldPd=new PropertiesDictionary();
                    oldPd.parseProperties(MutationSystem.getDictionary().getPropertiesMap());
                    setMutationLayerDirs(pp.getBaseDir(),pp.getChain().size()+pp.getMutationChain());
                    ConsoleController.listModeTriggered();
                    ConsoleController.modeSelector("all",regularProperties);
                    Map<String, Integer> indexMap=DatabaseCalls.retrieveOverallIndex(pp.getBaseDir());
                    for(Map.Entry<String, Integer> entry:indexMap.entrySet())
                    {
                        //Record a new mutant
                        List<String> localList=new LinkedList<>(pp.getChain());
                        localList.add(entry.getKey());
                        DatabaseCalls.insertChainInfo(pp.getBaseDir(),entry.getValue(),localList);
                    }
                    MutationSystem.getDictionary().clearDictionary();
                    MutationSystem.setJMutationStructure(oldPd.getPropertiesMap());
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                break;//no additional candidates found, abort
            }
        }
    }

    private static void setMutationLayerDirs(String baseDir, String geneticMarker) throws Exception{
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
            copyDirectory(MutationSystem.CLASS_PATH, SRC_PATH);
            copyDirectory(MutationSystem.MUTANT_HOME, MUTANT_HOME);
            copyDirectory(MutationSystem.TESTSET_PATH, TESTSET_PATH);
            File f = new File(CHAIN_PATH);
            f.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
            //This is a critical exception for this mode of operation
            throw new Exception("CRITICAL Unable to create file structure");
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
}
