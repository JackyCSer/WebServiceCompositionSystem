/**
 * @FileName: Graphviz.java
 * @Description: 
 * @author Jacky ZHANG
 * @date May 3, 2015
 */
package luna.graphviz;

import luna.LunaGlobal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Description:
 */
public class Graphviz
{
	private static Logger logger = LogManager.getLogger();
	private static String graphvizRootPath = LunaGlobal.graphvizRootPath;

	// for test:
	public static void main(String[] args)
	{
		System.out.println("----------------Graphviz main()-------------------");
		
		String sourceDOTfilePathname = "D:\\Programs\\Tomcat7\\webapps\\LunaPlanner\\"
				+ "planner_output\\final_plan.dot";
		String outputFileType = "png";
		String outputFilePathname = "D:\\final_plan_png.png";
		print(sourceDOTfilePathname, outputFileType, outputFilePathname);
		
		
		System.out.println("----------------End Graphviz main()-------------------");
	}

	public static void print(String sourceDOTfilePathname,
			String outputFileType, String outputFilePathname)
	{
		try
		{
			// e.g. dot final_plan.dot -Tpng -o D:\final_plan.png
			String command = graphvizRootPath + "dot " + sourceDOTfilePathname
					+ " " + "-T" + outputFileType + " -o " + outputFilePathname;
			Runtime.getRuntime().exec(command);
		} catch (Exception e)
		{
			e.printStackTrace();
			logger.error(e);
		}

	}
}
