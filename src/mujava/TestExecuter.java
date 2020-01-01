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

package mujava;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.*;

import mujava.test.*;
import mujava.util.*;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import static mujava.util.DatabaseCalls.insertResult;

/**
 * <p>
 * Description:
 * </p>
 * 
 * @author Yu-Seung Ma
 * @update by Nan Li May 2012 integration with JUnit
 * @version 1.0
 */

public class TestExecuter
{
//	Object lockObject = new Object();
	LinkedHashMap<String, Future<Result>> resultMap=new LinkedHashMap<>();

	// int TIMEOUT = 3000;
	int TIMEOUT;
	private int NUMBER_OF_THREADS=1;
	final int MAX_TRY = 100;



	volatile Object mutant_result;

//	Class mutant_executer; // test set class for a mutant
//	volatile Object mutant_obj; // test set object for a mutant

	Method[] testCases;
	volatile Method testcase;
	private String targetClassName;//unqualified
	String whole_class_name;
	String testSet;
//	boolean mutantRunning = true;

	// original test results
	Map<String, String> originalResults = new HashMap<String, String>();
	// results for mutants
//	Map<String, String> mutantResults = null;
	// JUnit test cases
	List<String> junitTests = new ArrayList<String>();
	// result of a test case
//	Result result = null;
	// results as to how many mutants are killed by each test
	Map<String, String> finalTestResults = new HashMap<String, String>();
	// results as to how many tests can kill each single mutant
	Map<String, String> finalMutantResults = new HashMap<String, String>();

	public TestExecuter(String targetClassName)
	{
		int index = targetClassName.lastIndexOf(".");
		if (index < 0)
		{
			this.targetClassName=targetClassName;
		}
		else
		{
			this.targetClassName=targetClassName.substring(index + 1, targetClassName.length());
		}

		MutationSystem.setDirectory(targetClassName);
		whole_class_name = targetClassName;

	}

	public void setTimeOut(int msecs)
	{
		TIMEOUT = msecs;
	}

	public boolean readTestSet(String testSetName)
	{
		try
		{
			System.out.println("Loading TEST" + testSetName);
			// testSet = "test/"+testSetName;
			testSet = testSetName;
			// Class loader for the original class
			ClassLoader parentClassLoader = OriginalLoader.class.getClassLoader();
			OriginalLoader myLoader = new OriginalLoader(parentClassLoader);
//			System.out.println(testSet);

			Class original_executer = myLoader.loadTestClass(testSetName);
			System.out.println("Class received");
			try {
				Object original_obj = original_executer.newInstance(); // initialization of the test set class
				if (original_obj == null) {
					System.out.println("Can't instantiate original object");
//				return false;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			System.out.println("Original object successfully instantiated");
			// read testcases from the test set class
			testCases = original_executer.getDeclaredMethods();
			if (testCases == null)
			{
				System.out.println(" No test case exist ");
				return false;
			}
			System.out.println("Test cases retrieved.");
		}
		catch(Error err)
		{
			err.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;

	}

	boolean sameResult(Object result1, Object result2)
	{
		if (!(result1.toString().equals(result2.toString())))
			return false;
		return true;
	}

    public TestResult runClassMutants(String mutantPath) throws NoMutantException, NoMutantDirException
    {
        return runClassMutants(mutantPath, null);
    }

	public TestResult runClassMutants(String mutantPath, OriginalTestResult originalResult) throws NoMutantException, NoMutantDirException
	{
		TestResult test_result = new TestResult();
		runMutants(test_result, "", mutantPath, originalResult);
		test_result.setMode(1);
		if(MutationSystem.testOutputMode.equalsIgnoreCase("database"))
		{
			insertResult(test_result);
		}
		return test_result;
	}

    public TestResult runExceptionMutants(String mutantPath) throws NoMutantException, NoMutantDirException
    {
        return runExceptionMutants(mutantPath, null);
    }

	public TestResult runExceptionMutants(String mutantPath, OriginalTestResult originalResult) throws NoMutantException, NoMutantDirException
	{
		TestResult test_result = new TestResult();
		runMutants(test_result, "", mutantPath, originalResult);
		if(MutationSystem.testOutputMode.equalsIgnoreCase("database"))
		{
			insertResult(test_result);
		}
		return test_result;
	}

    public TestResult runTraditionalMutants(String methodSignature, String mutantPath) throws NoMutantException, NoMutantDirException
    {
        return runTraditionalMutants(methodSignature,mutantPath, null);
    }

	public TestResult runTraditionalMutants(String methodSignature, String mutantPath, OriginalTestResult originalResult) throws NoMutantException, NoMutantDirException
	{

		TestResult test_result = new TestResult();
		
		if (methodSignature.equals("All method"))
		{
			try
			{
				// setMutantPath();
				// computeOriginalTestResults();
				File f = new File(mutantPath, "method_list");
				FileReader r = new FileReader(f);
				BufferedReader reader = new BufferedReader(r);
				String readSignature = reader.readLine();
				while (readSignature != null)
				{
					try
					{
						runMutants(test_result, readSignature, mutantPath, originalResult);
					}
					catch (NoMutantException e)
					{
					}
					readSignature = reader.readLine();
				}
				reader.close();
			}
			catch (Exception e)
			{
				System.err.println("[WARNING] A problem occurred when running the traditional mutants:");
				System.err.println();
				e.printStackTrace();
			}
		}
		else
		{
			runMutants(test_result, methodSignature, mutantPath, originalResult);
		}
		test_result.setMode(2);
		if(MutationSystem.testOutputMode.equalsIgnoreCase("database"))
		{
			insertResult(test_result);
		}
		return test_result;
	}

	/**
	 * get the result of the test under the mutanted program
	 * 
	 * @deprecated
	 * @param mutant
	 * @param testcase
	 * @throws InterruptedException
	 */

	void runMutants(Object mutant, Method testcase) throws InterruptedException
	{
//		mutantRunning = true;
		try
		{
			// testcase execution
			mutant_result = testcase.invoke(mutant, null);
		}
		catch (Exception e)
		{
			// execption occurred -> abnormal execution
			mutant_result = e.getCause().getClass().getName() + " : " + e.getCause().getMessage();
		}
//		mutantRunning = false;
//		synchronized (lockObject)
//		{
//			lockObject.notify();
//		}
		// throw new InterruptedException();
	}

	synchronized void waitUntilAtLeast(long timeOut) throws InterruptedException
	{
		wait(timeOut);
	}

	/**
	 * get the mutants for one method based on the method signature
	 * 
	 * @param methodSignature
	 * @return
	 * @throws NoMutantDirException
	 * @throws NoMutantException
	 */
	private String[] getMutants(String methodSignature, String mutantPath) throws NoMutantDirException, NoMutantException
	{

		// Read mutants
		File f = new File(mutantPath,methodSignature);

		if (!f.exists())
		{
			System.err.println(" The directory "+mutantPath+" doesn't exist.");
			System.err.println(" There is no directory for the mutants of " + whole_class_name);
			System.err.println(" Please generate mutants for " + whole_class_name);
			throw new NoMutantDirException();
		}
		
		// mutantDirectories match the names of mutants
		String[] mutantDirectories = f.list(new MutantDirFilter());
		if (mutantDirectories == null || mutantDirectories.length == 0)
		{
			if (!methodSignature.equals(""))
				System.err.println(" No mutants have been generated for the method " + methodSignature + " of the class "
						+ whole_class_name);
			else
				System.err.println(" No mutants have been generated for the class " + whole_class_name);
			// System.err.println(" Please check if zero mutant is correct.");
			// throw new NoMutantException();
		}

		return mutantDirectories;
	}

	/**
	 * compute the result of a test under the original program
	 */
	public OriginalTestResult computeOriginalTestResults(String testSetName)
	{
		OriginalTestResult originalTestResult=new OriginalTestResult();
		Integer resultingScore=-1;
	    ConcurrentHashMap<String, Integer> localOriginalResult=new ConcurrentHashMap<>();
		Debug.println(
				"\n\n======================================== Generating Original Test Results ========================================");
		try
		{
			// initialize the original results to "pass"
			// later the results of the failed test cases will be updated
			for (int k = 0; k < testCases.length; k++)
			{
				Annotation[] annotations = testCases[k].getDeclaredAnnotations();//System.out.println("CONTROL: "+testCases[k].toString()+"|"+annotations.length);
				if ((testCases[k].toString().indexOf("junit.framework.Test")!=-1)&&(testCases[k].getName().indexOf("suite")!=-1))
				{//Junit3
					originalResults.put(testCases[k].getName(), "pass");
					localOriginalResult.put(testCases[k].getName(), 100);
					junitTests.add(testCases[k].getName());
					finalTestResults.put(testCases[k].getName(), "");
					continue;
				}
				for (Annotation annotation : annotations)
				{
					// System.out.println("name: " + testCases[k].getName() + annotation.toString() + annotation.toString().indexOf("@org.junit.Test"));
					if (annotation.toString().indexOf("@org.junit.Test") != -1)
					{
						// killed_mutants[k]= ""; // At first, no mutants are killed by each test case
						originalResults.put(testCases[k].getName(), "pass");
                        localOriginalResult.put(testCases[k].getName(), 100);
						junitTests.add(testCases[k].getName());
						finalTestResults.put(testCases[k].getName(), "");
						break;
					}
				}
			}
			ClassLoader parentClassLoader = OriginalLoader.class.getClassLoader();
			OriginalLoader myLoader = new OriginalLoader(parentClassLoader);
//			System.out.println(testSet);

			Class original_executor = myLoader.loadTestClass(testSetName);
			JUnitCore jCore = new JUnitCore();
			// result = jCore.runMain(new RealSystem(), "VMTEST1");
			if(MutationSystem.debugOutputEnabled) {
				jCore.addListener(new TextListener(System.out));
			}
			Result result = jCore.run(original_executor);
			// get the failure report and update the original result of the test with the failures
			List<Failure> listOfFailure = result.getFailures();
			originalTestResult.setFailure(result.getFailures());
			originalTestResult.setRunCount(result.getRunCount());
			if(result.getRunCount() > 0)
			{
				resultingScore=(result.getRunCount()-result.getFailureCount())*100/result.getRunCount();//correctness percentage
//				if(testSetName.contains("BooleanLiteralSetTest")) {
//					if (result.wasSuccessful()) {
//						System.out.println("DEBUGGING INFORMATION: " + result.getRunTime() + "|" + result.getRunCount() + "|" + result.getFailureCount() + "|" + result.getIgnoreCount() + "|" + result.getFailures().size());
//					}
//					else
//					{
//						System.out.println("WARNING - Got something!!!");
//					}
//
//					for (Failure failure : listOfFailure)
//					{
//						System.out.println(failure.getMessage());
//					}
//					System.exit(0);
//				}
			}
			for (Failure failure : listOfFailure)
			{
				String nameOfTest = failure.getTestHeader().substring(0, failure.getTestHeader().indexOf("("));
				String testSourceName = testSet + "." + nameOfTest;

				// System.out.println("failure message: " + failure.getMessage() + failure.getMessage().equals(""));
				String[] sb = failure.getTrace().split("\\n");
				String lineNumber = "";
				for (int i = 0; i < sb.length; i++)
				{
					if (sb[i].indexOf(testSourceName) != -1)
					{
						lineNumber = sb[i].substring(sb[i].indexOf(":") + 1, sb[i].indexOf(")"));
					}
				}
                localOriginalResult.put(nameOfTest, 0);
				if(resultingScore==100)
				{
					System.err.println("WE HAVE A PROBLEM");
					resultingScore=-2;
				}
				// put the failure messages into the test results
				if (failure.getMessage() == null)
					originalResults.put(nameOfTest, nameOfTest + ": " + lineNumber + "; " + "fail");
				else
				{
					if (failure.getMessage().equals(""))
						originalResults.put(nameOfTest, nameOfTest + ": " + lineNumber + "; " + "fail");
					else
						originalResults.put(nameOfTest, nameOfTest + ": " + lineNumber + "; " + failure.getMessage());
				}

			}
			if(MutationSystem.debugOutputEnabled) {
				System.out.println(originalResults.toString());
			}

			// System.out.println(System.getProperty("user.dir"));
			// System.out.println(System.getProperty("java.class.path"));
			// System.out.println(System.getProperty("java.library.path"));

		}
		catch (NoClassDefFoundError e)
		{
			System.err.println(
					"Could not find one of the necessary classes for running tests. Make sure that .jar files for hamcrest and junit are in your classpath.");
			e.printStackTrace();
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();

			// original_results[k] = e.getCause().getClass().getName()+" : " +e.getCause().getMessage();
			// Debug.println("Result for " + testName + " : " +original_results[k] );
			// Debug.println(" [warining] " + testName + " generate exception as a result " );

			// ----------------------------------

		}
		finally
		{
			// originalResultFileRead();
		}
		originalTestResult.setResultScore(resultingScore);
        return originalTestResult;
    }
    private TestResult runMutants(TestResult tr, String methodSignature, String mutantPath) throws NoMutantException, NoMutantDirException
    {
        return runMutants(tr,methodSignature,mutantPath,null);
    }
	private TestResult runMutants(TestResult tr, String methodSignature, String mutantPath, OriginalTestResult originalResult) throws NoMutantException, NoMutantDirException
	{
		try
		{
			
			String[] mutantDirectories = getMutants(methodSignature, mutantPath);

			int mutant_num = mutantDirectories.length;
			tr.setMutants();
			tr.setProgramLocation(mutantPath);
			tr.setOriginalResult(originalResult);
			tr.setTestSetName(testSet);
			tr.setTargetMutant(whole_class_name);
			for (int i = 0; i < mutant_num; i++)
			{
				// set live mutnats
				tr.mutants.add(mutantDirectories[i]);
			}

			// result againg original class for each test case
			// Object[] original_results = new Object[testCases.length];
			// list of the names of killed mutants with each test case
			// String[] killed_mutants = new String[testCases.length];

			Debug.println(
					"\n\n======================================== Executing Mutants ========================================");
			
			ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);//new TimeoutThreadPoolExecutor(NUMBER_OF_THREADS, TIMEOUT, TimeUnit.MILLISECONDS);//Executors.newFixedThreadPool(32);
			for (int i = 0; i < tr.mutants.size(); i++)
			{
				try
				{
					// read the information for the "i"th live mutant					
					String mutant_name = tr.mutants.get(i).toString();
					finalMutantResults.put(mutant_name, "");
					ClassLoader parentClassLoader = JMutationLoader.class.getClassLoader();
					JMutationLoader mutantLoader = new JMutationLoader(parentClassLoader, mutantPath+File.separator+methodSignature, mutant_name);
					// mutantLoader.loadMutant();
					Class mutant_executer = mutantLoader.loadTestClass(testSet);
					Debug.println("We are loading " + testSet);
					try {
						Object mutant_obj = mutant_executer.newInstance();
						Debug.print("  " + mutant_name);
						Debug.println("TestExecutor executing mutant " + mutant_name);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					// Mutants are runned using Thread to detect infinite loop caused by mutation
					Callable<Result> c = new Callable<Result>()
					{
						public Result result;
						@Override
						public Result call() {
//							try
//							{
//								mutantRunning = true;

							// original test results
							HashMap<String, String> mutantResults = new HashMap<String, String>();
							for (int k = 0; k < testCases.length; k++) {
								Annotation[] annotations = testCases[k].getDeclaredAnnotations();
								for (Annotation annotation : annotations) {
									// System.out.println("name: " + testCases[k].getName() + annotation.toString() +
									// annotation.toString().indexOf("@org.junit.Test"));
									if (annotation.toString().indexOf("@org.junit.Test") != -1) {
										// killed_mutants[k]= ""; // At first, no mutants are killed by each test case
										mutantResults.put(testCases[k].getName(), "pass");
										continue;
									}
								}
							}
							Thread.UncaughtExceptionHandler h = (th, ex) -> {
								System.err.println("Uncaught exception: " + ex);
								//System.exit(0);
							};
							Runnable task = () -> {
								JUnitCore jCore = new JUnitCore();
								if(MutationSystem.debugOutputEnabled) {
									jCore.addListener(new TextListener(System.out));
								}
								result = jCore.run(mutant_executer);
							};
							Thread t = new Thread(task);
							t.setUncaughtExceptionHandler(h);
							t.start();
							try {
								t.join(TIMEOUT);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							t.stop();
							if (result == null)
							{
								//Timeout occured
								return null;
							}
							else if (result.getFailureCount() == 0) {
								return result;
							} else {
								List<Failure> listOfFailure = result.getFailures();
								for (Failure failure : listOfFailure) {
									String header=failure.getTestHeader();
									int endIndex=header.indexOf("(");
									String nameOfTest = (endIndex!=-1)?header.substring(0,
											endIndex):header;
									String testSourceName = testSet + "." + nameOfTest;

									// System.out.println(testSourceName);
									String[] sb = failure.getTrace().split("\\n");
									String lineNumber = "";
									for (int i = 0; i < sb.length; i++) {
										// System.out.println("sb-trace: " + sb[i]);
										if (sb[i].indexOf(testSourceName) != -1) {
											lineNumber = sb[i].substring(sb[i].indexOf(":") + 1,
													sb[i].indexOf(")"));

										}
									}
									// get the line where the error happens
									/*
									 * String tempLineNumber = "";
									 * if(failure.getTrace().indexOf(testSourceName) != -1){
									 * tempLineNumber = failure.getTrace().substring(failure.getTrace().indexOf(testSourceName) + testSourceName.length() +
									 * 1, failure.getTrace().indexOf(testSourceName) + testSourceName.length() + 5);
									 * System.out.println("tempLineNumber: " + tempLineNumber);
									 * lineNumber = tempLineNumber.substring(0, tempLineNumber.indexOf(")"));
									 * //System.out.print("LineNumber: " + lineNumber);
									 * }
									 */
									// get the test name that has the error and save the failure info to the results for mutants
									if (failure.getMessage() == null)
										mutantResults.put(nameOfTest,
												nameOfTest + ": " + lineNumber + "; " + "fail");
									else if (failure.getMessage().equals(""))
										mutantResults.put(nameOfTest,
												nameOfTest + ": " + lineNumber + "; " + "fail");
									else {
										StringWriter sw = new StringWriter();
										PrintWriter pw = new PrintWriter(sw);
										failure.getException().printStackTrace(pw);
										mutantResults.put(nameOfTest,
												nameOfTest + ": " + lineNumber + "; " + sw.toString());// failure.getMessage());
									}
								}
							}
//								System.out.println(mutantResults.toString());
//								mutantRunning = false;
//								synchronized (lockObject)
//								{
//									lockObject.notify();
//								}
							return result;
//							}
//							catch (Exception e)
//							{
//								e.printStackTrace();
//								// System.out.println("e.getMessage()");
//								// System.out.println(e.getMessage());
//							}
//							return null;
						}

					};

					resultMap.put(mutant_name, executor.submit(c));
//					Thread t = new Thread(r);
//					t.start();
//
//					synchronized (lockObject)
//					{
//						lockObject.wait(TIMEOUT); // Check out if a mutant is in infinite loop
//					}
//					if (mutantRunning)
//					{
//						// System.out.println("check point4");
//						t.interrupt();
//						// mutant_result = "time_out: more than " + TIMEOUT + " seconds";
//						System.out.println(" time_out: more than " + TIMEOUT + " milliseconds");
//						// mutantResults.put(nameOfTest, nameOfTest + ": " + lineNumber + "; " + failure.getMessage());
//
//						for (int k = 0; k < testCases.length; k++)
//						{
//							Annotation[] annotations = testCases[k].getDeclaredAnnotations();
//							for (Annotation annotation : annotations)
//							{
//								// System.out.println("name: " + testCases[k].getName() + annotation.toString() +
//								// annotation.toString().indexOf("@org.junit.Test"));
//								if (annotation.toString().indexOf("@org.junit.Test") != -1)
//								{
//									// killed_mutants[k]= ""; // At first, no mutants are killed by each test case
//									mutantResults.put(testCases[k].getName(),
//											"time_out: more than " + TIMEOUT + " milliseconds");
//									continue;
//								}
//							}
//						}
//
//					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					mutant_result = e.getCause().getClass().getName() + " : " + e.getCause().getMessage();
				}
//				if(resultMap.get(mutant_name).isEmpty())
//				{
//					tr.live_mutants.add(mutant_name);
//				}
//				else
//				{
//					tr.killed_mutants.add(mutant_name);
//				}
//				boolean sign = false;
//				for (int k = 0; k < junitTests.size(); k++)
//				{
//					String name = junitTests.get(k);
//					if (!mutantResults.get(name).equals(originalResults.get(name)))
//					{
//						sign = true;
//						// update the final results by tests
//						if (finalTestResults.get(name).equals(""))
//							finalTestResults.put(name, mutant_name);
//						else
//							finalTestResults.put(name, finalTestResults.get(name) + ", " + mutant_name);
//						// update the final results by mutants
//						if (finalMutantResults.get(mutant_name).equals(""))
//							finalMutantResults.put(mutant_name, name);
//						else
//							finalMutantResults.put(mutant_name, finalMutantResults.get(mutant_name) + ", " + name);
//					}
//				}
//				if (sign == true)
//					tr.killed_mutants.add(mutant_name);
//				else
//					tr.live_mutants.add(mutant_name);

//				mutantLoader = null;
//				mutant_executer = null;
				System.gc();
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			// determine whether a mutant is killed or not
			// update the test report

			for(Entry<String, Future<Result>> entry:resultMap.entrySet())
			{
				OriginalTestResult originalTestResult=new OriginalTestResult();
				String mutant_name=entry.getKey();
				if(MutationSystem.debugOutputEnabled) {
					System.out.println(mutant_name);
				}
				try
				{
					Result r=entry.getValue().get();
					try {
						originalTestResult.setRunCount(r.getRunCount());
						originalTestResult.setFailure(r.getFailures());
						if(r.getRunCount()>0)
						{
							originalTestResult.setResultScore((r.getRunCount()-r.getFailureCount())*100/r.getRunCount());
//						tr.live_mutants.add(mutant_name);
						}
						else
						{
							originalTestResult.setResultScore(-1);
//						tr.killed_mutants.add(mutant_name);
						}
					}
					catch (NullPointerException e)
					{
						originalTestResult.setResultScore(-1);
						originalTestResult.setRunCount(-1);
						originalTestResult.setFailure(new ArrayList<>());

					}
					tr.mutation_results.put(mutant_name,originalTestResult);//correctness percentage
				}
				catch(Exception e)
				{
					e.printStackTrace();
					tr.killed_mutants.add(mutant_name);
				}
			}
			if(MutationSystem.debugOutputEnabled) {
				System.out.println("DONE");
			}
			resultMap.clear();
			for (int i = 0; i < tr.killed_mutants.size(); i++)
			{
				if(tr.live_mutants.remove(tr.killed_mutants.get(i)))
				{
					System.out.println("THIS IS A TEST - killed mutant detected among live: "+tr.killed_mutants.get(i));
//					System.exit(0);
				}
			}
			/*
			 * System.out.println(" Analysis of testcases ");
			 * for(int i = 0;i < killed_mutants.length;i++){
			 * System.out.println("  test " + (i+1) + "  kill  ==> " + killed_mutants[i]);
			 * }
			 */
		}
		catch (NoMutantException e1)
		{
			throw e1;
		}
		catch (NoMutantDirException e2)
		{
			throw e2;
		}
		/*
		 * catch(ClassNotFoundException e3){
		 * System.err.println("[Execution 1] " + e3);
		 * return null;
		 * }
		 */catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		if(MutationSystem.debugOutputEnabled) {
			System.out.println("test report: " + finalTestResults);
			System.out.println("mutant report: " + finalMutantResults);
		}
		return tr;
	}

	void erase_killed_mutants(Vector v, String mutantPath)
	{
		System.out.println("Deleting directories of killed mutants");
		for (int i = 0; i < v.size(); i++)
		{
			System.out.print(v.get(i).toString() + " ");
			erase_directory(v.get(i).toString(), mutantPath);
		}
	}

	void erase_directory(String mutant_name, String mutantPath)
	{
		File mutant_dir = new File(mutantPath + "/" + mutant_name);
		File[] f = mutant_dir.listFiles();
		boolean flag = false;
		for (int i = 0; i < f.length; i++)
		{
			while (!flag)
			{
				flag = f[i].delete();
			}
			flag = false;
		}

		while (!flag)
		{
			flag = mutant_dir.delete();
		}
	}

	public void setNumberOfThreads(int parseInt)
	{
		NUMBER_OF_THREADS=parseInt;		
	}
}
