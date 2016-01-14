/**
 * @ClassName: LunaParameters
 * @Description: 
 * @author Jacky ZHANG
 * @date 17 Aug 2014
 */
package luna.web.model;

/**
 * @Description:
 */
public class LunaParameters
{
	// Parameters for upload PDDL files
	private String domainFileName;
	private String problemFileName;
	private String fileType; // FOND/POND

	
	
	// Getters & Setters
	public String getDomainFileName()
	{
		return domainFileName;
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

	public String getFileType()
	{
		return fileType;
	}

	public void setFileType(String fileType)
	{
		this.fileType = fileType;
	}

}
