package mynd.heuristic.pdb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import mynd.Global;
import mynd.explicit.ExplicitState;
import mynd.heuristic.graph.Node;
import mynd.state.State;

/**
 * Pattern database.
 * 
 * @author Robert Mattmueller
 */
public class ExplicitStatePDB extends PDB
{

	/**
	 * Sorted pattern, i.e., set of state variables.
	 */
	private final TreeSet<Integer> pattern;

	/**
	 * Mapping from (perfect) hash values of abstract states to heuristic
	 * values.
	 */
	private double[] patternDatabase;

	/**
	 * Set true for debug output information.
	 */
	public static final boolean DEBUG = false;

	/**
	 * Constructor.
	 * 
	 * @param pattern
	 *            The variables contained in the pattern.
	 */
	public ExplicitStatePDB(Set<Integer> pattern)
	{
		long start = System.currentTimeMillis();
		assert !pattern.isEmpty();
		if (DEBUG)
		{
			System.out.print("Create new ExplicitStatePDB for variables ");
			for (int var : pattern)
			{
				System.out.print(var + " ");
			}
		}
		if (pdbMaxSize == -1) // max size is not set by an option
			pdbMaxSize = 100000;
		// This pattern has to be sorted for computation of abstract hash code.
		this.pattern = new TreeSet<Integer>(pattern);
		if (DEBUG)
		{
			System.out.println("Sorted pattern: " + this.pattern);
		}
		System.err.println();
		initializePatternDatabase();
		fillPDB();
		System.err.print("Created new ExplicitStatePDB for variables ");
		for (int var : pattern)
		{
			System.err.print(var + " ");
		}
		System.err.println("\nin " + (System.currentTimeMillis() - start)
				/ 1000 + "s");
		System.err.println();
		if (DEBUG)
		{
			// System.out.println("Average heuristic value is " +
			// averageHeuristicValue());
			System.out.println("PDB = " + this);
		}
	}

	/**
	 * Initialize pattern database.
	 */
	private void initializePatternDatabase()
	{
		patternDatabase = new double[PDB.numAbstractStates(pattern)];
		Arrays.fill(patternDatabase, Double.POSITIVE_INFINITY);
	}

	/**
	 * Compute hash code of the abstraction of the given state with respect to
	 * the pattern of this PDB.
	 * 
	 * Note: This must be consistent with the hash code one would get by
	 * explicitly abstracting the state to the pattern and calling the abstract
	 * state's hashCode() method.
	 * 
	 * @param state
	 *            Concrete state whose abstraction should be hashed
	 * @return Hash code of abstraction of the given concrete state
	 */
	private int abstractHashCode(ExplicitState state)
	{
		int hashCode = 0;
		int oldVar = -1;
		for (int var : pattern)
		{
			assert var > oldVar;
			hashCode *= Global.problem.domainSizes.get(var);
			hashCode += state.variableValueAssignment.get(var);
			oldVar = var;
		}
		return hashCode;
	}

	/**
	 * Actual computation and storage of abstract cost values
	 */
	private void fillPDB()
	{
		Abstraction abstraction = Global.problem.abstractToPattern(pattern);
		// abstraction.dump();
		AbstractCostComputation comp = new AbstractCostComputation(abstraction);
		Collection<Node> nodes = comp.run();
		for (Node node : nodes)
		{
			if (node.getCostEstimate() != Node.UNINITIALIZED_COST_ESTIMATE)
			{
				patternDatabase[node.getState().hashCode()] = node
						.getCostEstimate();
			}
		}
		if (DEBUG)
		{
			System.out.println();
			System.out.println("fillPDB()");
			System.out.println("pdb lenght " + patternDatabase.length);

			System.out.println("Nodes:");
			for (Node node : nodes)
			{
				System.out.print(node);
				if (node != null)
				{
					System.out.println(" cost estimate: "
							+ node.getCostEstimate() + " / state: "
							+ node.getState());
					System.out.println(" node hash code: "
							+ node.getState().hashCode());
				} else
				{
					System.out.println();
				}
			}
			System.out.println(this);
			System.out.println();
		}
	}

	/**
	 * Look up heuristic value in pattern database.
	 * 
	 * @param state
	 *            Concrete state to evaluate.
	 * @return Heuristic value of the given state.
	 */
	public double getHeuristic(State state)
	{
		ExplicitState s = (ExplicitState) state;
		if (abstractHashCode(s) >= patternDatabase.length)
		{
			System.err
					.println("abstract hash code    = " + abstractHashCode(s));
			System.err.println("pattern database size = "
					+ patternDatabase.length);
		}
		return patternDatabase[abstractHashCode(s)];
	}

	/**
	 * Get string representation of this pattern database.
	 * 
	 * @return string representation of this pattern database
	 */
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("Pattern Database:\n");
		buffer.append("[ ");
		for (int num = 0; num < patternDatabase.length; num++)
		{
			buffer.append(num + ":" + patternDatabase[num] + ", ");
		}
		return buffer.substring(0, buffer.length() - 2) + " ]";

	}

	/**
	 * Compute average heuristic value. Infinity values are ignored.
	 * 
	 * @return average heuristic value
	 */
	@Override
	public double averageHeuristicValue()
	{
		double average = 0;
		for (int i = 0; i < patternDatabase.length; i++)
		{
			// assert (patternDatabase[i] != Double.POSITIVE_INFINITY); // true
			// for Blocksworld
			if (patternDatabase[i] != Double.POSITIVE_INFINITY)
			{
				average += patternDatabase[i];
			}
		}
		return (average / patternDatabase.length);
	}
}
