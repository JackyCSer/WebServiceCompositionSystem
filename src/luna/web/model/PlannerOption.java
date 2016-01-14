/**
 * @FileName: PlannerOption.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 8, 2015
 */
package luna.web.model;

/**
 * @Description:
 */
public class PlannerOption
{
	private String searchAlgorithm; // -aostar, -laostar, -bfsstar, -laostarx
	private String heuristics; //
	private String printPlan = "-dumpPlan"; // default: -dumpPlan

	
	
	public PlannerOption()
	{
		// Default:
		this.searchAlgorithm = "-bfsstar";
		
	}
	
	
	@Override
	public String toString()
	{
		return "PlannerOption [searchAlgorithm=" + searchAlgorithm
				+ ", heuristics=" + heuristics + ", printPlan=" + printPlan
				+ "]";
	}

	public String getPrintPlan()
	{
		return printPlan;
	}

	public void setPrintPlan(String printPlan)
	{
		this.printPlan = printPlan;
	}

	/**
	 * Caution:
	 */
	public String convertToCommandString()
	{
		String string = "";
		if (validate())
		{
			if (this.searchAlgorithm != null && this.searchAlgorithm != "")
			{
				string += this.searchAlgorithm + " ";
			}

			if (this.heuristics != null && this.heuristics != "")
			{
				string += this.heuristics + " ";
			}
			
			if (this.printPlan != null && this.printPlan == "-dumpPlan")
			{
				string += this.printPlan + " ";
			}

		}
		return string.trim();
	}

	public boolean validate()
	{
		return true;
	}

	public String getSearchAlgorithm()
	{
		return searchAlgorithm;
	}

	public void setSearchAlgorithm(String searchAlgorithm)
	{
		this.searchAlgorithm = searchAlgorithm;
	}

	public String getHeuristics()
	{
		return heuristics;
	}

	public void setHeuristics(String heuristics)
	{
		this.heuristics = heuristics;
	}

}
