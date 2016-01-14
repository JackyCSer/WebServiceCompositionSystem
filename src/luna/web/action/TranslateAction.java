/**
 * @FileName: TranslateAction.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 7, 2015
 */
package luna.web.action;

import java.io.InputStream;
import java.util.Map;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionContext;

import luna.LunaGlobal;
import luna.LunaPlanner;
import luna.LunaPlanner.FileType;
import luna.util.JsonConverter;

/**
 * @Description:
 * @param 1. InputStream: PPDDL file
 * @param 2. String: Planner Options
 * 
 * @return Planner Output
 */
public class TranslateAction extends LunaActionBase
{

	private static final long serialVersionUID = 1L;

	// Input 1: PPDDL file , Type: InputStream


	// Output:
	// Fields for packaging into JSON:
	private String translatorOutput;
	
	// TODO
	// private WeblogicDynamicInfo weblogicDynamicInfo;

	@Override
	public String execute()
	{
		try
		{
			// for test
			logger.entry();
			
			String projectRootPath = ServletActionContext.getServletContext()
					.getRealPath("/");

			String pythonPathname = projectRootPath + LunaGlobal.pythonPathname;	
			String fondTranslatorPathname = projectRootPath + LunaGlobal.fondTranslatorPathname;	
			String pondTranslatorPathname = projectRootPath + LunaGlobal.pondTranslatorPathname; 
			
			// 1. Validating input:
			if (parameters == null)
			{
				logger.error("输入参数为空。");
				return ERROR;
			}

			// 2. Converting query parameters:
			ActionContext actionContext = ActionContext.getContext();
			Map<String, Object> sessionMap = actionContext.getSession();
			String domainFileName = (String) sessionMap.get("domainFileName");
			String problemFileName = (String) sessionMap.get("problemFileName");
			
			paramMap = JsonConverter.jsonToMap(parameters);
			logger.debug(paramMap);
			String fileTypeString = paramMap.get("fileType");
			// Convert String to Enum
			FileType fileType = FileType.valueOf(fileTypeString);

			String translatorPathname;
			if (fileType == LunaPlanner.FileType.FOND)
			{
				translatorPathname = fondTranslatorPathname;
			}
			else {
				translatorPathname = pondTranslatorPathname;
			}
			// logger.debug("serverName=" + serverName);

			// 3. Logging

			// 4. Executing
			// TODO
			logger.info("Translate PDDL file: \nDomain:" + domainFileName
					+ "\nProblem:" + problemFileName + "\nfileType: "
					+ fileTypeString);

			translatorOutput = LunaPlanner.translate(pythonPathname, translatorPathname, domainFileName,
					problemFileName, fileType);

			return SUCCESS;
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
			return ERROR;
		}

	}
	
	// Getters & Setters
	public String getTranslatorOutput()
	{
		return translatorOutput;
	}

	public void setTranslatorOutput(String translatorOutput)
	{
		this.translatorOutput = translatorOutput;
	}




}
