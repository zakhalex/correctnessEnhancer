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

import java.util.Vector;

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
	public int mutant_score = 0;
	private int mode = -1;// Type of results - 1-class, 2-traditional
	private String testSetName;//Name of the testset for which these results are applicable
	private String targetMutant;//Which mutant were they running on
	
	
	public void setMutants()
	{
		mutants = new Vector<String>();
	}

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
}
