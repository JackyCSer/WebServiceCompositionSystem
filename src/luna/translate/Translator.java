/**
 * @FileName: Translator.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 8, 2015
 */
package luna.translate;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import luna.LunaPlanner;
import luna.LunaPlanner.FileType;

/**
 * @Description: PDDL to SAS translator
 */
public class Translator
{
	private static Logger logger = LogManager.getLogger();

	/**
	 * @param type
	 *            1: FOND, 2: POND
	 * @param pythonPathname
	 *            For example:
	 *            D:\Programs\Tomcat7\webapps\LunaPlanner\Python27\python.exe
	 *    
	 *            
	 * @return outputString from the Python translator
	 * 
	 *         Node: SAS file, Saved in: type1: FOND: ..\sas\output.sas type2:
	 *         POND: ..\sas\output.sas
	 */
	public static String pddlToSAS(String pythonPathname,
			String translatorPathname, String domainFilePathame,
			String problemFilePathname, FileType type)
	{
		try
		{
			logger.debug("type = " + type);
			logger.debug(domainFilePathame);
			logger.debug(problemFilePathname);

			logger.debug(translatorPathname);

			/*
			 * Example translator-fond/translate.py
			 * data/fond-pddl/blocksworld/domain.pddl
			 * data/fond-pddl/blocksworld/p1.pddl
			 */
			String command = pythonPathname + " " + translatorPathname + " "
					+ domainFilePathame + " " + problemFilePathname;

			// String command6 = "D:\\Programs\\Program Files\\"
			// + "Graphviz\\graphviz-2.38\\release\\bin\\gvedit.exe";

			logger.debug(command);

			Process process = Runtime.getRuntime().exec(command);

			logger.debug(process);

			// Get output
			BufferedReader output = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			logger.debug(output);

			String line;
			StringBuilder stringBuilder = new StringBuilder();

			while ((line = output.readLine()) != null)
			{
				stringBuilder.append(line + "\n");
			}
			output.close();

			String translatorOutputString = stringBuilder.toString();

			// for test:
			logger.debug("-------------------translatorOutputString-----------------");
			System.out.println(translatorOutputString);
			logger.debug("-------------------End: translatorOutputString-----------------");

			//process.waitFor(); // MUSTbe called immediately.

			return translatorOutputString;
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
			return null;
		}
	}

	// for test:
	public static void main(String[] args)
	{

		String command5 = " python .\\translator-pond\\translate.py "
				+ " D:\\Programs\\Tomcat7\\webapps\\LunaPlanner\\"
				+ "upload_pddl_files\\domain.pddl "
				+ " D:\\Programs\\Tomcat7\\webapps\\LunaPlanner\\"
				+ "upload_pddl_files\\blocksworld_p1.pddl";
		String domain = " D:\\Programs\\Tomcat7\\webapps\\LunaPlanner\\"
				+ "upload_pddl_files\\domain.pddl ";
		String problem = " D:\\Programs\\Tomcat7\\webapps\\LunaPlanner\\"
				+ "upload_pddl_files\\blocksworld_p1.pddl";

		String pythonPathname = "D:\\Programs\\Tomcat7\\webapps\\LunaPlanner"
				+ "\\Python27\\python.exe";

		String translatorPathname = "D:\\Programs\\Tomcat7\\webapps\\"
				+ "LunaPlanner\\translator-pond\\translate.py";

		logger.debug("-----------------Test--------------");
		pddlToSAS(pythonPathname, translatorPathname, domain, problem,
				LunaPlanner.FileType.POND);

	}

}
