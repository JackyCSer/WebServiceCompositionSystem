package mynd.explicit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mynd.Global;
import mynd.heuristic.pdb.Abstraction;
import mynd.state.Condition;
import mynd.state.State;
import mynd.util.Pair;

/**
 * A condition is a set (conjunction) of variable-value pairs.
 * 
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class ExplicitCondition implements Condition
{
	/**
	 * Set true to get more information as output.
	 */
	public final boolean DEBUG = Global.EXPLICIT_CONDITION_DEBUG;

	/**
	 * Mapping from variables to values.
	 */
	public final Map<Integer, Integer> variableValueMap;

	/**
	 * Number of variables to which this condition refers.
	 */
	public final int size;

	/**
	 * Create a condition which is an assignment of variables to values.
	 * 
	 * @param variableValueMap
	 *            mapping from variables to values
	 */
	public ExplicitCondition(Map<Integer, Integer> variableValueMap)
	{
		this.variableValueMap = Collections.unmodifiableMap(variableValueMap);
		this.size = variableValueMap.size();
	}

	/**
	 * Get variable-value assignment of this condition as integer pairs.
	 * 
	 * @return Variable-value assignment as list of pairs
	 */
	public List<Pair<Integer, Integer>> getVariableValueAssignmentAsPairs()
	{
		ArrayList<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
		for (int var : variableValueMap.keySet())
		{
			result.add(new Pair<Integer, Integer>(var, variableValueMap
					.get(var)));
		}
		return result;
	}

	/**
	 * Test if this condition is tautologically true.
	 * 
	 * @return true iff this condition is tautologically true
	 */
	public boolean isTrue()
	{
		return size == 0;
	}

	/**
	 * Tests if this condition is satisfied by the given state.
	 * 
	 * @param state
	 * @return true iff this condition is satisfied by the given state.
	 * @modified 
	 * @author Lavender
	 * 
	 */
	public boolean isSatisfiedIn(State state)
	{
		// TODO avoid casts!
		// Modified: 2015-4-24 00:02:01
		// ----------------------------Modified--------------------------------
		
		
		return isSatisfiedIn(((ExplicitState) state).ownVariableValueAssignment);
		
		// ------------------------------End Modified--------------------------
	}

	// ------------------------Modified for LAO*------------------------------
	/**
	 * Tests if this condition is satisfied by the given state.
	 * 
	 * @param state
	 * @return true iff this condition is satisfied by the given state.
	 */
	public boolean isSatisfiedInForLAOStar(State state)
	{
		// TODO avoid casts!
		return isSatisfiedIn(((ExplicitState) state).variableValueAssignment);
	}
	
	// --------------------------End------------------------------------------------
	
	
	
	/**
	 * Tests if this condition is satisfied by the given assignment.
	 * 
	 * @param variableValueAssingment
	 *            variable assignment
	 * @return true iff this condition is satisfied in the given assignment
	 */
	boolean isSatisfiedIn(Map<Integer, Integer> variableValueAssignment)
	{
		if (DEBUG)
		{
			System.out.println("Tests if this condition: "+ this 
					+" is satisfied by the given assignment: "
					+ variableValueAssignment);
		}
		// for (int var : variableValueAssingment.keySet()) {
		// if (this.variableValueMap.containsKey(var) &&
		// this.variableValueMap.get(var) != variableValueAssingment.get(var)) {
		// return false;
		// }
		// }
		// return true;

		for (int var : variableValueMap.keySet())
		{
			if (variableValueAssignment.get(var).intValue() != variableValueMap
					.get(var).intValue())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Abstraction of this condition to given set of variables. Returns the
	 * abstracted condition.
	 * 
	 * @param pattern
	 *            set of variables
	 * @return abstracted explicit condition induced by given pattern
	 */
	public ExplicitCondition abstractToPattern(Set<Integer> pattern)
	{
		return new ExplicitCondition(Abstraction.abstractVariableValueMap(
				variableValueMap, pattern));
	}

	/**
	 * Test if given object and this condition are equal.
	 * 
	 * @param obj
	 *            other object
	 * @return true iff given object and this condition are equal
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ExplicitCondition))
		{
			return false;
		}
		return variableValueMap
				.equals(((ExplicitCondition) obj).variableValueMap);
	}

	/**
	 * Get hash code of this condition.
	 * 
	 * @return hash code
	 */
	@Override
	public int hashCode()
	{
		return variableValueMap.hashCode();
	}

	/**
	 * Get string representation of this condition.
	 * 
	 * @return string representation of this condition
	 */
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		for (int variable : variableValueMap.keySet())
		{
			buffer.append("(");
			buffer.append(variable);
			buffer.append(":");
			int value = variableValueMap.get(variable);
			buffer.append(value);
			buffer.append(", name: ");

			String string = Global.problem.propositionNames.get(variable).get(
					value);
			buffer.append(string);
			buffer.append(")");

		}
		return buffer.toString();
	}

	/**
	 * Dump this explicit condition. Use for debugging.
	 */
	public void dump()
	{
		System.out.println(this);
	}

}