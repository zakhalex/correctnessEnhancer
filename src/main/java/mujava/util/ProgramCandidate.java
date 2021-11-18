package mujava.util;

import java.io.Serializable;
import java.util.*;

/**
 * This class stores information about the chain of mutants that were applied to reach the current level.
 * It stores the location of the mutated file as the key and the name of the mutant (e.g. SDL_4) as the value.
 */
public class ProgramCandidate implements Serializable {
    private final LinkedHashMap<String, String> chain;//The chain of mutations that had to be applied to get to the current point
    private final String mutationChain;//The names of mutators
    private final String baseDir;
    private final int overallIndex;

    public ProgramCandidate(Map<String,String> previousChain, int overallIndex, String baseDir) {
        chain = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : previousChain.entrySet()) {
            chain.put(entry.getKey(), entry.getValue());
            sb.append(entry.getValue());
        }
        this.mutationChain = sb.toString();
        this.overallIndex=overallIndex;
        this.baseDir=baseDir;
    }

    public ProgramCandidate(Map<String, String> previousChain, String mutationChain, int overallIndex, String baseDir) {
        chain = new LinkedHashMap<>(previousChain);
        this.mutationChain = mutationChain;
        this.overallIndex=overallIndex;
        this.baseDir=baseDir;
    }

    public LinkedHashMap<String, String> getChain() {
        return chain;
    }

    public String getMutationChain() {
        return mutationChain;
    }

    public int getOverallIndex() {
        return overallIndex;
    }

    public String getBaseDir() {
        return baseDir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramCandidate that = (ProgramCandidate) o;
        return overallIndex == that.overallIndex && Objects.equals(chain, that.chain) && Objects.equals(mutationChain, that.mutationChain) && Objects.equals(baseDir, that.baseDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chain, mutationChain, baseDir, overallIndex);
    }
}
