package mynd.heuristic.pdb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import mynd.Global;
import mynd.heuristic.Heuristic;
import mynd.heuristic.graph.Node;
import mynd.state.State;
import mynd.symbolic.BeliefState;

/**
 * Pattern database.
 * 
 * @author Manuela Ortlieb
 */
public class BeliefStatePDB extends PDB
{

	/**
	 * Abstraction which is induced by given pattern.
	 */
	private final Abstraction abstraction;

	/**
	 * Mapping from abstract belief states to heuristic values.
	 */
	private Map<Integer, Double> patternDatabase;

	/**
	 * Set true for debug output information.
	 */
	public final static boolean DEBUG = false;

	/**
	 * Constructor.
	 * 
	 * @param pattern
	 *            The variables contained in the pattern.
	 */
	public BeliefStatePDB(Set<Integer> pattern)
	{
		long start = System.currentTimeMillis();
		assert !pattern.isEmpty();
		if (pdbMaxSize == -1) // max size is not set by an option
			pdbMaxSize = 10000;
		abstraction = Global.problem.abstractToPattern(pattern);
		if (DEBUG)
		{
			System.out.println("Computed abstraction for pattern " + pattern);
		}
		patternDatabase = new HashMap<Integer, Double>((int) Math.ceil(PDB
				.numAbstractStates(pattern) / 0.75));
		fillPDB();
		System.err.print("Created new Belief State PDB for variables ");
		System.err.println(pattern);
		System.err.println("in " + (System.currentTimeMillis() - start) / 1000
				+ "s");
		if (DEBUG)
		{
			System.out.println("pdb = " + patternDatabase);
		}
	}

	/**
	 * Actual computation and storage of abstract cost values
	 */
	private void fillPDB()
	{
		AbstractCostComputation comp = new AbstractCostComputation(abstraction);
		Collection<Node> nodes = comp.run();
		for (Node node : nodes)
		{
			if (node.getCostEstimate() != Node.UNINITIALIZED_COST_ESTIMATE)
			{
				patternDatabase.put(node.index, node.getCostEstimate());
			}
		}
	}

	/**
	 * Look up heuristic value in pattern database.
	 * 
	 * @param state
	 *            concrete state to evaluate
	 * @return heuristic value of the given state
	 */
	public double getHeuristic(State state)
	{
		BeliefState abstractedBeliefState = ((BeliefState) state)
				.abstractToPattern(abstraction);
		if (patternDatabase.containsKey(abstractedBeliefState.hashCode()))
		{
			return patternDatabase.get(abstractedBeliefState.hashCode());
		} else
		{
			return Double.POSITIVE_INFINITY;
		}
	}

	/**
	 * Get a string representation of this pattern database.
	 * 
	 * @return string representation of this pdb
	 */
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append(patternDatabase.toString());
		return buffer.toString();
	}

	/**
	 * Get average heuristic value of this pattern database. Infinity values are
	 * ignored.
	 * 
	 * @return average heuristic value
	 */
	@Override
	public double averageHeuristicValue()
	{
		double average = 0;
		for (double h : patternDatabase.values())
		{
			assert (h != Heuristic.INFINITE_HEURISTIC); // for Blocksworld
			average += h;
		}
		return (average / patternDatabase.size());
	}

}
