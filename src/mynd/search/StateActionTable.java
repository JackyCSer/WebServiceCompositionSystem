package mynd.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mynd.Global;
import mynd.explicit.ExplicitState;
import mynd.state.Operator;
import mynd.state.State;

/**
 * An explicit state-action table is a mapping from states to actions, aka. plan
 * aka. policy.
 * 
 * @author Robert Mattmueller
 * 
 */
public class StateActionTable
{

	class StateActionTableKey
	{

		/**
		 * State which is mapped to an applicable action.
		 */
		State state;

		/**
		 * Create a new state action table key.
		 * 
		 * @param state
		 *            which is mapped to an applicable action.
		 */
		StateActionTableKey(State state)
		{
			this.state = state;
		}

		/**
		 * String representation of this state action table key.
		 */
		@Override
		public String toString()
		{
			StringBuffer buffer = new StringBuffer();
			buffer.append(state.toString());
			return buffer.toString();
		}

	}

	/**
	 * A entry in a state-action table is a pair consisting of a condition (not
	 * necessarily representing a single but possibly multiple states) and an
	 * action. Conditions from multiple state-action table entries must not
	 * overlap, unless they lead to the same action.
	 * 
	 * @author Robert Mattmueller
	 * 
	 */
	class StateActionTableEntry
	{

		/**
		 * Condition describing a set of states.
		 */
		StateActionTableKey key;

		/**
		 * Action to be applied in the states represented by the condition.
		 */
		Operator operator;

		/**
		 * Construct a new state-action table entry from a given condition and
		 * action.
		 * 
		 * @param condition
		 *            The condition
		 * @param operator
		 *            The action
		 */
		StateActionTableEntry(StateActionTableKey state, Operator operator)
		{
			this.key = state;
			this.operator = operator;
		}
	}

	/**
	 * List of state-action table entries. This list is assumed to be
	 * consistent, i.e., there may be no two overlapping conditions unless they
	 * prescribe the same action. The consistency is, however, never strictly
	 * enforced.
	 */
	private List<StateActionTableEntry> entries;

	/**
	 * Set of seen states to assert that there is no duplicate entry.
	 */
	private Set<State> seenKeys;

	/**
	 * Set of seen operators.
	 */
	private Set<Operator> seenOperators;

	/**
	 * Construct a new state-action table for a given problem. Initially, the
	 * state-action table is empty.
	 * 
	 * @param problem
	 *            The problem to which this solution belongs.
	 */
	public StateActionTable()
	{
		entries = new ArrayList<StateActionTableEntry>(100);
		seenKeys = new HashSet<State>(100);
		seenOperators = new HashSet<Operator>(100);
	}

	/**
	 * Add an entry, i.e., a condition-action pair, to this state-action table.
	 * 
	 * @param vals
	 *            The condition values
	 * @param polarities
	 *            The desired polarities
	 * @param operator
	 *            The action
	 */
	public void addEntry(State state, Operator operator)
	{
		boolean newState = seenKeys.add(state);
		assert newState;
		seenOperators.add(operator);
		entries.add(new StateActionTableEntry(new StateActionTableKey(state),
				Global.problem.getOriginalOperatorMap().get(operator.getName())));
	}

	/**
	 * Dump this state-action table to standard out.
	 */
	public void dump()
	{
		System.out.println(toString());
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		for (StateActionTableEntry entry : entries)
		{
			State state = entry.key.state;
			buffer.append("if ");
			buffer.append(state);
			buffer.append("\n");
			buffer.append("then ");
			buffer.append(entry.operator);
			buffer.append(";");
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/**
	 * Assemble a string representing this state-action table in the plan output
	 * format used in the Uncertainty Part of the 6th International Planning
	 * Competition 2008.
	 * 
	 * @return Compact (yet unfactored) policy representation of this
	 *         state-action table.
	 */
	public String toStringPolicy()
	{
		StringBuffer buffer = new StringBuffer();
		List<List<String>> propositionNames = Global.problem.propositionNames;

		// // Propositions
		// Collect and enumerate proposition names. Skip negated propositions.
		// FIXME: Is this CWA correct/does it match the semantics of the IPC
		// format?
		List<String> allPropositions = new ArrayList<String>();
		Map<String, Integer> allPropositionsMap = new LinkedHashMap<String, Integer>();
		int i = 0;
		for (List<String> names : propositionNames)
		{
			for (String name : names)
			{
				if (!name.startsWith("(not ("))
				{ // FIXME ???
					allPropositions.add(name);
					allPropositionsMap.put(name, i);
					i++;
				}
				// else if (name.contains("axiom")) {
				// // TODO
				// }
			}
		}
		// Output proposition names.
		buffer.append(allPropositions.size());
		buffer.append(" ");
		for (String name : allPropositions)
		{
			buffer.append(name);
			buffer.append(" ");
		}
		buffer.append("\n%%\n");

		// // Operators
		// Collect and enumerate operator names.
		List<String> allOperators = new ArrayList<String>();
		Map<String, Integer> allOperatorsMap = new LinkedHashMap<String, Integer>();
		i = 0;
		for (StateActionTableEntry entry : entries)
		{
			String name = entry.operator.getName();
			if (!allOperatorsMap.containsKey(name))
			{
				allOperators.add(name);
				allOperatorsMap.put(name, i);
				i++;
			}
		}
		// Output operators
		buffer.append(allOperators.size());
		buffer.append(" ");
		for (String name : allOperators)
		{
			buffer.append("(");
			buffer.append(name);
			buffer.append(")");
			buffer.append(" ");
		}
		buffer.append("\n%%\n");

		// Actual policy
		buffer.append("policy ");
		buffer.append(entries.size());
		buffer.append(" ");

		// For each mapping from partial state to action ...
		for (StateActionTableEntry entry : entries)
		{
			String operator = entry.operator.getName();

			// ... determine size of partial state (number of satisfied
			// propositions) ...
			// FIXME: Is this correct?
			i = 0;
			Map<Integer, Integer> variableValuePairs = ((ExplicitState) entry.key.state).variableValueAssignment;
			for (int var : variableValuePairs.keySet())
			{
				int value = variableValuePairs.get(var);
				String name = propositionNames.get(var).get(value);
				if (!name.startsWith("(not ("))
				{
					// if (name.contains("axiom")) {
					// System.out.println("var " + var + " with name " + name);
					// System.out.println("val " + value);
					// System.out.println(Global.problem.axioms);
					// System.out.println(Global.problem.axiomLayer);
					// System.out.println("map " + axiomMap);
					// assert false;
					// }
					i++;
				}
			}

			// ... print that number ...
			buffer.append(i);
			buffer.append(" ");

			// ... print the propositions of that partial state (or rather their
			// indices as determined above) ...
			for (int var : variableValuePairs.keySet())
			{
				int value = variableValuePairs.get(var);
				String name = propositionNames.get(var).get(value);
				if (!name.startsWith("(not ("))
				{
					buffer.append(allPropositionsMap.get(name));
					buffer.append(" ");
				}
			}

			// ... and print the number of the operator to be applied in that
			// state.
			buffer.append(allOperatorsMap.get(operator));
			buffer.append(" ");
		}
		return buffer.toString();
	}

	/**
	 * Convert a variable-value pair into a PDDL representation. If a
	 * proposition name "propositionName" for the given variable-value pair is
	 * defined, return "(propositionName)", otherwise return
	 * "(= variableName value)", where variableName is the name of the given
	 * variable (index).
	 * 
	 * @param var
	 *            Variable index
	 * @param val
	 *            Variable value index
	 * @return PDDL representation of the fact that the given variable has the
	 *         given value.
	 */
	public String variableValuePairToString(int var, int val)
	{
		List<String> varNames = Global.problem.variableNames;
		List<List<String>> propositionNames = Global.problem.propositionNames;

		StringBuffer buffer = new StringBuffer();

		String propositionName = propositionNames.get(var).get(val);
		if (propositionName != "")
		{
			buffer.append(propositionName);
		} else
		{
			buffer.append("(= ");
			buffer.append(varNames.get(var));
			buffer.append(" ");
			buffer.append(val);
			buffer.append(")");
		}
		return buffer.toString();
	}

	public Operator findMatchingOperator(State state)
	{
		StateActionTableEntry entry = findMatchingEntry(state);
		return entry.operator;
	}

	private StateActionTableEntry findMatchingEntry(State state)
	{
		StateActionTableEntry goodEntry = null;
		for (StateActionTableEntry entry : entries)
		{
			if (state.equals(entry.key.state))
			{
				if (goodEntry != null && (goodEntry.operator != entry.operator))
				{
					System.err
							.println("WARNING: State-action table contains more than one");
					System.err.println("         entry for state " + state
							+ ".");
					System.err.println("         1st: " + goodEntry.operator);
					System.err.println("         2nd: " + entry.operator);
					System.err.println("         1st cond: " + goodEntry.key);
					System.err.println("         2nd cond: " + entry.key);
					System.err
							.println("         Arbitrarily using first entry and discarding second.");
					System.err
							.println("         Note that this can, of course, be suboptimal, in particular for Gamer.");
				} else
				{
					goodEntry = entry;
				}
			}
		}
		if (goodEntry == null)
		{
			System.err
					.println("ERROR: State-action table contains no entry for");
			System.err.println("       state " + state + ".");
			System.exit(1);
		}
		return goodEntry;
	}

	public int getNumberOfEntries()
	{
		return entries.size();
	}

	/**
	 * Get all sensing operators which are in the resulting policy (meaning
	 * there is a entry with it in this table).
	 * 
	 * @return set of sensing operators
	 */
	public Set<Operator> getSensingOperators()
	{
		Set<Operator> result = new HashSet<Operator>(seenOperators.size());
		for (Operator op : seenOperators)
		{
			if (op.isSensing)
			{
				result.add(op);
			}
		}
		return result;
	}

	/**
	 * Return the occurrences of sensing actions in the resulting policy. So if
	 * a sensing action is used n times, it is count n times.
	 * 
	 * @return occurrences of sensing
	 */
	public int getNumberOfSensingApplication()
	{
		int result = 0;
		for (StateActionTableEntry entry : entries)
		{
			if (entry.operator.isSensing)
			{
				result++;
			}
		}
		return result;
	}

	/**
	 * Get the set contained in the state action table.
	 * 
	 * @return
	 */
	public Set<State> getStates()
	{
		return seenKeys;
	}

	public Set<Operator> getOperators()
	{
		return seenOperators;
	}

}
