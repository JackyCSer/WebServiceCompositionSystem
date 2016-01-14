/**
 * @FileName: LunaPlannerOutput.java
 * @Description: 
 * @author Jacky ZHANG
 * @date Apr 24, 2015
 */
package luna.web.model;

/**
 * @Description:
 */
public class LunaPlannerOutput
{
	private String result; // INITIAL IS PROVEN!
	private String resultType; // Strong cyclic plan found

	private String timeNeededForPreprocess; // (Parsing, PDBs, ...): 0.016
											// seconds.
	private String timeNeededForSearch; // : 0.266 seconds.
	private String timeNeeded; // : 0.282 seconds.

	private int totalNodeNumber; // : Out of 50 nodes, 21 are proven
	private int provenNodeNumber;
	private int expandedNodeNumber; // : 28

	/*
	 * policyEntries: 16 Number of sensing applications in policy: 0 8
	 * (assemble_yes) (avail_inventory_nd) (avail_inventory_no)
	 * (avail_market_nd) (avail_supplier_nd) (avail_supplier_no) (avail_yes)
	 * (ship_yes) %% 6 (select_manufacturer) (check_market) (assemble) (ship)
	 * (check_inventory) (check_supplier) %% policy 16 0 0 1 3 1 2 3 6 2 3 0 3 6
	 * 3 1 1 4 2 1 6 2 3 0 1 6 3 2 1 2 4 3 1 2 6 2 4 0 1 2 6 3 1 4 5 2 4 6 2 3 0
	 * 4 6 3 2 4 5 5 3 4 5 6 2 4 0 4 5 6 3 Total Garbage Collections: 1
	 */

	private String totalGarbageCollectionTime; // (ms): 23

	// Path
	private String finaPlanPath;
	private int solutionNumber;

	public String getResult()
	{
		return result;
	}

	public void setResult(String result)
	{
		this.result = result;
	}

	public String getTimeNeededForPreprocess()
	{
		return timeNeededForPreprocess;
	}

	public void setTimeNeededForPreprocess(String timeNeededForPreprocess)
	{
		this.timeNeededForPreprocess = timeNeededForPreprocess;
	}

	public String getTimeNeededForSearch()
	{
		return timeNeededForSearch;
	}

	public void setTimeNeededForSearch(String timeNeededForSearch)
	{
		this.timeNeededForSearch = timeNeededForSearch;
	}

	public String getTimeNeeded()
	{
		return timeNeeded;
	}

	public void setTimeNeeded(String timeNeeded)
	{
		this.timeNeeded = timeNeeded;
	}

	public String getTotalGarbageCollectionTime()
	{
		return totalGarbageCollectionTime;
	}

	public void setTotalGarbageCollectionTime(String totalGarbageCollectionTime)
	{
		this.totalGarbageCollectionTime = totalGarbageCollectionTime;
	}

	public String getResultType()
	{
		return resultType;
	}

	public void setResultType(String resultType)
	{
		this.resultType = resultType;
	}

	public String getFinaPlanPath()
	{
		return finaPlanPath;
	}

	public void setFinaPlanPath(String finaPlanPath)
	{
		this.finaPlanPath = finaPlanPath;
	}

	public int getTotalNodeNumber()
	{
		return totalNodeNumber;
	}

	public void setTotalNodeNumber(int totalNodeNumber)
	{
		this.totalNodeNumber = totalNodeNumber;
	}

	public int getProvenNodeNumber()
	{
		return provenNodeNumber;
	}

	public void setProvenNodeNumber(int provenNodeNumber)
	{
		this.provenNodeNumber = provenNodeNumber;
	}

	public int getExpandedNodeNumber()
	{
		return expandedNodeNumber;
	}

	public void setExpandedNodeNumber(int expandedNodeNumber)
	{
		this.expandedNodeNumber = expandedNodeNumber;
	}

	public int getSolutionNumber()
	{
		return solutionNumber;
	}

	public void setSolutionNumber(int solutionNumber)
	{
		this.solutionNumber = solutionNumber;
	}

}
