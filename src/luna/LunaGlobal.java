/**
 * @FileName: Global.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 8, 2015
 */
package luna;

import mynd.Global;

/**
 * @Description: Class used to hold static global settings.
 */
public class LunaGlobal
{
	// Global settings:

	/**
	 * Debug setting for global
	 */
	public static boolean LUNA_MAIN_DEBUG = false;
	
	// For project:
	public static final String projectRootPath = "D:\\Programs\\Tomcat7\\webapps\\LunaPlanner\\";

	// For Graphviz:
	public static final String graphvizRootPath = projectRootPath
			+ "MyGraphviz\\release\\bin\\";

	// For Python Translators:
	public static final String pythonPathname = "Python27\\python.exe";

	public static final String pondTranslatorPathname = "translator-pond\\translate.py";

	public static final String fondTranslatorPathname = "translator-fond\\translate.py";

	public static final String translatorOutputFilePathname = "translator_output\\output.sas";

	public static final String uploadFileFolderName = "upload_pddl_files\\";
	
	// For planner:
	// planner_output\\ao_star, lao_star, bfs_star, lao_star_x
	public static String plannerOutputFolderName = "planner_output\\";

	public static final String plannerOutputFileName = "planner_output.txt";

}
