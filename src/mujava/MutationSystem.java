/**
 * Copyright (C) 2015  the original author or authors.
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

import openjava.mop.OJSystem;
import openjava.mop.OJClass;

import java.io.*;

import mujava.util.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import com.sun.tools.javac.util.Pair;

/**
 * <p>Description: Control an entire MuJava system -- 
 *    specify path of MuJava system; 
 *    specify path of Java source files which mutation is applied to;
 *    specify path of classes of Java source files;
 *    specify path which mutants, class mutants, traditional mutants, and
 *       exception-related mutants are put into;
 *    specify path where test cases are located;
 *    list names of class mutation operators, traditional mutation operators,
 *       and exception-related mutation operators; and
 *    clean up previously generated mutants </p>
 *      
 * <p> Requires: mujava.config must be stored in MuJava system home directory </p>
 * <p> Limitation: MuJava currently does not support interface, abstract, gui, 
 *                 and applet.   
 *        
 * @author Yu-Seung Ma
 * @update by Nan Li May 2012 
 * @new function addURL:  classes are added to the CLASSPATH dynamically so that users do not need to set up the CLASSPATH manually any more
 * @version 1.0
  */

public class MutationSystem extends OJSystem
{
	private static final PropertiesDictionary dictionary=new PropertiesDictionary();
	
	
   public static final int INTERFACE = 0;
   public static final int ABSTRACT = 1;
   public static final int GUI = 2;
   public static final int MAIN = 3;
   public static final int MAIN_ONLY = 4;
   public static final int NORMAL = 5;
   public static final int APPLET = 6;

   public static final int CM = 0; // Class Mutation Operator
   public static final int TM = 1; // Traditional Mutation Operator
   public static final int EM = 2; // Exceptional Mutation Operator


   /** home path where inputs and output of mujava system are located*/
   //public static String SYSTEM_HOME = "C:/jmutation";
   //public static String SYSTEM_HOME = "/Users/dmark/mujava";
//   public static String SYSTEM_HOME = "";
   public static String SYSTEM_HOME = System.getProperty("user.dir");

   /** path of Java source files which mutation is applied to  */
   public static String SRC_PATH = SYSTEM_HOME + File.separatorChar + "src";

   /** path of classes of Java source files at SRC_PATH directory */
   public static String CLASS_PATH = SYSTEM_HOME + File.separatorChar + "classes";

   /** home path which mutants are put into */
   public static String MUTANT_HOME = SYSTEM_HOME + File.separatorChar + "result";

   /** path which class mutants are put into */
   private static String CLASS_MUTANT_PATH = "";

   /** path which traditional mutants are put into */
   private static String TRADITIONAL_MUTANT_PATH = "";

   /** path which exception-related mutants are put into */
   private static String EXCEPTION_MUTANT_PATH = "";

   /** ??? absolute path for ???*/
   private static String MUTANT_PATH = "";

   /** ??? absolute path for the original Java source*/
   private static String ORIGINAL_PATH = "";

   /** absolute path where test cases are located */
   public static String TESTSET_PATH = SYSTEM_HOME + File.separatorChar + "testset";

   /** class name without package name that mutation is applied into */
   private static String CLASS_NAME;
//   public static String getClassName()
//   {
//	   return CLASS_NAME;
//   }
//   public static void setClassName(String className)
//   {
//	   CLASS_NAME=className;
//   }
   public static String[] getTestSetNames()
	{
		ArrayList<String> v = new ArrayList<String>();
		getTestSetNames(new File(MutationSystem.TESTSET_PATH), v);
		// String[] result = new String[v.size()];
		System.out.println("We have identified " + v.size() + " classes as test classes");
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < v.size(); i++)
		{
			String localResult = v.get(i).toString();
			// if (localResult.toLowerCase().contains("test."))
			{
				result.add(localResult);
			}
			// result[i] = localResult;
		}
		return result.toArray(new String[result.size()]);
	}
   
   public static void getTestSetNames(File testDir, ArrayList<String> v)
	{

		String[] t_list;
		if (MutationSystem.getDictionary().getProperty("filter_tests", "N").equalsIgnoreCase("Y"))
		{
			ArrayList<String> testFiles = new ArrayList<String>();
			for (File file : testDir.listFiles(new ExtensionFilter("class")))
			{
				try
				{
					byte[] data = Files.readAllBytes(file.toPath());
					byte[] pattern = "junit".getBytes();
					if (indexOf(data, pattern) != -1)
					{
						testFiles.add(file.getName());
					}
					else
					{
						System.out.println("Not considered a test: " + file.getAbsolutePath());
					}
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			t_list = testFiles.toArray(new String[testFiles.size()]);
			/*
			 * ConcurrentLinkedQueue<File> stack = new ConcurrentLinkedQueue<File>();
			 * stack.add(testDir);
			 * while(!stack.isEmpty()) {
			 * File child = stack.poll();
			 * if (child.isDirectory()) {
			 * for(File f : child.listFiles()) stack.add(f);
			 * } else if (child.isFile()) {
			 * System.out.println(child.getPath());
			 * }
			 * }
			 */
		}
		else
		{
			t_list = testDir.list(new ExtensionFilter("class"));
		}

		if (t_list == null)
		{
			System.out.println(" [Error] No test suite is detected. ");
			return;
		}
		int start_index = MutationSystem.TESTSET_PATH.length();
		int end_index = testDir.getAbsolutePath().length();
		if (start_index < end_index)
			start_index++;
		String suffix = testDir.getAbsolutePath().substring(start_index, end_index);
		if (suffix == null || suffix.equals(""))
		{
			suffix = "";
		}
		else
		{
			String temp = "";
			for (int k = 0; k < suffix.length(); k++)
			{
				char ch = suffix.charAt(k);
				if (ch == File.separatorChar)
				{
					temp = temp + ".";
				}
				else
				{
					temp = temp + ch;
				}
			}
			suffix = temp + ".";
		}

		for (int i = 0; i < t_list.length; i++)
		{
			v.add(suffix + t_list[i]);
		}

		File[] subDir = testDir.listFiles(new DirFileFilter());
		for (int i = 0; i < subDir.length; i++)
		{
			getTestSetNames(subDir[i], v);
		}

	}

	public static int indexOf(byte[] data, byte[] pattern)
	{
		int[] failure = computeFailure(pattern);

		int j = 0;
		if (data.length == 0)
			return -1;

		for (int i = 0; i < data.length; i++)
		{
			while (j > 0 && pattern[j] != data[i])
			{
				j = failure[j - 1];
			}
			if (pattern[j] == data[i])
			{
				j++;
			}
			if (j == pattern.length)
			{
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	/**
	 * Computes the failure function using a boot-strapping process,
	 * where the pattern is matched against itself.
	 */
	private static int[] computeFailure(byte[] pattern)
	{
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++)
		{
			while (j > 0 && pattern[j] != pattern[i])
			{
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i])
			{
				j++;
			}
			failure[i] = j;
		}

		return failure;
	}
	
	public static String[] eraseExtension(String[] list, String extension)
	{
		String[] result = new String[list.length];
		for (int i = 0; i < list.length; i++)
		{
			result[i] = list[i].substring(0, list[i].length() - extension.length() - 1);
		}
		return result;
	}
   /** path for */
   private static String DIR_NAME;
   public static synchronized void setDirectory(String directory)
   {
	   DIR_NAME=directory;
   }
   
   /** directory name for class mutants */
   public static String CM_DIR_NAME = "class_mutants";

   /** directory name for exception-related mutants */
   public static String EM_DIR_NAME = "exception_mutants";

   /** directory name for traditional mutants */
   public static String TM_DIR_NAME = "traditional_mutants";

   /** directory name for original class */
   public static String ORIGINAL_DIR_NAME = "original";

   public static String LOG_IDENTIFIER = ":";
    
   /** List of names of class mutation operators */
   public static String[] cm_operators = { "IHI","IHD","IOD","IOP","IOR","ISI","ISD","IPC",  // 8��
                                            "PNC","PMD","PPD","PCI","PCC","PCD","PRV",        // 7��
                                            "OMR","OMD","OAN",                    // 3��
                                            "JTI","JTD","JSI","JSD","JID","JDC",              // 6��
                                            "EOA","EOC","EAM","EMM" };                        // 4�� - �� 28��

   // public static String[] first_tm_operators = {  "ABS","AOR","LCR","ROR","UOI" };
   /*public static String[] second_tm_operators = {  "AOR","AOD","AOI","ROR",
                                            "COR","COD","COI","SOR","LOR","LOI","LOD"
                                            ,"SCR","SCI","SCD"};*/

   /** List of names of traditional mutation operators */
   // Upsorn: (04/06/2009) added Statement Deletion operator (SID, SWD, SFD, SSD)
   public static String[] tm_operators = {   "AORB","AORS","AOIU","AOIS","AODU","AODS",
                                             "ROR","COR","COD","COI","SOR","LOR","LOI","LOD","ASRS","SDL","VDL","CDL","ODL"};
//                                             "IBD", "WBD", "FBD", "SBD" };
//                                             "SID", "SWD", "SFD", "SSD" };
//                                             "SDL"};   

   /** List of names of exception-related mutation operators */
   public static String[] em_operators = {  "EFD", "EHC", "EHD", "EHI",
                                            "ETC", "ETD"};


   
   // Upsorn: (05/18/2009) added mutation operators' description
   public static String[] op_desc = {   "" };
   
   
   

   
 /**
   * Return type of class.
   * @param name of class
   * @return type of class ( types: interface, abstract, GUI, main, normal, applet )
   */
   public static int getClassType (String class_name)
   {   
      try 
      {
         Class c = Class.forName (class_name);
         if (c.isInterface())       
            return INTERFACE;
         
         if (Modifier.isAbstract(c.getModifiers())) 
            return ABSTRACT;
         
         Method[] ms = c.getDeclaredMethods();

         if (ms != null)
         {
            if ( (ms.length == 1) && (ms[0].getName().equals("main"))) 
               return MAIN_ONLY;
        
            for (int i=0; i<ms.length; i++)
            {
               if (ms[i].getName().equals("main")) 
                  return MAIN;
            }
         }
         
         if (isGUI(c))       return GUI;
         if (isApplet(c))    return APPLET;
         
         return NORMAL;
      } 
      catch(Exception e)  {   return -1;   }
      catch(Error e)  {   return -1;  }
   }
   

   /** Examine if class <i>c</i> is a GUI class */
   private static boolean isGUI (Class c)
   {
      while (c != null)
      {
         if ( (c.getName().indexOf("java.awt") == 0) || 
              (c.getName().indexOf("javax.swing") == 0) ) 
            return true;
     
         c = c.getSuperclass();
         if (c.getName().indexOf("java.lang") == 0)       
            return false;
      }
      return false;
   }

   
  /** Examine if class <i>c</i> is an applet class */
   private static boolean isApplet(Class c)
   {
      while (c != null)
      {
         if ( c.getName().indexOf("java.applet")==0)   
            return true;
     
         c = c.getSuperclass();
         if (c.getName().indexOf("java.lang")==0) 
            return false;
      }
      return false;
   }


   /**  Inheritance Informations  */
   public static InheritanceINFO[] classInfo = null;


   /** Examine if type is of primitive types (boolean, byte, char, short,
    * int, long, double, void) */ 
   public static boolean isPrimitive(OJClass type)
   {
      if (type.equals(BOOLEAN))  return true;
      if (type.equals(BYTE))     return true;
      if (type.equals(CHAR))     return true;
      if (type.equals(SHORT))    return true;
      if (type.equals(INT))      return true;
      if (type.equals(LONG))     return true;
      if (type.equals(DOUBLE))   return true;
      if (type.equals(VOID))     return true;
      return false;
   }

   public static PropertiesDictionary getDictionary()
   {
	   return dictionary;
   }
   /** Clear arranged original file made before*/
   private static void clearPreviousOriginalFiles()
   {
      File original_class_dir = new File(MutationSystem.ORIGINAL_PATH);
      int i;
      File[] old_files = original_class_dir.listFiles();
      
      if (old_files == null)        return;
      
      for (i=0; i<old_files.length; i++)
      {
		 old_files[i].delete();
      }
   }

   
   /* Clear mutants generated from previous run (in class_mutants folder)*/
   static void clearPreviousMutants(String path)
   {
      File mutant_classes_dir = new File(path);
      int i;
      // delete previous mutant files
      File[] old_files = mutant_classes_dir.listFiles();
      
      if (old_files==null)         return;
      
      for (i=0; i<old_files.length; i++)
      {
         if (old_files[i].isDirectory())
         {
            File[] old_mutants = old_files[i].listFiles();
            for (int j=0; j<old_mutants.length; j++)
            {
               old_mutants[j].delete();
            }
         }
         old_files[i].delete();
      }
   }


   /* Clear mutants generated from previous run (in traditional_mutants folder)*/
   static void clearPreviousTraditionalMutants(String path)
   {
      File traditional_mutant_dir = new File(path);
      int i;
      // delete previous mutant files
      File[] methods = traditional_mutant_dir.listFiles();

      if (methods == null)
         return;
      
      for (i=0; i<methods.length; i++)
      {
         if (methods[i].isDirectory())
         {
            File[] mutant_dir = methods[i].listFiles();
            
            for (int j=0; j<mutant_dir.length; j++)
            {
               if (mutant_dir[j].isDirectory())
               {
                  File[] old_mutants = mutant_dir[j].listFiles();
                  for (int k=0; k<old_mutants.length; k++)
                  {
                     old_mutants[k].delete();
                  }
               }
               mutant_dir[j].delete();
            }
         }
         methods[i].delete();
      }
   }

   /** Delete all traditional mutants generated before */
   public static void clearPreviousTraditionalMutants()
   {
      clearPreviousTraditionalMutants(MutationSystem.TRADITIONAL_MUTANT_PATH);
   }

   /** Delete all class mutants generated before */
   public static void clearPreviousClassMutants()
   {
      clearPreviousMutants(MutationSystem.CLASS_MUTANT_PATH);
   }

    /** Delete all mutants generated before */
   public static void clearPreviousMutants()
   {
      clearPreviousOriginalFiles();
      clearPreviousClassMutants();
      clearPreviousTraditionalMutants();
   }

   /* Set up target files (stored in src folder) to be tested */
   public static Vector<String> getNewTragetFiles()
   {
      Vector<String> targetFiles = new Vector<String>();
      getJavacArgForDir (MutationSystem.SRC_PATH, "", targetFiles);
      return targetFiles;
   }

   protected static String getJavacArgForDir (String dir, String str, Vector<String> targetFiles)
   {
      String result = str;
      String temp = "";

      File dirF = new File(dir);
      File[] javaF = dirF.listFiles (new ExtensionFilter("java"));      
      if (javaF.length > 0)
      {
         result = result + dir + "/*.java ";
         
         for (int k=0; k<javaF.length; k++)
         {
            temp = javaF[k].getAbsolutePath();
            temp.replace('\\', '/');
            targetFiles.add(temp.substring(MutationSystem.SRC_PATH.length()+1, temp.length()));
         }
      }

      File[] sub_dir = dirF.listFiles (new DirFileFilter());
      if (sub_dir.length == 0)    return result;

      for (int i=0; i<sub_dir.length; i++)
      {
         result = getJavacArgForDir(sub_dir[i].getAbsolutePath(), result, targetFiles);
      }
      return result;
   }

   /** Return list of class names = class name of <i>dir</i> directory + <i>result</i>*/
   public static String[] getAllClassNames(String[] result,String dir)
   {
      String[] classes;
      String temp;
      File dirF = new File(dir);
      
      File[] classF = dirF.listFiles (new ExtensionFilter("class"));      
      if (classF != null)
      {
         classes = new String[classF.length];
         for(int k=0; k<classF.length; k++) 
         {
            temp = classF[k].getAbsolutePath();
            classes[k] = temp.substring(MutationSystem.CLASS_PATH.length()+1, temp.length()-".class".length());
            classes[k] = classes[k].replace('\\', '.');
            classes[k] = classes[k].replace('/', '.');
         }
         result = addClassNames(result, classes);
      }
      
      File[] sub_dir = dirF.listFiles(new DirFileFilter());
      if (sub_dir == null)       return result;
      
      for (int i=0; i<sub_dir.length; i++)
      {
         result = getAllClassNames(result,sub_dir[i].getAbsolutePath());
      }
      return result;
   }
   

  /** Return combine list of <i> list1 </i> and <i> list2</i> lists.
   *  @param list1 String list
   *  @param list2 String list
   *  @return combined list of list1 and list2*/
   private static final String[] addClassNames(String[] list1, String[] list2)
   {
      if (list1 == null)         
         list1 = list2;
      else
      {
         int num = list1.length;
         String[] temp = new String[num];
      
         for (int i=0; i<temp.length; i++)
         {
            temp[i] = list1[i];
         }
         
         num = num + list2.length;
         list1 = new String[num];
         
         for (int i=0; i<temp.length; i++)
         {
            list1[i] = temp[i];
         }
         
         for (int i=temp.length; i<num; i++)
         {
            list1[i] = list2[i-temp.length];
         }
      }
      return list1;
   }

  /** Return inheritance information for give class <br>
   *  @param class_name name of class
   *  @return inheritance information
   */
   public static InheritanceINFO getInheritanceInfo(String class_name)
   {
      for (int i=0; i<classInfo.length; i++)
      {
         if (classInfo[i].getClassName().equals(class_name)) 
            return classInfo[i];
      }
      return null;
   }

  /** Recognize inheritance relation of all classes located at CLASS_PATH directory. <br>
   *  <b>* CAUTION: </b> this function should be called before
   *  using <i>MutantsGenerator</i> or sub-classes of <i>MutantsGenerator</i>.
 * @throws Exception 
   *  @see MutantsGenerator, AllMutantsGenerator, TraditionalMutantsGenerator, ClassMutantsGenerator, ExceptionMutantsGenerator*/
   public static void recordInheritanceRelation() throws Exception
   {
      String[] classes = null;
      classes = MutationSystem.getAllClassNames(classes, MutationSystem.CLASS_PATH);
      
      if (classes == null)
      {
         System.err.println("[ERROR] There are no classes to mutate.");
         System.err.println(" Please check the directory  " + MutationSystem.CLASS_PATH + " and be sure that MuJava_HOME is set correctly (without a trailing slash) in mujava.config.");
         Runtime.getRuntime().exit(0);
      }
      classInfo = new InheritanceINFO[classes.length];

	  boolean[] bad = new boolean[classes.length];

      for (int i = 0; i < classes.length; i++)
      {
         bad[i] = false;
         try
         {       	
         	String classpath = System.getProperty("java.class.path");
         	//add the class path dynamically
         	addURL(MutationSystem.CLASS_PATH);
            //create a new class from the class name
            Class c = Class.forName(classes[i]);
            //create the parent class of the class above
            Class parent = c.getSuperclass();
            
            if ( (parent == null) || (parent.getName().equals("java.lang.Object")) )
            {
               classInfo[i] = new InheritanceINFO(classes[i], "");
            }
            else
            {
               classInfo[i] = new InheritanceINFO(classes[i], parent.getName());
            }
         } catch (ClassNotFoundException e)  
         {
            System.err.println("[ERROR] Can't find the class: " + classes[i]);
            System.err.println("Please check that the compiled class for the code you want to mutate is in the classes/ directory. Also check that the MuJava_HOME variable in mujava.config does not end with a trailing slash. " );
		    bad[i] = true; 
		    classInfo[i] = new InheritanceINFO(classes[i], "");
            Runtime.getRuntime().exit(0);
         } catch (Error er)
         {
            // Sometimes error occurred. However, I can't solve..
            // To muJava users: try do your best to solve it. ^^;
            System.out.println ("[ERROR] for class " + classes[i] + " => "+ er.getMessage() );
			er.printStackTrace();
            bad[i] = true; 
			classInfo[i] = new InheritanceINFO(classes[i], "");
         }
      }

      for (int i=0; i<classes.length; i++)
      {
		 if (bad[i])         
            continue;

         String parent_name = classInfo[i].getParentName();

         for (int j=0; j<classes.length; j++)
         {
            if (bad[j])             
               continue;
            
            if (classInfo[j].getClassName().equals(parent_name))
            {
               classInfo[i].setParent(classInfo[j]);
               classInfo[j].addChild(classInfo[i]);
               break;
            }
         }
      }
   }
   
   /**
    * add the class path dynamically
    * @param classPath
    * @throws Exception
    */
   
   public static void addURL(String classPath) throws Exception {
		  Method addClass = null;
		  ClassLoader cl = null;
		  File f = null;

		  addClass = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
		  addClass.setAccessible(true);
		  f = new File(classPath);
		  cl = ClassLoader.getSystemClassLoader();
		  addClass.invoke(cl, new Object[] { f.toURL() });
		}

  /** Re-setting MuJava structure for give class name <br>
   * @param name of class (including package name) */
   public static void setJMutationPaths(String whole_class_name)
   {
      int temp_start = whole_class_name.lastIndexOf(".") + 1;
      if (temp_start < 0)
      {
         temp_start=0;
      }
  
      int temp_end = whole_class_name.length();
      MutationSystem.CLASS_NAME = whole_class_name.substring(temp_start, temp_end);
      MutationSystem.DIR_NAME   = whole_class_name;
      MutationSystem.ORIGINAL_PATH = MutationSystem.MUTANT_HOME
                                + "/" + whole_class_name + "/" + MutationSystem.ORIGINAL_DIR_NAME;
      MutationSystem.CLASS_MUTANT_PATH = MutationSystem.MUTANT_HOME
                                + "/" + whole_class_name + "/" + MutationSystem.CM_DIR_NAME;
      MutationSystem.TRADITIONAL_MUTANT_PATH = MutationSystem.MUTANT_HOME
                                + "/" + whole_class_name + "/" + MutationSystem.TM_DIR_NAME;
      MutationSystem.EXCEPTION_MUTANT_PATH = MutationSystem.MUTANT_HOME
                                + "/" + whole_class_name + "/" + MutationSystem.EM_DIR_NAME;

   }
   
	private static HashMap<String, String> readPropertiesFromFile(String file) throws IOException
	{
		HashMap<String,String> properties=new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while((line=br.readLine())!=null)
		{
			Pair<String, String>localPair=parseProperty(line);
			if(localPair!=null)
			{
				properties.put(localPair.fst, localPair.snd);
			}
		}
		br.close();
		return properties;

	}
   
   private static Pair<String,String> parseProperty(String property)
   {
	   int dividerPosition=property.indexOf('=');
		   if(dividerPosition>0)//if the property starts from = - we ignore it
		   {
			   return new Pair<String, String>(property.substring(0, dividerPosition),property.substring(dividerPosition+1, property.length()));
		   }
	   
	   return null;
   }
   
   private static HashMap<String,String> parseProperties(Collection<String> lines)
   {
	   HashMap<String,String> properties=new HashMap<>();
	   for(String property:lines)
	   {
		   int dividerPosition=property.indexOf('=');
		   if(dividerPosition>0)//if the property starts from = - we ignore it
		   {
			   properties.put(property.substring(0, dividerPosition),property.substring(dividerPosition+1, property.length()));
		   }
	   }
	   return properties;
   }
  /** <b> Default mujava system structure setting function </b>
   * <p> Recognize file structure for mutation system based on "mujava.config". </p>
   *  <p> ** CAUTION : this function or `setJMutationStructure(String home_path)' should be called before generating and running mutants. */
   public static void setJMutationStructure()
   {
      try 
      {
    	  //Starting from properties
//    	  HashMap<String,String> properties=readPropertiesFromFile(MutationSystem.SYSTEM_HOME + "/mujava.config");

//    	  dictionary.readPropertiesFromFile(MutationSystem.SYSTEM_HOME + "/mujava.config");
    	  dictionary.readPropertiesFromFile("mujava.config");
//         File f = new File (MutationSystem.SYSTEM_HOME + "/mujava.config");
//         FileReader r = new FileReader(f);
//         BufferedReader reader = new BufferedReader(r);
//         String str = reader.readLine();
//         String home_path = str.substring("MuJava_HOME=".length(), str.length());
//         SYSTEM_HOME = home_path;
			SYSTEM_HOME = dictionary.getProperty("MuJava_Home");
			SRC_PATH = dictionary.getProperty("MuJava_src", SYSTEM_HOME + File.separator + "src");
			CLASS_PATH = dictionary.getProperty("MuJava_class", SYSTEM_HOME + File.separator + "classes");
			MUTANT_HOME = dictionary.getProperty("MuJava_mutants", SYSTEM_HOME + File.separator + "result");
			TESTSET_PATH = dictionary.getProperty("MuJava_tests", SYSTEM_HOME + File.separator + "testset");

		}
		catch (FileNotFoundException e1)
      {
         System.err.println("[ERROR] Can't find mujava.config file");
         e1.printStackTrace();
      } catch (Exception e)
      {
         e.printStackTrace();
      }
   }

  /** <p> Recognize file structure for mutation system from not "mujava.config" but from user directly </p>*/
   public static void setJMutationStructure(String home_path)
   {
      SYSTEM_HOME = home_path;
      SRC_PATH = dictionary.getProperty("MuJava_src",SYSTEM_HOME + File.separator + "src");
      CLASS_PATH = dictionary.getProperty("MuJava_class",SYSTEM_HOME + File.separator + "classes");
      MUTANT_HOME = dictionary.getProperty("MuJava_mutants",SYSTEM_HOME + File.separator + "result");
      TESTSET_PATH = dictionary.getProperty("MuJava_tests",SYSTEM_HOME + File.separator + "testset");
  }
   
   /** <p> Recognize file structure for mutation system from not "mujava.config" but from user directly - with session </p>*/
   public static void setJMutationStructure(String home_path, String sessionName)
   {
      SYSTEM_HOME = home_path+File.separator+sessionName;
      SRC_PATH = dictionary.getProperty("MuJava_src",SYSTEM_HOME + File.separator + "src");
      CLASS_PATH = dictionary.getProperty("MuJava_class",SYSTEM_HOME + File.separator + "classes");
      MUTANT_HOME = dictionary.getProperty("MuJava_mutants",SYSTEM_HOME + File.separator + "result");
      TESTSET_PATH = dictionary.getProperty("MuJava_tests",SYSTEM_HOME + File.separator + "testset");
  }
   
   /*
    * Lin add for setting sessions
    * 
    */
   public static void setJMutationStructureAndSession (String sessionName)
   {
      try 
      {
    	  dictionary.readPropertiesFromFile("mujava.config");
    	 SYSTEM_HOME = dictionary.getProperty("MuJava_Home")+File.separator+sessionName;
         SRC_PATH = dictionary.getProperty("MuJava_src",SYSTEM_HOME + File.separator+"src");
         CLASS_PATH = dictionary.getProperty("MuJava_class",SYSTEM_HOME + File.separator+"classes");
         MUTANT_HOME = dictionary.getProperty("MuJava_mutants",SYSTEM_HOME + File.separator+"result");
         TESTSET_PATH = dictionary.getProperty("MuJava_tests",SYSTEM_HOME + File.separator+"testset");
      } catch (FileNotFoundException e1)
      {
         System.err.println("[ERROR] Can't find mujava.config file");
         e1.printStackTrace();
      } catch (Exception e)
      {
         e.printStackTrace();
      }
   }
   
   
   
}

