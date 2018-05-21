package mujava.util;

import java.io.File;

import mujava.MutationSystem;
import mujava.op.AMC;
import mujava.op.EAM;
import mujava.op.EMM;
import mujava.op.EOA;
import mujava.op.EOC;
import mujava.op.IHD;
import mujava.op.IHI;
import mujava.op.IOD;
import mujava.op.IOP;
import mujava.op.IOR;
import mujava.op.IPC;
import mujava.op.ISD;
import mujava.op.JDC;
import mujava.op.JID;
import mujava.op.JSD;
import mujava.op.JSI;
import mujava.op.JTD;
import mujava.op.JTI;
import mujava.op.OAN;
import mujava.op.OMD;
import mujava.op.OMR;
import mujava.op.PCC;
import mujava.op.PCD;
import mujava.op.PCI;
import mujava.op.PMD;
import mujava.op.PNC;
import mujava.op.PPD;
import mujava.op.PRV;
import mujava.op.basic.AODS;
import mujava.op.basic.AODU;
import mujava.op.basic.AOIS;
import mujava.op.basic.AOIU;
import mujava.op.basic.AORB;
import mujava.op.basic.AORS;
import mujava.op.basic.ASRS;
import mujava.op.basic.CDL;
import mujava.op.basic.COD;
import mujava.op.basic.COI;
import mujava.op.basic.COR;
import mujava.op.basic.LOD;
import mujava.op.basic.LOI;
import mujava.op.basic.LOR;
import mujava.op.basic.ODL;
import mujava.op.basic.ROR;
import mujava.op.basic.SDL;
import mujava.op.basic.SOR;
import mujava.op.basic.VDL;
import mujava.op.exception.EFD;
import mujava.op.exception.EHC;
import mujava.op.exception.EHD;
import mujava.op.exception.EHI;
import mujava.op.exception.ETC;
import mujava.op.exception.ETD;
import mujava.op.util.DeclAnalyzer;
import mujava.op.util.Mutator;
import openjava.mop.FileEnvironment;
import openjava.ptree.ClassDeclaration;
import openjava.ptree.CompilationUnit;
import openjava.ptree.util.ParseTreeVisitor;

public class MutationFactory
{
	public static Mutator getClassMutant(String type, FileEnvironment file_env, ClassDeclaration cdecl,
			CompilationUnit comp_unit, String className, String qualifiedName)
	{
		return getMutant(type, file_env, cdecl, comp_unit, className, qualifiedName, null);
	}

	public static ParseTreeVisitor getExceptionMutant(String type, FileEnvironment file_env, ClassDeclaration cdecl,
			CompilationUnit comp_unit, String className)
	{
		return getMutant(type, file_env, cdecl, comp_unit, className, null, null);
	}

	public static Mutator getTraditionalMutant(String type, FileEnvironment file_env, ClassDeclaration cdecl,
			CompilationUnit comp_unit, String className)
	{
		return getMutant(type, file_env, cdecl, comp_unit, className, null, null);
	}
	
	public static DeclAnalyzer getDeclarationMutant(String type, FileEnvironment file_env, ClassDeclaration cdecl,
			CompilationUnit comp_unit, String className)
	{
		switch (type)
		{
			case "IHD":
			{
				return new IHD(file_env, null, cdecl, className);
			}
			case "IHI":
			{
				return new IHI(file_env, null, cdecl, className);
			}
			case "IOD":
			{
				return new IOD(file_env, null, cdecl, className);
			}
			case "OMR":
			{
				return new OMR(file_env, null, cdecl, className);
			}
			case "OMD":
			{
				return new OMD(file_env, null, cdecl, className);
			}
			case "JDC":
			{
				return new JDC(file_env, null, cdecl, className);
			}
			default:
				throw new UnsupportedOperationException();
		}
	}
	
	public static Mutator getMutant(String type, FileEnvironment file_env, ClassDeclaration cdecl,
			CompilationUnit comp_unit, String className, String qualifiedName, String methodName)
	{
		/*
		 * try
		 * {
		 * Class<?> classType = Class.forName(className);
		 * return (Mutator) classType.getDeclaredConstructor(FileEnvironment.class, ClassDeclaration.class,
		 * CompilationUnit.class, String.class).newInstance(file_env, cdecl, comp_unit, className);
		 * }
		 * catch (Exception e)
		 * {
		 * e.printStackTrace();
		 * return null;
		 * }
		 */

		switch (type)
		{
			case "AMC":
			{
				return new AMC(file_env, cdecl, comp_unit, className);
			}
			case "IOR":
			{
				return null;
				// Debug.println(" Applying IOR ... ... ");
				// try
				// {
				// Class parent_class = Class.forName(qname).getSuperclass();
				// if (!(parent_class.getName().equals("java.lang.Object")))
				// {
				// String temp_str = parent_class.getName();
				// String result_str = "";
				// for (int k=0; k<temp_str.length(); k++)
				// {
				// char c = temp_str.charAt(k);
				// if (c == '.')
				// {
				// result_str = result_str + "/";
				// }
				// else
				// {
				// result_str = result_str + c;
				// }
				// }
				//
				// File f = new File(MutationSystem.SRC_PATH, result_str + ".java");
				// if (f.exists())
				// {
				// CompilationUnit[] parent_comp_unit = new CompilationUnit[1];
				// FileEnvironment[] parent_file_env = new FileEnvironment[1];
				// this.generateParseTree(f, parent_comp_unit, parent_file_env, className);
				// this.initParseTree(parent_comp_unit, parent_file_env);
				// Mutator mutant_op = new IOR(file_env, cdecl, comp_unit);
				// ((IOR)mutant_op).setParentEnv(parent_file_env[0], parent_comp_unit[0]);
				// return mutant_op;
				// }
				// }
				// } catch (ClassNotFoundException e)
				// {
				// System.out.println(" Exception at generating IOR mutant. File : AllMutantsGenerator.java ");
				// } catch (NullPointerException e1)
				// {
				// System.out.print(" IOP ^^; ");
				// }
			}
			case "ISD":
			{
				return new ISD(file_env, cdecl, comp_unit, className);
			}
			case "IOP":
			{
				return new IOP(file_env, cdecl, comp_unit, className);
			}
			case "IPC":
			{
				return new IPC(file_env, cdecl, comp_unit, className);
			}
			case "PNC":
			{
				return new PNC(file_env, cdecl, comp_unit, className);
			}
			case "PMD":
			{
				return new PMD(file_env, cdecl, comp_unit, className);
			}
			case "PPD":
			{
				return new PPD(file_env, cdecl, comp_unit, className);
			}
			case "PRV":
			{
				return new PRV(file_env, cdecl, comp_unit, className);
			}
			case "PCI":
			{
				return new PCI(file_env, cdecl, comp_unit, className);
			}
			case "PCC":
			{
				return new PCC(file_env, cdecl, comp_unit, className);
			}
			case "PCD":
			{
				return new PCD(file_env, cdecl, comp_unit, className);
			}
			case "JSD":
			{
				return new JSD(file_env, cdecl, comp_unit, className);
			}
			case "JSI":
			{
				return new JSI(file_env, cdecl, comp_unit, className);
			}
			case "JTD":
			{
				return new JTD(file_env, cdecl, comp_unit, className);
			}
			case "JTI":
			{
				return new JTI(file_env, cdecl, comp_unit, className);
			}
			case "JID":
			{
				return new JID(file_env, cdecl, comp_unit, className);
			}
			case "OAN":
			{
				return new OAN(file_env, cdecl, comp_unit, className);
			}
			case "EOA":
			{
				return new EOA(file_env, cdecl, comp_unit, className);
			}
			case "EOC":
			{
				return new EOC(file_env, cdecl, comp_unit, className);
			}
			case "EAM":
			{
				return new EAM(file_env, cdecl, comp_unit, className);
			}
			case "EMM":
			{
				return new EMM(file_env, cdecl, comp_unit, className);
			}
			// Exception
			case "EFD":
			{
				return new EFD(file_env, cdecl, comp_unit, className);
			}
			case "EHC":
			{
				return new EHC(file_env, cdecl, comp_unit, className);
			}
			case "EHD":
			{
				return new EHD(file_env, cdecl, comp_unit, className);
			}
			case "EHI":
			{
				return new EHI(file_env, cdecl, comp_unit, className);
			}
			case "ETC":
			{
				return new ETC(file_env, cdecl, comp_unit, className);
			}
			case "ETD":
			{
				return new ETD(file_env, cdecl, comp_unit, className);
			}
			// Method-level
			case "AORB":
			{
				return new AORB(file_env, cdecl, comp_unit, className);
			}
			case "AORS":
			{
				return new AORS(file_env, cdecl, comp_unit, className);
			}
			case "AODU":
			{
				return new AODU(file_env, cdecl, comp_unit, className);
			}
			case "AODS":
			{
				return new AODS(file_env, cdecl, comp_unit, className);
			}
			case "AOIU":
			{
				return new AOIU(file_env, cdecl, comp_unit, className);
			}
			case "AOIS":
			{
				return new AOIS(file_env, cdecl, comp_unit, className);
			}
			case "ROR":
			{
				return new ROR(file_env, cdecl, comp_unit, className);
			}
			case "COR":
			{
				return new COR(file_env, cdecl, comp_unit, className);
			}
			case "COD":
			{
				return new COR(file_env, cdecl, comp_unit, className);
			}
			case "COI":
			{
				return new COI(file_env, cdecl, comp_unit, className);
			}
			case "SOR":
			{
				return new SOR(file_env, cdecl, comp_unit, className);
			}
			case "LOR":
			{
				return new LOR(file_env, cdecl, comp_unit, className);
			}
			case "LOI":
			{
				return new LOI(file_env, cdecl, comp_unit, className);
			}
			case "LOD":
			{
				return new LOD(file_env, cdecl, comp_unit, className);
			}
			case "ASRS":
			{
				return new ASRS(file_env, cdecl, comp_unit, className);
			}
			case "SDL":
			{
				return new SDL(file_env, cdecl, comp_unit, className);
			}
			case "VDL":
			{
				return new VDL(file_env, cdecl, comp_unit, className);
			}
			case "CDL":
			{
				return new CDL(file_env, cdecl, comp_unit, className);
			}
			case "ODL":
			{
				return new ODL(file_env, cdecl, comp_unit, className);
			}
			default:
				throw new UnsupportedOperationException();
		}
	}

	
}
