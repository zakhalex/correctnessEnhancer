package mujava.util;

import mujava.MutationSystem;

import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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
                    else if(MutationSystem.stopOnAbsolutelyCorrect&&pp.getOverallIndex()%1000000==100100)
                    {
                        //Candidate is absolutely correct
                        break;
                    }
                    // Now it can be sent for processing.
                    createMutationLayerDirs(pp.getBaseDir(),pp.getChain().size()+pp.getMutationChain());
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

    private static void createMutationLayerDirs(String baseDir,String geneticMarker)
    {

        String SRC_PATH = baseDir + File.separator + geneticMarker + File.separator + "src";
        String CLASS_PATH = baseDir + File.separator + geneticMarker + File.separator + "classes";
        String MUTANT_HOME = baseDir + File.separator + geneticMarker + File.separator + "result";
        String TESTSET_PATH = baseDir + File.separator + geneticMarker + File.separator + "testset";
        String CHAIN_PATH = baseDir + File.separator + geneticMarker + File.separator + "mutationchain";

    }
}
