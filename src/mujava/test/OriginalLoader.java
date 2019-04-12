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


package mujava.test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import mujava.MutationSystem;
import mujava.cli.Util;
import org.apache.commons.io.FileUtils;

/**
 * <p>Description: </p>
 * @author Yu-Seung Ma
 * @author Nan Li modified on 06/30/2013 for adding getResource(String)
 * @version 1.0
  */

public class OriginalLoader extends ClassLoader{

  public OriginalLoader()
  {
    super(null);
  }


  public synchronized Class loadTestClass(String name) throws ClassNotFoundException{
    Class result;
    try{
      byte[] data = getClassData(name,MutationSystem.TESTSET_PATH);
      System.out.println("Original Class Loader. Total file size:"+data.length);
      result = defineClass(null, data,0,data.length);
//      System.out.println("Class defined");
      if(result==null){
        throw new ClassNotFoundException(name);
      }
    }catch(IOException e){
      throw new ClassNotFoundException();
    }
    return result;
  }
  @Override
  public synchronized Class loadClass(String name) throws ClassNotFoundException
  {
    // See if type has already been loaded by
    // this class loader
    Class result = findLoadedClass(name);
    if (result != null){
      // Return an already-loaded class
      return result;
    }

    try{
      result = findSystemClass(name);
      return result;
    } catch (ClassNotFoundException e){
      // keep looking
    }

    try{
      byte[] data;
      try{
      // Try to load it
        data = getClassData(name,MutationSystem.CLASS_PATH);
      }catch(FileNotFoundException e){
        data = getClassData(name,MutationSystem.TESTSET_PATH);
      }
      result = defineClass(name, data,0,data.length);
      if(result==null) throw new ClassNotFoundException(name);
      return result;
    }catch(IOException e){
      throw new ClassNotFoundException();
    }
  }



  private byte[] getClassData(String name,String directory) throws FileNotFoundException,IOException
  {
    if(MutationSystem.debugOutputEnabled) {
      System.out.println("Original Loader: Searching for " + name + " in " + directory);
    }
    String fileName = name.replace ('.', File.separatorChar) + ".class";
    File f = new File (directory, fileName);
    Util.DebugPrint("file name: " + fileName);
    // Create a file object relative to directory provided

    if (MutationSystem.softClassMatch) {
      if (!f.exists()) {//We have an imprecise match
        if(MutationSystem.debugOutputEnabled) {
          System.out.println("Original Soft match initiated.");
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
    try {
      int c = bis.read();
      while (c != -1) {
        out.write(c);
        c = bis.read();
      }
    } catch (IOException e) {
      return null;
    }
    return out.toByteArray();
  }
  
  /**
   * Overrides getResource (String) to get non-class files including resource bundles from property files
   */
  @Override
  public URL getResource(String name){
	  URL url = null;
	  File resource = new File(MutationSystem.CLASS_PATH, name);
	  if(resource.exists()){
		  try {
			return resource.toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  return url;
  }

}


