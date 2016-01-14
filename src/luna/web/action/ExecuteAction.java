/**
 * @FileName: ExecuteAction.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 8, 2015
 */
package luna.web.action;

import java.awt.Graphics;
import java.util.Set;

import org.apache.struts2.ServletActionContext;

import luna.LunaGlobal;
import luna.LunaPlanner;
import luna.LunaPlanner.FileType;
import luna.graphviz.Graphviz;
import luna.util.JsonConverter;
import luna.web.model.LunaPlannerOutput;
import luna.web.model.PlannerOption;
import mynd.Global;

/**
 * @Description:
 */
public class ExecuteAction extends LunaActionBase
{

	private static final long serialVersionUID = 1L;

	// Input 1: PPDDL file , Type: InputStream
	private PlannerOption plannerOption;

	// Output:
	// Fields for packaging into JSON:
	private String plannerOutputString;
	private LunaPlannerOutput plannerOutputInfo;

	// TODO
	// private WeblogicDynamicInfo weblogicDynamicInfo;

	@Override
	public String execute()
	{
		try
		{
			// for test
			logger.entry();
			logger.debug(plannerOption);
			logger.debug("parameters = " + parameters);

			String projectRootPath = ServletActionContext.getServletContext()
					.getRealPath("/");

			// for test:
			// String projectRootPath = LunaGlobal.projectRootPath;

			// 1. Validating input:

			// parameters MAY be null.
			// if (parameters == null)
			// {
			// logger.error("输入参数为空。");
			// return ERROR;
			// }

			// 2. Converting query parameters:
			if (parameters != null)
			{
				plannerOption = JsonConverter.jsonToBean(parameters,
						PlannerOption.class);
			}

			logger.debug("plannerOption = " + plannerOption);
			// 3. Logging

			// 4. Executing
			// TODO
			// Set default argument: dumpPlan
			if (plannerOption == null)
			{
				plannerOption = new PlannerOption();

			}
			plannerOption.setPrintPlan("-dumpPlan");

			logger.info("Execute: \nPlannerOptions:" + plannerOption);

			// 5.Set result
			plannerOutputString = LunaPlanner.execute(projectRootPath,
					plannerOption);

			plannerOutputInfo = Global.problem.lunaPlannerOutput;

			// 6. Return
			return SUCCESS;
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
			return ERROR;
		}

	}

	public PlannerOption getPlannerOption()
	{
		return plannerOption;
	}

	public void setPlannerOption(PlannerOption plannerOption)
	{
		this.plannerOption = plannerOption;
	}

	public String getPlannerOutputString()
	{
		return plannerOutputString;
	}

	public void setPlannerOutputString(String plannerOutputString)
	{
		this.plannerOutputString = plannerOutputString;
	}

	public LunaPlannerOutput getPlannerOutputInfo()
	{
		return plannerOutputInfo;
	}

	public void setPlannerOutputInfo(LunaPlannerOutput plannerOutputInfo)
	{
		this.plannerOutputInfo = plannerOutputInfo;
	}

}
