package mynd.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mynd.Global;
import mynd.explicit.ExplicitState;

/**
 * An abstract search engine.
 * 
 * @author Robert Mattmueller
 */
public abstract class AbstractSearch
{
	/**
	 * For test
	 */
	public static Logger logger = LogManager.getLogger();

	/**
	 * Value indicating that the problem has been neither solved nor proven
	 * unsolvable yet.
	 */
	public static final int UNDECIDED = -1;

	/**
	 * Return value indicating existence of a winning strategy for the
	 * protagonist
	 */
	public static final int PROTAGONIST_WINS = 0;

	/**
	 * Return value indicating non-existence of a winning strategy for the
	 * protagonist
	 */
	public static final int ANTAGONIST_WINS = 1;

	/**
	 * Return value indicating time-out
	 */
	public static final int TIMEOUT = 2;

	/**
	 * Indicates that no time-out is used.
	 */
	private static final long NO_TIMEOUT = Long.MAX_VALUE;

	/**
	 * System time when search started.
	 */
	protected long starttime;

	/**
	 * System time when search ended.
	 */
	public long endtime;

	/**
	 * Search time-out.
	 */
	long timeout = AbstractSearch.NO_TIMEOUT;

	/**
	 * Counter for node expansions.
	 */
	public static int NODE_EXPANSIONS = 0;

	
	/**
	 * Start node of the search.
	 */
	protected AbstractNode initialNode;

	/**
	 * Dump the plan in an arbitrary format.
	 */
	public abstract void dumpPlan();

	/**
	 * Print Final Explict Graph in a dot format. <br/>
	 * Similiar to method: AOStartSearch.dumpStateSpace()
	 * 
	 */
	public abstract void printFinalExplictGraph();

	/**
	 * Print a plan in the competition policy format, if one has been found.
	 */
	public void getPlanAsPolicy()
	{
		assert (Global.problem.isFullObservable);
		System.err.println("Dump plan...");
		System.out.println(getExplicitStateActionTable().toStringPolicy());
		System.err.println("Done");
	}

	/**
	 * Print a plan, if one has been found.
	 */
	public void getPlanAsDebugOutput()
	{
		System.err.println("Dump plan...");
		System.err.println(getExplicitStateActionTable().toString());
	}

	/**
	 * Return a plan in the form of an explicit state-luna.action table.
	 * 
	 * @return Explicit state luna.action table representing the plan that was
	 *         found.
	 */
	public abstract StateActionTable getExplicitStateActionTable();

	/**
	 * Perform a complete run of the search algorithm.
	 * 
	 * @return Indicator of result. <tt>AbstractSearch.PROTAGONIST_WINS</tt> if
	 *         the protagonist provably wins,
	 *         <tt>AbstractSearch.ANTAGONIST_WINS</tt> if the antagonist
	 *         provably wins, and <tt>AbstractSearch.TIMEOUT</tt> if time-out
	 *         occurred before proof.
	 */
	public abstract int run();

	/**
	 * Set the time-out for the search.
	 * 
	 * @param timeout
	 *            Time-out in milliseconds
	 */
	public void setTimeout(long timeout)
	{
		assert timeout > 0;
		this.timeout = timeout;
	}

	/**
	 * Check whether a time-out has occurred.
	 * 
	 * @return True iff a time-out has been set and has been exceeded.
	 */
	protected boolean timeout()
	{
		if (timeout == AbstractSearch.NO_TIMEOUT)
		{
			return false;
		}
		return System.currentTimeMillis() - starttime > timeout;
	}

	/**
	 * Print statistics about the search.
	 * 
	 * @param simulatePlan
	 *            Indicates if the plan should be simulated to compute costs.
	 */
	public abstract void printStats(boolean simulatePlan);

	public abstract String printFinalPlanPath();



}
