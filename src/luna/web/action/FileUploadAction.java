/**
 * @FileName: FileUploadAction.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 8, 2015
 */
package luna.web.action;

import java.io.File;
import java.util.Map;

import luna.util.JsonConverter;

import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionContext;

/**
 * @Description:
 */
public class FileUploadAction extends LunaActionBase
{

	private static final long serialVersionUID = 1L;

	private File domain;
	private File problem;
	private String domainFileName;
	private String problemFileName;

	@Override
	public String execute()
	{

		try
		{
			// for test
			logger.entry();
			String uploadFilePath = ServletActionContext.getServletContext()
					.getRealPath("/upload_pddl_files");
			
			logger.debug("uploadFilePath = " + uploadFilePath);

			ActionContext actionContext = ActionContext.getContext();
			Map<String, Object> sessionMap = actionContext.getSession();

			// 1. Validating input:
			if (domainFileName == null || problemFileName == null)
			{
				logger.error("输入参数为空。");
				return ERROR;
			}

			// 2. Converting query parameters:
			logger.debug(uploadFilePath);

			File savedDomainFile = new File(new File(uploadFilePath),
					domainFileName);
			File savedProblemFile = new File(new File(uploadFilePath),
					problemFileName);

			if (!savedDomainFile.getParentFile().exists())
			{
				savedDomainFile.getParentFile().mkdirs();
			}

			FileUtils.copyFile(domain, savedDomainFile);
			FileUtils.copyFile(problem, savedProblemFile);

			// Session Scope

			String savedDomainFilePathname = savedDomainFile.getAbsolutePath();
			String savedProblemPathname = savedProblemFile.getAbsolutePath();
			
			sessionMap.put("domainFileName", savedDomainFilePathname);
			sessionMap.put("problemFileName", savedProblemPathname);

			logger.debug(savedDomainFilePathname);
			logger.debug(savedProblemPathname);

			return SUCCESS;
		} catch (Exception e)
		{
			logger.error(e);
			e.printStackTrace();
			return ERROR;
		}
	}

	// Getters & Setters

	public String getDomainFileName()
	{
		return domainFileName;
	}

	public File getDomain()
	{
		return domain;
	}

	public void setDomain(File domain)
	{
		this.domain = domain;
	}

	public File getProblem()
	{
		return problem;
	}

	public void setProblem(File problem)
	{
		this.problem = problem;
	}

	public void setDomainFileName(String domainFileName)
	{
		this.domainFileName = domainFileName;
	}

	public String getProblemFileName()
	{
		return problemFileName;
	}

	public void setProblemFileName(String problemFileName)
	{
		this.problemFileName = problemFileName;
	}

}
