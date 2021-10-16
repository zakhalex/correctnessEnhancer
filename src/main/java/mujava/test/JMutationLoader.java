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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import mujava.MutationSystem;
import mujava.cli.Util;
import mujava.op.util.Mutator;
import mujava.util.Debug;
import org.apache.commons.io.FileUtils;

/**
 * <p>
 * Description:
 * </p>
 * 
 * @author Yu-Seung Ma
 * @author Nan Li modified on 06/30/2013 for adding getResource(String)
 * @version 1.0
 */

public class JMutationLoader extends ClassLoader implements InstrumentedClassLoader
{

	private ConcurrentHashMap<String, byte[]> instrumentedClass;
	private String mutant_name;
	private String mutantPath;
	boolean tt = false;

	public JMutationLoader()
	{
		super(null);
	}

	public JMutationLoader(ClassLoader parentClassLoader)
	{
		super(parentClassLoader);
	}

	public JMutationLoader(String mutantPath, String dir)
	{
		super(null);
		mutant_name = dir;
		this.mutantPath=mutantPath;
	}

	public JMutationLoader(ClassLoader parentClassLoader, String mutantPath, String dir)
	{
		super(parentClassLoader);
		mutant_name = dir;
		this.mutantPath=mutantPath;
	}

	public synchronized Class loadTestClass(String name) throws ClassNotFoundException
	{
		Class result;
		try
		{
			// Try to load mutant class
			byte[] data = getClassData(name, MutationSystem.TESTSET_PATH);
			result = defineClass(null, data, 0, data.length);
			if (result == null)
			{
				Debug.println("Test class result load failure.");
				throw new ClassNotFoundException(name);
			}
			else
			{
				Debug.println("Test class loaded.");
			}
		}
		catch (IOException e)
		{
			throw new ClassNotFoundException();
		}
		return result;
	}

	@Override
	public synchronized Class loadClass(String name) throws ClassNotFoundException
	{

		Debug.println("Entering classloader for class " + name);
		// See if type has already been loaded by
		// this class loader
		Class result = findLoadedClass(name);
		if (result != null)
		{
			Debug.println("For class " + name + " match has been found as " + result.getCanonicalName());
			// Return an already-loaded class
			return result;
		}

		try
		{
			byte[] data = null;
			try
			{
				try
				{
					int start_index = name.lastIndexOf(".");
					Debug.println("Detected start index:" + start_index);
					if (start_index >= 0)
					{
						String nameWithNoPackage = name.substring(start_index + 1, name.length());
						data = getClassData(nameWithNoPackage, mutantPath+File.separator+mutant_name);
					}
					else
					{
						data = getClassData(name, mutantPath +File.separator+ mutant_name);
					}

				}
				catch (FileNotFoundException e)
				{
					Debug.println("File not found");
					data = getClassData(name, MutationSystem.CLASS_PATH);
				}
				finally
				{
					if (data!=null)
					{
						try {
							data = instrumentBytecode(data);
						}
						catch(Exception ignored) {

						}//We don't want to impact classloading due to instrumentation failure
					}
				}
			}
			catch (FileNotFoundException e)
			{
				Debug.println("File not found");
				data = getClassData(name, MutationSystem.TESTSET_PATH);
			}

			result = defineClass(null, data, 0, data.length);

		}
		catch (IOException e)
		{

		}

		try
		{
			return super.loadClass(name);
			// result = findSystemClass(name);
			// System.out.println("We've got a system class "+name);
			// return result;
		}
		catch (ClassNotFoundException e)
		{
			// keep looking
		}
		if (result == null)
		{
			throw new ClassNotFoundException(name);
		}
		return result;
	}

	private byte[] instrumentBytecode(final byte[] bytecode) throws IOException {
		return MutationSystem.jacocoInstrumenter.instrument(bytecode, "");
	}

	private byte[] getClassData(String name, String directory) throws FileNotFoundException, IOException
	{
		if(MutationSystem.debugOutputEnabled) {
			System.out.println("Mutant Loader: Searching for " + name + " in " + directory);
		}
		String fileName = name.replace ('.', File.separatorChar) + ".class";
		File f = new File (directory, fileName);
		Util.DebugPrint("file name: " + fileName);
		// Create a file object relative to directory provided

		if (MutationSystem.softClassMatch) {
			if (!f.exists()) {//We have an imprecise match
				if(MutationSystem.debugOutputEnabled) {
					System.out.println("Mutant Soft match initiated.");
				}
				File root = new File(directory);
				String absolutePath = f.getAbsolutePath();
				System.out.println("Absolute Path is: "+absolutePath);
				int distance = Integer.MAX_VALUE;
				File newFile = f;
				boolean recursive = true;

				Collection<File> files = FileUtils.listFiles(root, null, recursive);
				String unclassifiedFileName=fileName.substring(fileName.lastIndexOf(File.separatorChar)+1);
				for (File file:files ) {
					try {
						if (file.getName().equalsIgnoreCase(unclassifiedFileName)) {
							int candidateDistance = LevenshteinDistance.computeLevenshteinDistance(absolutePath, file.getAbsolutePath());
							if (distance > Math.min(candidateDistance, distance)) {
								distance=candidateDistance;
								newFile = file;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (distance < Integer.MAX_VALUE) {
					f = newFile;
					if(MutationSystem.debugOutputEnabled) {
						System.out.println("Approximate match for " + name + " found as " + f.getAbsolutePath());
					}
				}
			}
		}
		// Get stream to read from
		FileInputStream fis = new FileInputStream(f);

		BufferedInputStream bis = new BufferedInputStream(fis);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			int c = bis.read();
			while (c != -1)
			{
				out.write(c);
				c = bis.read();
			}
		}
		catch (IOException e)
		{
			return null;
		}
		return out.toByteArray();
	}

	/**
	 * Overrides getResource (String) to get non-class files including resource
	 * bundles from property files
	 */
	@Override
	public URL getResource(String name)
	{
		URL url = null;
		File resource = new File(MutationSystem.CLASS_PATH, name);
		if (resource.exists())
		{
			try
			{
				return resource.toURI().toURL();
			}
			catch (MalformedURLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return url;
	}

	@Override
	public byte[] getInstrumentedClass(String name) {
		return instrumentedClass.get(name);
	}
}
