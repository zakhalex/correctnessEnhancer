package mujava;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MutationControl
{
	private static final int numberOfThreads = 1;

	public void performMutation(Collection<String> file_list)
	{
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		ArrayList<Future<ArrayList<String>>> futures = new ArrayList<>();
		for (String file_path : file_list)
		{
			// file_name = ABSTRACT_PATH - MutationSystem.SRC_PATH
			// For example: org/apache/bcel/Class.java
			MutationWorker mworker = new MutationWorker(file_path, MutationSystem.cm_operators,
					MutationSystem.tm_operators);
			futures.add(executor.submit(mworker));
		}
		executor.shutdown();
		try
		{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (Future<ArrayList<String>> future : futures)
		{
			try
			{
				for (String s : future.get())
				{
					System.out.println(s);
				}
			}
			catch (Exception e1)
			{
				System.out.println("CRITICAL - unable to execute");
				e1.printStackTrace();
			}
		}

	}

	public void performMutation(Collection<String> file_list, Collection<String> class_ops,
			Collection<String> traditional_ops)
	{
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		ArrayList<Future<ArrayList<String>>> futures = new ArrayList<>();
		for (String file_path : file_list)
		{
			// file_name = ABSTRACT_PATH - MutationSystem.SRC_PATH
			// For example: org/apache/bcel/Class.java
			MutationWorker mworker = new MutationWorker(file_path, class_ops, traditional_ops);
			futures.add(executor.submit(mworker));
		}
		executor.shutdown();
		try
		{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (Future<ArrayList<String>> future : futures)
		{
			try
			{
				for (String s : future.get())
				{
					System.out.println(s);
				}
			}
			catch (Exception e1)
			{
				System.out.println("CRITICAL - unable to execute");
				e1.printStackTrace();
			}
		}

	}

	private class MutationWorker implements Callable<ArrayList<String>>
	{
		private String file_name;
		private String[] class_ops;
		private String[] traditional_ops;

		public MutationWorker(String fileName, String[] class_ops, String[] traditional_ops)
		{
			this.file_name = fileName;
			this.class_ops = class_ops;
			this.traditional_ops = traditional_ops;
		}

		public MutationWorker(String fileName, Collection<String> class_ops, Collection<String> traditional_ops)
		{
			this.file_name = fileName;
			this.class_ops = class_ops != null ? class_ops.toArray(new String[class_ops.size()]) : null;
			this.traditional_ops = traditional_ops != null ? traditional_ops.toArray(new String[traditional_ops.size()])
					: null;
		}

		@Override
		public ArrayList<String> call() throws Exception
		{
			ArrayList<String> output = new ArrayList<String>();
			boolean failure = false;// Will be set to true if one of the catches below fires
			try
			{
				// System.out.println(i + " : " + file_name);
				// [1] Examine if the target class is interface or abstract class
				// In that case, we can't apply mutation testing.

				// Generate class name from file_name
				String temp = file_name.substring(0, file_name.length() - ".java".length());
				String class_name = "";

				for (int j = 0; j < temp.length(); j++)
				{
					if ((temp.charAt(j) == '\\') || (temp.charAt(j) == '/'))
					{
						class_name = class_name + ".";
					}
					else
					{
						class_name = class_name + temp.charAt(j);
					}
				}

				int class_type = MutationSystem.getClassType(class_name);

				if (class_type == MutationSystem.NORMAL)
				{ // do nothing?
				}
				else if (class_type == MutationSystem.MAIN)
				{
					output.add(" -- " + file_name + " class contains 'static void main()' method.");
					output.add("    Please note that mutants are not generated for the 'static void main()' method");
				}
				// Added on 1/19/2013, no mutants will be generated for a class having only one main method
				else if (class_type == MutationSystem.MAIN_ONLY)
				{
					output.add(" -- Class " + file_name
							+ " has only the 'static void main()' method and no mutants will be generated.");
					return output;
				}
				// else
				// {
				// switch (class_type)
				// {
				// case MutationSystem.INTERFACE :
				// System.out.println(" -- Can't apply because " + file_name+ " is 'interface' ");
				// break;
				// case MutationSystem.ABSTRACT :
				// System.out.println(" -- Can't apply because " + file_name+ " is 'abstract' class ");
				// break;
				// case MutationSystem.APPLET :
				// System.out.println(" -- Can't apply because " + file_name+ " is 'applet' class ");
				// break;
				// case MutationSystem.GUI :
				// System.out.println(" -- Can't apply because " + file_name+ " is 'GUI' class ");
				// break;
				// }
				// deleteDirectory();
				// continue;
				// }

				// [2] Apply mutation testing
				HashMap<String, String> allPaths=setMutationSystemPathFor(file_name);
				// File[] original_files = new File[1];
				// original_files[0] = new File(MutationSystem.SRC_PATH,file_name);

				File original_file = new File(MutationSystem.SRC_PATH, file_name);

				/*
				 * AllMutantsGenerator genEngine;
				 * genEngine = new AllMutantsGenerator(original_file,class_ops,traditional_ops);
				 * genEngine.makeMutants();
				 * genEngine.compileMutants();
				 */

				ClassMutantsGenerator cmGenEngine;

				// do not generate class mutants if no class mutation operator is selected
				if (class_ops != null)
				{
					try
					{
						cmGenEngine = new ClassMutantsGenerator(original_file, class_ops);
						cmGenEngine.makeMutants(allPaths.get("originalpath"),allPaths.get("classmutantpath"),allPaths.get("qualifiedName"));
						cmGenEngine.compileMutants(allPaths.get("classmutantpath"));
					}
					catch (OpenJavaException oje)
					{
						failure = true;
						output.add("[OJException] " + file_name + " " + oje.toString());
						// System.out.println("Can't generate mutants for " +file_name + " because OpenJava " + oje.getMessage());
					}
					catch (Exception exp)
					{
						failure = true;
						output.add("[Exception] " + file_name + " " + exp.toString());
						exp.printStackTrace();
						// System.out.println("Can't generate mutants for " +file_name + " due to exception" + exp.getClass().getName());
						// exp.printStackTrace();
					}
					catch (Error er)
					{
						failure = true;
						output.add("[Error] MutationControl " + file_name + " " + er.toString());
						output.add("MutantsGenPanel: " + er.getMessage());
						er.printStackTrace();

						// System.out.println("Can't generate mutants for " +file_name + " due to error" + er.getClass().getName());

					}
				}

				// do not generate traditional mutants if no class traditional operator is selected
				if (traditional_ops != null)
				{
					try
					{
						TraditionalMutantsGenerator tmGenEngine;
						// System.out.println("original_file: " + original_file);
						// System.out.println("traditional_ops: " + traditional_ops);
						tmGenEngine = new TraditionalMutantsGenerator(original_file, traditional_ops);
						tmGenEngine.makeMutants(allPaths.get("originalpath"),allPaths.get("traditionalmutantpath"),allPaths.get("qualifiedName"));
						tmGenEngine.compileMutants(allPaths.get("traditionalmutantpath"));
					}
					catch (OpenJavaException oje)
					{
						failure = true;
						output.add("[OJException] " + file_name + " " + oje.toString());
						// System.out.println("Can't generate mutants for " +file_name + " because OpenJava " + oje.getMessage());
					}
					catch (Exception exp)
					{
						failure = true;
						output.add("[Exception] " + file_name + " " + exp.toString());
						exp.printStackTrace();
						// System.out.println("Can't generate mutants for " +file_name + " due to exception" + exp.getClass().getName());
						// exp.printStackTrace();
					}
					catch (Error er)
					{
						failure = true;
						output.add("[Error] MutationControl " + file_name + " " + er.toString());
						output.add("MutantsGenPanel: " + er.getMessage());
						er.printStackTrace();

						// System.out.println("Can't generate mutants for " +file_name + " due to error" + er.getClass().getName());

					}
				}

			}
			
			finally
			{
				if (failure)
				{
					// Cleanup
					String mutationDirectory = file_name.substring(0, file_name.length() - ".java".length());
					mutationDirectory = mutationDirectory.replace('/', '.');
					mutationDirectory = mutationDirectory.replace('\\', '.');

					File originalDir = new File(MutationSystem.MUTANT_HOME + "/" + mutationDirectory + "/"
							+ MutationSystem.ORIGINAL_DIR_NAME);
					while (originalDir.delete())
					{ // do nothing?
					}

					File cmDir = new File(
							MutationSystem.MUTANT_HOME + "/" + mutationDirectory + "/" + MutationSystem.CM_DIR_NAME);
					while (cmDir.delete())
					{ // do nothing?
					}

					File tmDir = new File(
							MutationSystem.MUTANT_HOME + "/" + mutationDirectory + "/" + MutationSystem.TM_DIR_NAME);
					while (tmDir.delete())
					{ // do nothing?
					}

					File myHomeDir = new File(MutationSystem.MUTANT_HOME + "/" + mutationDirectory);
					while (myHomeDir.delete())
					{ // do nothing?
					}
				}
			}
			return output;
		}
	}

	public HashMap<String, String> setMutationSystemPathFor(String file_name)
	{
		HashMap<String,String> allPaths=new HashMap<String, String> ();
		try
		{
			String temp;
			temp = file_name.substring(0, file_name.length() - ".java".length());
			temp = temp.replace('/', '.');
			temp = temp.replace('\\', '.');
			int separator_index = temp.lastIndexOf(".");

			if (separator_index >= 0)
			{
//				MutationSystem.setClassName(temp.substring(separator_index + 1, temp.length()));
				allPaths.put("classname", temp.substring(separator_index + 1, temp.length()));
			}
			else
			{
//				MutationSystem.setClassName(temp);
				allPaths.put("classname", temp);
			}

			String mutant_dir_path = MutationSystem.MUTANT_HOME + File.separator + temp;
			File mutant_path = new File(mutant_dir_path);
			mutant_path.mkdir();

			String class_mutant_dir_path = mutant_dir_path + File.separator + MutationSystem.CM_DIR_NAME;
			File class_mutant_path = new File(class_mutant_dir_path);
			class_mutant_path.mkdir();

			String traditional_mutant_dir_path = mutant_dir_path + File.separator + MutationSystem.TM_DIR_NAME;
			File traditional_mutant_path = new File(traditional_mutant_dir_path);
			traditional_mutant_path.mkdir();

			String original_dir_path = mutant_dir_path + File.separator + MutationSystem.ORIGINAL_DIR_NAME;
			File original_path = new File(original_dir_path);
			original_path.mkdir();

//			MutationSystem.CLASS_MUTANT_PATH = class_mutant_dir_path;
//			MutationSystem.TRADITIONAL_MUTANT_PATH = traditional_mutant_dir_path;
//			MutationSystem.ORIGINAL_PATH = original_dir_path;
//			MutationSystem.setDirectory(temp);
			
			allPaths.put("classmutantpath", class_mutant_dir_path);
			allPaths.put("traditionalmutantpath", traditional_mutant_dir_path);
			allPaths.put("originalpath", original_dir_path);
			allPaths.put("qualifiedName", temp);
			allPaths.put("unqualifiedName", temp.substring(separator_index + 1, temp.length()));
		}
		catch (Exception e)
		{
			System.err.println(e);
		}
		return allPaths;
	}
}
