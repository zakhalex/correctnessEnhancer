/**
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mujava.test;

import mujava.util.JacocoTestResult;
import mujava.util.OriginalTestResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Description:
 * </p>
 * 
 * @author Yu-Seung Ma
 * @update by Nan Li May 2012
 * @version 1.0
 */

public class TestResult
{
	// all mutants in a class
	public Vector<String> mutants = new Vector<String>();
	// killed mutants in a class
	public Vector<String> killed_mutants = new Vector<String>();
	// live mutants in a class
	public Vector<String> live_mutants = new Vector<String>();//fixes that worked

    // mutation score
	public final ConcurrentHashMap<String, OriginalTestResult> mutation_results=new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String,JacocoTestResult> instrumentedTestResultsMap = new ConcurrentHashMap<>();
//    public final ConcurrentHashMap<String, Integer> originalResults=new ConcurrentHashMap<>();
    private OriginalTestResult originalResult=null;

	private int mode = -1;// Type of results - 1-class, 2-traditional
	private String testSetName;//Name of the testset for which these results are applicable
	private String targetMutant;//Which mutant were they running on
	private String programLocation="DEFAULT";

	public HashMap<String, String> getComment() {
		return comment;
	}

	final private HashMap<String,String> comment = new HashMap<String, String>();
	
	public void setMutants()
	{
		mutants = new Vector<String>();
	}
    public OriginalTestResult getOriginalResult()
    {
        return originalResult;
    }

    public void setOriginalResult(OriginalTestResult newOriginalResult)
    {
        originalResult=newOriginalResult;
    }
//	public void setOriginalResults(Map<String, Integer> newOriginalResults)
//    {
//    	if(newOriginalResults!=null) {
//			originalResults.putAll(newOriginalResults);
//		}
//		else
//		{
//			originalResults.clear();
//		}
//    }

	public int getMode()
	{
		return mode;
	}

	public void setMode(int mode)
	{
		this.mode = mode;
	}

	public String getTestSetName()
	{
		return testSetName;
	}

	public void setTestSetName(String testSetName)
	{
		this.testSetName = testSetName;
	}

	public String getTargetMutant()
	{
		return targetMutant;
	}

	public void setTargetMutant(String targetMutant)
	{
		this.targetMutant = targetMutant;
	}

	public String getProgramLocation()
	{
		return programLocation;
	}

	public void setProgramLocation(String programLocation)
	{
		this.programLocation = programLocation;
	}

	public JacocoTestResult getInstrumentedTestResult(String mutantName)
	{
		return instrumentedTestResultsMap.get(mutantName);
	}

	public void putInstrumentedTestResult(String mutantName, JacocoTestResult jacocoTestResult)
	{
		instrumentedTestResultsMap.put(mutantName, jacocoTestResult);
	}
}
