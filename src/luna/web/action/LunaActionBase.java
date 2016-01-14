/**
 * @FileName: LunaActionBase.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 7, 2015
 */
package luna.web.action;

import java.util.Map;

import luna.web.model.LunaParameters;
import luna.web.service.inter.LunaServiceInter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opensymphony.xwork2.ActionSupport;


/**
 * @Description: 
 */
public class LunaActionBase extends ActionSupport
{
	private static final long serialVersionUID = 1L;
	
	// Fields for Spring injecting
	protected Logger logger = LogManager.getLogger();

	// Input: Fields for receiving input:
	protected String parameters;


	protected Map<String, String> paramMap;


	public Logger getLogger()
	{
		return logger;
	}

	public void setLogger(Logger logger)
	{
		this.logger = logger;
	}

	public String getParameters()
	{
		return parameters;
	}

	public void setParameters(String parameters)
	{
		this.parameters = parameters;
	}

	public Map<String, String> getParamMap()
	{
		return paramMap;
	}

	public void setParamMap(Map<String, String> paramMap)
	{
		this.paramMap = paramMap;
	}

}
