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

import openjava.ptree.*;
import openjava.ptree.util.ParseTreeVisitor;

import java.io.*;
import mujava.op.exception.*;
import mujava.op.util.*;
import mujava.util.Debug;
import mujava.util.MutationFactory;

/**
 * <p>
 * Generate exception-related mutants
 * </p>
 * 
 * @author Yu-Seung Ma
 * @version 1.0
 */

public class ExceptionMutantsGenerator extends MutantsGenerator
{
	String[] exceptionOp;

	public ExceptionMutantsGenerator(File f)
	{
		super(f);
		exceptionOp = MutationSystem.em_operators;
	}

	public ExceptionMutantsGenerator(File f, boolean debug)
	{
		super(f, debug);
		exceptionOp = MutationSystem.em_operators;
	}

	public ExceptionMutantsGenerator(File f, String[] eOP)
	{
		super(f);
		exceptionOp = eOP;
	}

	void genMutants(String mutantPath, String className)
	{
		if (comp_unit == null)
		{
			System.err.println(original_file + " is skipped.");
		}

		ClassDeclarationList cdecls = comp_unit.getClassDeclarations();
		if (cdecls == null || cdecls.size() == 0)
			return;

		if (exceptionOp != null && exceptionOp.length > 0)
		{
			MutationSystem.clearPreviousMutants();
			CodeChangeLog.openLogFile(mutantPath);
			genExceptionMutants(cdecls, className);
			CodeChangeLog.closeLogFile();
		}
	}

	/**
	 * Compile exception-related mutants into bytecode
	 */
	public void compileMutants(String exceptionMutantPath)
	{
		if (exceptionOp != null && exceptionOp.length > 0)
		{
			Debug.println("* Compiling exception-related mutants into bytecode");
			super.compileMutants(exceptionMutantPath);
		}
	}

	void genExceptionMutants(ClassDeclarationList cdecls, String className)
	{
		for (int j = 0; j < cdecls.size(); ++j)
		{
			ClassDeclaration cdecl = cdecls.get(j);

			int unQualifier = className.lastIndexOf('.') + 1;
			if (cdecl.getName().equalsIgnoreCase(className.substring(unQualifier, className.length())))
			{
				try
				{
					// mujava.op.util.Mutator mutant_op;
					for (String type : exceptionOp)
					{
						ParseTreeVisitor mutant_op=MutationFactory.getExceptionMutant(type, file_env, cdecl, comp_unit, className);
						if(mutant_op!=null)
						{
							comp_unit.accept(mutant_op);
						}
					}
					// if (hasOperator(exceptionOp, "EFD"))
					// {
					// Debug.println(" Applying EFD ... ... ");
					// mutant_op = new EFD(file_env, cdecl, comp_unit);
					// comp_unit.accept(mutant_op);
					// }
					//
					// if (hasOperator(exceptionOp, "EHC"))
					// {
					// Debug.println(" Applying EHC ... ... ");
					// mutant_op = new EHC(file_env, cdecl, comp_unit, className);
					// comp_unit.accept(mutant_op);
					// }
					//
					// if (hasOperator(exceptionOp, "EHD"))
					// {
					// Debug.println(" Applying EHD ... ... ");
					// mutant_op = new EHD(file_env, cdecl, comp_unit);
					// comp_unit.accept(mutant_op);
					// }
					//
					// if (hasOperator(exceptionOp, "EHI"))
					// {
					// Debug.println(" Applying EHI ... ... ");
					// mutant_op = new EHI(file_env, cdecl, comp_unit);
					// comp_unit.accept(mutant_op);
					// }
					//
					// if (hasOperator(exceptionOp, "ETC"))
					// {
					// Debug.println(" Applying ETC ... ... ");
					// mutant_op = new ETC(file_env, cdecl, comp_unit);
					// comp_unit.accept(mutant_op);
					// }
					//
					// if (hasOperator(exceptionOp, "ETD"))
					// {
					// Debug.println(" Applying ETD ... ... ");
					// mutant_op = new ETD(file_env, cdecl, comp_unit);
					// comp_unit.accept(mutant_op);
					// }
				}
				catch (ParseTreeException e)
				{
					System.err.println("Exception, during generating traditional mutants for the class " + className);
					e.printStackTrace();
				}
			}
		}
	}
}
