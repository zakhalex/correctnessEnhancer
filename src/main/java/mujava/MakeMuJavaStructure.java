/**
 * Copyright (C) 2015  the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package mujava;

import java.io.*;

/**
 * <p>Description: </p>
 * @author Yu-Seung Ma
 * @version 1.0
 */

public class MakeMuJavaStructure {

    public static void main(String[] args) {
        deploy();
    }

    public static void deploy() {
        MutationSystem.setJMutationStructure();
        makeDir(new File(MutationSystem.SYSTEM_HOME));
        makeDir(new File(MutationSystem.SRC_PATH));
        makeDir(new File(MutationSystem.CLASS_PATH));
        makeDir(new File(MutationSystem.MUTANT_HOME));
        makeDir(new File(MutationSystem.TESTSET_PATH));
        makeDir(new File(MutationSystem.CHAIN_PATH));
    }

    public static void makeDir(File dir) {
        System.out.println("\nMake " + dir.getAbsolutePath() + " directory...");
        boolean newly_made = dir.mkdir();
        if (!newly_made) {
            System.out.println(dir.getAbsolutePath() + " directory exists already.");
        } else {
            System.out.println("Making " + dir.getAbsolutePath() + " directory " + " ...done.");
        }
    }
}
