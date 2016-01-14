/**
 * @FileName: LunaPlanner.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 7, 2015
 */
package luna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import luna.graphviz.Graphviz;
import luna.translate.Translator;
import luna.util.file.DirectoryList;
import luna.web.action.ExecuteAction;
import luna.web.model.PlannerOption;
import mynd.Global;
import mynd.MyNDPlanner;
import mynd.MyNDPlanner.Algorithm;
import mynd.problem.Problem;
import mynd.search.AbstractNode;
import mynd.search.AbstractSearch;

/**
 * @Description: Input: PPDDL file: Type: InputStream Output:
 * 
 *               Process: 1. Translate PPDDL into SAS 2. Execute 3. Return
 *               results
 * 
 */
public class LunaPlanner
{
	private static Logger logger = LogManager.getLogger();

	/**
	 * Enumeration of PDDL file type.
	 */
	public enum FileType
	{
		FOND, POND
	};

	// For test:
	public static void main(String[] args)
	{
		try
		{
			System.out
					.println("---------------------LunaPlanner.main()-----------------");

			LunaGlobal.LUNA_MAIN_DEBUG = true;
			// Parameters:
			String projectRootPath = LunaGlobal.projectRootPath;

			/*
			 * // 1. Translate System.out.println(
			 * "------------------------1. Translate-----------------------");
			 * String pythonPathname = projectRootPath +
			 * LunaGlobal.pythonPathname; String translatorPathname =
			 * projectRootPath + LunaGlobal.fondTranslatorPathname; String
			 * domainFileName = projectRootPath + LunaGlobal.domainFilePathname;
			 * String problemFileName = projectRootPath +
			 * LunaGlobal.problemFilePathname; FileType fileType =
			 * FileType.FOND;
			 * 
			 * translate(pythonPathname, translatorPathname, domainFileName,
			 * problemFileName, fileType);
			 */

			// 2. Execute
			System.out
					.println("------------------------2. Execute-----------------------");
			PlannerOption plannerOption = new PlannerOption();
			plannerOption.setPrintPlan("-dumpPlan");
			String algorithmString = "-laostarx";
			plannerOption.setSearchAlgorithm(algorithmString);
			// Local
			String resultString = execute(projectRootPath, plannerOption);
			System.out.println(resultString);

			// // Web
			// ExecuteAction action = new ExecuteAction();
			// System.out.println("result = " + action.execute());

			System.out
					.println("--------------------End LunaPlanner.main()---------------");
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
		}

	}

	// 1. Translate PPDDL into SAS
	public static String translate(String pythonPathname,
			String translatorPathname, String domainFilePathname,
			String problemFilePathname, FileType fileType)
	{
		try
		{
			String outputString = Translator.pddlToSAS(pythonPathname,
					translatorPathname, domainFilePathname, problemFilePathname,
					fileType);

			return outputString;
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 
	 * @Title: translate
	 * @Description: TODO
	 * @param @param projectRootPath
	 * @param @param domainFileName: e.g. retailer_manufacturer_domain.pddl
	 * @param @param problemFileName
	 * @param @return
	 * @return String
	 * @throws
	 */
	public static String translate(String projectRootPath,
			String domainFileName, String problemFileName)
	{
		try
		{
			String domainFilePathname = projectRootPath + LunaGlobal.uploadFileFolderName
					+ domainFileName;
			String problemFilePathname = projectRootPath + LunaGlobal.uploadFileFolderName
					+ problemFileName;

			String pythonPathname = projectRootPath + LunaGlobal.pythonPathname;
			
			String fondTranslatorPathname = projectRootPath
					+ LunaGlobal.fondTranslatorPathname;
			
			FileType fileType = LunaPlanner.FileType.FOND;

			String translatorOutputString = translate(pythonPathname, fondTranslatorPathname, domainFilePathname,
					problemFilePathname, fileType);

			return translatorOutputString;
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}

	}

	// 2. Execute
	/**
	 * 
	 * @Title: execute
	 * @Description:
	 * 
	 * @param @param args: PlannerOptions pathToSASFile
	 * @return void
	 * @throws
	 */
	public static String execute(String projectRootPath,
			PlannerOption plannerOption)
	{
		try
		{
			if (plannerOption == null)
			{
				plannerOption = new PlannerOption();
			}
			// -----------------------Initialize Planner----------------------------
			
			AbstractNode.setNextFreeIndex(0);
			AbstractSearch.NODE_EXPANSIONS = 0;
			
			// Reset to: planner_output\\
			LunaGlobal.plannerOutputFolderName = "planner_output\\";
			
			
			// Set problem = null, so every time execute LunaPlanner will create a new Problem:
			// and need to be preprocess:
			Problem.setInstance(null);
			
			// -----------------------End Initialize-----------------------------

			// Parse arguments and choose output file folder:
			String opt = plannerOption.getSearchAlgorithm();
			String subFolder;
			// 1. Search algorithms.
			if (opt.equals("-aostar"))
			{
				subFolder = Algorithm.AO_STAR.toString().toLowerCase();
			} else if (opt.equals("-laostar"))
			{
				subFolder = Algorithm.LAO_STAR.toString().toLowerCase();
			} else if (opt.equals("-laostarx"))
			{
				subFolder = Algorithm.LAO_STAR_X.toString().toLowerCase();
			} else
			// if (opt.equals("-bfsstar"))
			{
				subFolder = Algorithm.BFS_STAR.toString().toLowerCase();
			}

			String pathToSASFile = projectRootPath
					+ LunaGlobal.translatorOutputFilePathname;
			
			LunaGlobal.plannerOutputFolderName += subFolder + "\\";

			String plannerOutputFileName = projectRootPath
					+ LunaGlobal.plannerOutputFolderName
					+ LunaGlobal.plannerOutputFileName;

			logger.debug(plannerOutputFileName);
			logger.debug(pathToSASFile);

			String plannerOutputString;

			// Execute:
			String argsString = pathToSASFile;
			argsString = plannerOption.convertToCommandString() + " "
					+ pathToSASFile;

			logger.debug("argsString = " + argsString);
			String[] args = argsString.split(" ");

			logger.debug(args.length);

			// Redirect System.out
			PrintStream out = System.out;
			PrintStream err = System.err;
			PrintStream ps = new PrintStream(plannerOutputFileName);

			logger.debug(plannerOutputFileName);

			System.setOut(ps);
			System.setErr(ps);

			MyNDPlanner.main(args);

			// 2. Print final plan to png format:
			String sourceDOTfilePathname = LunaGlobal.projectRootPath
					+ LunaGlobal.plannerOutputFolderName + "final_plan.dot";
			String outputFileType = "png";
			String outputFilePath = LunaGlobal.projectRootPath
					+ LunaGlobal.plannerOutputFolderName;
			String outputFilePathname = LunaGlobal.projectRootPath
					+ LunaGlobal.plannerOutputFolderName + "final_plan.png";

			Graphviz.print(sourceDOTfilePathname, outputFileType,
					outputFilePathname);

			if (Global.SEARCH_DEBUG)
			{
				// Print all DOT files into PNG:
				DirectoryList dlList = new DirectoryList(".*\\.dot");
				File[] files = dlList.list(outputFilePath);

				for (File file : files)
				{
					Graphviz.print(file.getAbsolutePath(), outputFileType, file
							.getAbsolutePath().replace(".dot", ".png"));
				}
			}

			// Reset to standard out, err
			System.setOut(out);
			System.setErr(err);

			BufferedReader reader = new BufferedReader(new FileReader(
					plannerOutputFileName));
			String line;
			StringBuilder stringBuilder = new StringBuilder();

			while ((line = reader.readLine()) != null)
			{
				stringBuilder.append(line + "\n");

			}
			reader.close();
			plannerOutputString = stringBuilder.toString();

			// for test:
			System.out
					.println("-------------------plannerOutputString-----------------");
			System.out.println(plannerOutputString);
			System.out
					.println("-------------------End: plannerOutputString-----------------");

			return plannerOutputString;
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @Title: execute 
	 * @Description: TODO
	 * @param @param projectRootPath
	 * @param @return 
	 * @return String
	 * @throws
	 */
	public static String execute(String projectRootPath)
	{
		try
		{
			PlannerOption plannerOption = new PlannerOption();
			plannerOption.setSearchAlgorithm("-laostar");
		
			String resultString = execute(projectRootPath,plannerOption);
			return resultString;
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @Title: execute 
	 * @Description: TODO
	 * @param @param projectRootPath
	 * @param @param algrithmString: -laostar/-aostar
	 * @param @return 
	 * @return String
	 * @throws
	 */
	public static String execute(String projectRootPath, String algrithmString)
	{
		try
		{
			PlannerOption plannerOption = new PlannerOption();
			
			
			plannerOption.setSearchAlgorithm(algrithmString);
		
			String resultString = execute(projectRootPath,plannerOption);
			return resultString;
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	
	// 3. Return results
}
