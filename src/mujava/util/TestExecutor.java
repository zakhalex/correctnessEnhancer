package mujava.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mujava.MutationSystem;
import mujava.TestExecuter;
import mujava.test.NoMutantDirException;
import mujava.test.NoMutantException;
import mujava.test.TestResult;

public class TestExecutor
{
	int numberOfThreads;
	public TestExecutor()
	{
		this.numberOfThreads=1;
	}
	public TestExecutor(int numberOfThreads)
	{
		this.numberOfThreads=numberOfThreads;
	}
	public ArrayList<TestResult> executeTests(Collection<String> targetClassSet, Collection<String> testSet,
			String methodSignature, int timeout)
	{
		return executeTests(targetClassSet,testSet,methodSignature,0,timeout);
	}
	/**
	 * This method runs tests on the original and mutants and compares the results.
	 * 
	 * @param targetClassSet
	 *            - the mutations of which class should be focused on
	 * @param testSet
	 *            - which tests to run.
	 * @param methodSignature
	 *            - the mutants for which method should be tested. If null or blank - all of them
	 * @param mode
	 *            - what type of runs to perform. 0: all of them, 1: only class, 2: only traditional
	 * @param timeout
	 *            - the timeout on test executors
	 * @return
	 */
	public ArrayList<TestResult> executeTests(Collection<String> targetClassSet, Collection<String> testSet,
			String methodSignature, int mode, int timeout)
	{
		// targetClassSet=null;
		if ((targetClassSet == null) || (targetClassSet.isEmpty()))
		{
			targetClassSet = Arrays.asList(new File(MutationSystem.MUTANT_HOME).list(new DirFileFilter()));
		}
		// testSet=null;
		if ((testSet == null) || (testSet.isEmpty()))
		{
			testSet = Arrays.asList(MutationSystem.eraseExtension(MutationSystem.getTestSetNames(), "class"));
		}
		if ((methodSignature == null) || (methodSignature.isEmpty()))
		{
			methodSignature = "All method";
		}
		ArrayList<TestResult> test_result = new ArrayList<TestResult>();
		for (String targetClassName : targetClassSet)
		{
			TestExecuter test_engine = new TestExecuter(targetClassName);
			test_engine.setTimeOut(timeout);
			test_engine.setNumberOfThreads(numberOfThreads);

			// First, read (load) test suite class.
			for (String testSetName : testSet)
			{
				boolean loadTestSet=test_engine.readTestSet(testSetName);
				if(!loadTestSet)
				{
					System.out.println("Executor block - no tests have been detected.");
					continue;
				}
				Map<String, Integer> originalResultsMap = test_engine.computeOriginalTestResults();
				Integer result;
				if (originalResultsMap.size() != 0)
				{
					result = 0;
					for (Map.Entry<String, Integer> entry : originalResultsMap.entrySet())
					{
						result += entry.getValue();
					}
					result /= originalResultsMap.size();
				}
				else
				{
					result = -1;
				}
				try
				{
					switch (mode)
					{
						case 0:
						{
							test_result.add(test_engine.runClassMutants(MutationSystem.MUTANT_HOME
	                                + "/" + targetClassName + "/" + MutationSystem.CM_DIR_NAME, result));
							test_result.add(test_engine.runTraditionalMutants(methodSignature.toString(), MutationSystem.MUTANT_HOME
	                                + "/" + targetClassName + "/" + MutationSystem.TM_DIR_NAME, result));
							break;
						}
						case 1:
							test_result.add(test_engine.runClassMutants(MutationSystem.MUTANT_HOME
	                                + "/" + targetClassName + "/" + MutationSystem.CM_DIR_NAME, result));
							break;
						case 2:
							test_result.add(test_engine.runTraditionalMutants(methodSignature.toString(), MutationSystem.MUTANT_HOME
	                                + "/" + targetClassName + "/" + MutationSystem.TM_DIR_NAME, result));
							break;
						default:
							break;
					}
				}
				catch (NoMutantException e1)
				{
				}
				catch (NoMutantDirException e2)
				{
				}
				catch (Exception e)
				{

				}
			}
		}
		return test_result;
	}
}
