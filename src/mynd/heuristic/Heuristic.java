package mynd.heuristic;

import mynd.Global;
import mynd.state.State;

/**
 * 
 * @author Robert Mattmueller
 * 
 */
public abstract class Heuristic
{

	/**
	 * How to accumulate heuristic values of sample world states in case of
	 * partial observability.
	 */
	public enum HeuristicStrategy
	{
		MAX, ADD, AVERAGE
	};

	/**
	 * Indicates an infinite heuristic value.
	 */
	public static final double INFINITE_HEURISTIC = Double.POSITIVE_INFINITY;

	/**
	 * Get heuristic value for given state.
	 * 
	 * @param state
	 *            state to be evaluated by heuristic
	 * @return heuristic value for given state
	 */
	public abstract double getHeuristic(State state);

	/**
	 * Aggregation strategy of heuristic value in case of partial observability
	 * to be used. By default maximization is used.
	 */
	public static HeuristicStrategy heuristicStrategy = HeuristicStrategy.MAX;

	/**
	 * Denotes how many world states of a belief state are sampled during
	 * search. Only used for FF-heuristic and for PDBs by assumption of full
	 * observability.
	 */
	public static int numberOfWorldStatesToBeSampled = 10;

	public Heuristic(boolean supportsAxioms)
	{
		if (!supportsAxioms)
		{
			if (Global.problem.numAxioms != 0)
			{
				System.err
						.println("Heuristic does not support axioms. Planner will be terminated.");
				System.exit(-1);
			}
		}
	}
}
