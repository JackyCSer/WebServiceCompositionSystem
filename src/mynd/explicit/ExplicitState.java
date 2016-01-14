package mynd.explicit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mynd.Global;
import mynd.heuristic.pdb.Abstraction;
import mynd.state.Operator;
import mynd.state.State;

/**
 * A state is a function mapping state variables to values from their respective
 * domains. This explicit state is a helper class and is not used in the search.
 * 
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class ExplicitState extends State
{

	/**
	 * Debug mode for additional outputs.
	 */
	public final static boolean DEBUG = Global.EXPLICIT_STATE_DEBUG;

	/**
	 * Variable value mapping describing this state. Note: this map includes
	 * states <strong>both</strong> inherited from <br/>
	 * it's parent, and this node' s own state.
	 */
	public final Map<Integer, Integer> variableValueAssignment;

	/**
	 * Variable value mapping describing this state. Node: this map
	 * <strong>ONLY</strong> includes this node's own state.<br/>
	 * Cf: variableValueAssignment
	 * 
	 * @modifid April 23, 2015
	 * @author Jacky ZHANG
	 */
	public Map<Integer, Integer> ownVariableValueAssignment;

	/**
	 * Number of variables describing this state.
	 */
	public final int size;

	/**
	 * HashCode which is used in collections which use hashing.
	 */
	public final int hashCode;

	/**
	 * Axiom evaluator.
	 */
	private ExplicitAxiomEvaluator axiomEvaluator;

	/**
	 * Creates a new state from a given variable assignment.
	 * 
	 * @param values
	 *            variable assignment
	 */
	public ExplicitState(int[] values, ExplicitAxiomEvaluator axiomEvaluator)
	{
		this(axiomEvaluator.evaluate(valuesToVariableValueMap(values)), false,
				null, axiomEvaluator);
	}

	/**
	 * Creates a new state from given assignment.
	 * 
	 * @param variableValueAssignment
	 */
	public ExplicitState(Map<Integer, Integer> variableValueAssignment,
			ExplicitAxiomEvaluator axiomEvaluator)
	{
		this(axiomEvaluator.evaluate(variableValueAssignment), false, null,
				axiomEvaluator);
		assert variableValueAssignment.size() == Global.problem.numStateVars;
		// Initialize ownVariableValueAssignment:
		this.ownVariableValueAssignment = variableValueAssignment;
	}

	/**
	 * Creates a new abstracted state from given assignment and abstraction.
	 * 
	 * @param variableValueAssignment
	 *            assignment of variables to values
	 * @param isAbstractedState
	 *            denote if the state is abstracted by a pattern
	 * @param abstractedProblem
	 *            denotes the abstracted problem of an abstracted state
	 */
	public ExplicitState(Map<Integer, Integer> variableValueAssignment,
			Abstraction abstraction, ExplicitAxiomEvaluator axiomEvaluator)
	{
		this(axiomEvaluator.evaluate(variableValueAssignment), true,
				abstraction, axiomEvaluator);
		
	}

	private ExplicitState(Map<Integer, Integer> variableValueAssignment,
			boolean isAbstractedState, Abstraction abstraction,
			ExplicitAxiomEvaluator axiomEvaluator)
	{
		super(computeUniqueID(variableValueAssignment), isAbstractedState,
				abstraction);
		assert assertVariableOrder(variableValueAssignment);
		this.variableValueAssignment = variableValueAssignment;
		size = variableValueAssignment.size();
		hashCode = uniqueID.intValue();
		this.axiomEvaluator = axiomEvaluator;
		// Initialize ownVariableValueAssignment:
		this.ownVariableValueAssignment = variableValueAssignment;
		
		// It is important to assert that there is no overflow for abstract
		// states.
		assert (!isAbstractedState || (uniqueID.intValue() == hashCode));
	}

	/**
	 * Test if a given operator is applicable in this state. Operators without
	 * effects are omitted (i.e. not applicable), because they are not useful
	 * under full observability.
	 * 
	 * @param operator
	 * @return true iff given operator is applicable in this state and it is
	 *         causative.
	 */
	@Override
	public boolean isApplicable(Operator operator)
	{
		return operator.isCausative && operator.isEnabledIn(this);
	}
	

	/**
	 * Apply given operator to this state.
	 * 
	 * Modified here: 2015-4-23 23:27:41
	 * 
	 * @param op
	 *            operator to apply
	 * @return set of successor states reached by applying given operator
	 * 
	 * @author Lavender
	 * 
	 */
	@Override
	public Set<State> apply(Operator op)
	{
		assert (op.isCausative);

		if (DEBUG)
		{
			System.out.println("Apply Operator: ");
			op.dump();
		}

		ExplicitOperator explicitOp = (ExplicitOperator) op;
		Set<State> states = new HashSet<State>();
		for (Set<ExplicitEffect> choice : explicitOp
				.getNondeterministicEffect())
		{
			// Modified here: 2015-4-23 23:27:41
			// -----------------------------Modified----------------------------

			for (ExplicitEffect effect : choice)
			{
				if (variableValueAssignment.get(effect.variable) != effect.value)
				{
					ExplicitState newState = progressWithNOInheriting(effect);
					states.add(newState);
				}
			}

			// -------------------------End Modified-------------------------

		}
		return states;
	}
	

	// -----------------------------Modified-------------------------------
	/**
	 * Apply given operator to this state.
	 * 
	 * @param op
	 *            operator to apply
	 * @return set of successor states reached by applying given operator
	 */
	@Override
	public Set<State> applyForLAOStar(Operator op)
	{
		assert (op.isCausative);
		ExplicitOperator explicitOp = (ExplicitOperator) op;
		Set<State> states = new HashSet<State>();
		for (Set<ExplicitEffect> choice : explicitOp
				.getNondeterministicEffect())
		{
			ExplicitState newState = progressForLAOStar(choice);
			states.add(newState);
		}
		return states;
	}

	/**
	 * Apply a deterministic effect to this state.
	 * 
	 * @param effect
	 *            effect to apply in this state
	 * @return successor state obtained by applying the given effect to this
	 *         state.
	 */
	public ExplicitState progressForLAOStar(Set<ExplicitEffect> effect)
	{
		if (DEBUG)
		{
			// System.out.println("Apply a deterministic effect to this state.");
			System.out.println("Current state (Positive): "
					+ toStringWithPositivePropositionNames());
		}

		// 1. Make a copy of the current state:
		Map<Integer, Integer> succVariableValuePairs = new LinkedHashMap<Integer, Integer>(
				(int) Math.ceil(size / 0.75));
		succVariableValuePairs.putAll(variableValueAssignment);

		// 2. Apply effects
		for (ExplicitEffect eff : effect)
		{
			if (eff.isEnabledIn(variableValueAssignment))
			{
				assert (succVariableValuePairs.containsKey(eff.variable));

				if (DEBUG)
				{
					System.out.println("Apply Effect: " + eff
							+ " in this state");
					System.out.println();
				}

				succVariableValuePairs.put(eff.variable, eff.value);

			} else
			{
				assert false; // effect conditions are not supported.
			}
		}

		if (isAbstractedState)
		{
			assert (axiomEvaluator != null);
			return new ExplicitState(succVariableValuePairs, abstraction,
					axiomEvaluator);
		} else
		{
			assert (axiomEvaluator != null);
			return new ExplicitState(succVariableValuePairs, axiomEvaluator);
		}

	}

	// -----------------------------End Modified-------------------------------
	
	
	/**
	 * Apply a deterministic effect to this state, <br/>
	 * similar to progress(Set<ExplicitEffect> effect) method above. <br/>
	 * but <strong>NOT inherit</strong> parent's state <br/>
	 * so, this method will create a totally <strong>NEW</strong> state, <br/>
	 * which only includes one new state obtained by applying the given effect
	 * to this state.
	 * 
	 * @param effect
	 *            effect to apply in this state
	 * @return successor state obtained by applying the given effect to this
	 *         state.
	 * @author Jacky ZHANG
	 * @date 2015-4-22
	 */
	public ExplicitState progressWithNOInheriting(ExplicitEffect effect)
	{
		if (DEBUG)
		{
			System.out.println();
			System.out.println("progressWithNOInheriting()");

			System.out.println("Current state (Positive): "
					+ toStringWithPositivePropositionNames());
		}

		// 1. Make a copy of the current state:
		Map<Integer, Integer> succVariableValuePairs = new LinkedHashMap<Integer, Integer>(
				(int) Math.ceil(size / 0.75));
		succVariableValuePairs.putAll(variableValueAssignment);

		// Create empty map:
		Map<Integer, Integer> ownVariableValuePairs = new LinkedHashMap<Integer, Integer>(
				(int) Math.ceil(size / 0.75));
		// Initialize the newVariableValuePairs:
		ExplicitState initialExplicitState = (ExplicitState) Global.problem
				.getSingleInitialState();
		Map<Integer, Integer> initialVariableValuePairs = initialExplicitState.variableValueAssignment;
		ownVariableValuePairs.putAll(initialVariableValuePairs);

		if (DEBUG)
		{
			System.out.println("ownVariableValueAssignment = "
					+ toStringOwnVariableValueAssignment());
			System.out.println("variableValueAssignment = "
					+ toStringWithPositivePropositionNames());
		}

		// 2. Apply effects
		if (ownVariableValueAssignment == null)
		{
			if (DEBUG)
			{
				System.out.println("ownVariableValueAssignment == null");
			}

			ownVariableValueAssignment = variableValueAssignment;
		}

		// Using this.ownVariableValueAssignment to create new successor
		// state:
		if (effect.isEnabledIn(ownVariableValueAssignment))
		{
			assert (succVariableValuePairs.containsKey(effect.variable));

			// Test if (eff.variable, eff.value) included in:
			// succVariableValuePairs
			// if included: NOT add into:
			// ownVariableValuePairs and succVariableValuePairs
			if (DEBUG)
			{
				System.out
						.println("Apply Effect: " + effect + " in this state");
				System.out.println();
			}

			// Add both:
			ownVariableValuePairs.put(effect.variable, effect.value);
			succVariableValuePairs.put(effect.variable, effect.value);
		} else
		{
			assert false; // effect conditions are not supported.
		}

		// 3. Create a new state:
		if (isAbstractedState)
		{
			assert (axiomEvaluator != null);

			ExplicitState newState = new ExplicitState(succVariableValuePairs,
					abstraction, axiomEvaluator);
			newState.ownVariableValueAssignment = ownVariableValuePairs;

			return newState;
		} else
		{
			assert (axiomEvaluator != null);

			ExplicitState newState = new ExplicitState(succVariableValuePairs,
					axiomEvaluator);
			newState.ownVariableValueAssignment = ownVariableValuePairs;

			return newState;
		}

	}

	/**
	 * Create a variable value assignment from given values.
	 * 
	 * @param values
	 * @return variable value assignment
	 */
	private static Map<Integer, Integer> valuesToVariableValueMap(int[] values)
	{
		// Note: LinkedHashMap has a fix iteration order which is important for
		// unique hash values.
		Map<Integer, Integer> variableValuePairs = new LinkedHashMap<Integer, Integer>(
				(int) Math.ceil(values.length / 0.75));
		for (int var = 0; var < values.length; var++)
		{
			variableValuePairs.put(var, values[var]);
		}
		return variableValuePairs;
	}

	/**
	 * Compute unique BigInteger value for this state.
	 */
	private static BigInteger computeUniqueID(
			Map<Integer, Integer> variableValueMap)
	{
		BigInteger uniqueID = BigInteger.ZERO;
		for (int var = 0; var < Global.problem.numStateVars; var++)
		{
			if (variableValueMap.containsKey(var))
			{
				uniqueID = uniqueID.multiply(BigInteger
						.valueOf(Global.problem.domainSizes.get(var)));
				uniqueID = uniqueID.add(BigInteger.valueOf(variableValueMap
						.get(var)));
			}
		}
		return uniqueID;
	}

	/**
	 * Abstract this state to the pattern of the given abstraction.
	 * 
	 * @param abstraction
	 */
	public State abstractToPattern(Abstraction abstraction)
	{
		return new ExplicitState(Abstraction.abstractVariableValueMap(
				variableValueAssignment, abstraction.pattern), abstraction,
				abstraction.getExplicitAxiomEvaluator());
	}

	/**
	 * Get all explicit world states of this explicit state which is exactly
	 * one, namely itself.
	 * 
	 * @return list containing this explicit state
	 */
	@Override
	public List<ExplicitState> getAllExplicitWorldStates()
	{
		return new ArrayList<ExplicitState>(Arrays.asList(this));
	}

	/**
	 * Check equality for this object and given object.
	 * 
	 * @param o
	 *            object to compare
	 * @return true iff both objects are explicit states and variable value
	 *         assignments are equal.
	 */
	public boolean equals(Object o)
	{
		if (!(o instanceof ExplicitState))
		{
			return false;
		}
		ExplicitState s = (ExplicitState) o;
		if (size != s.size)
		{
			return false;
		}
		for (int var : variableValueAssignment.keySet())
		{
			if (!s.variableValueAssignment.containsKey(var))
			{
				return false;
			}
			if (!variableValueAssignment.get(var).equals(
					s.variableValueAssignment.get(var)))
				return false;
		}
		return true;
	}

	/**
	 * HashCode which is not unique.
	 */
	@Override
	public int hashCode()
	{
		return hashCode;
	}

	/**
	 * Asserts that variables are sorted in ascending order.
	 * 
	 * @param variableValueAssignment
	 * @return true iff variables are sorted in ascending order
	 */
	public static boolean assertVariableOrder(
			Map<Integer, Integer> variableValueAssignment)
	{
		return assertVariableOrder(variableValueAssignment.keySet());
	}

	/**
	 * Asserts that variables are sorted in ascending order.
	 * 
	 * @param variables
	 *            set of variables
	 * @return true iff variables are sorted in ascending order
	 */
	public static boolean assertVariableOrder(Set<Integer> variables)
	{
		int oldVar = -1;
		for (int var : variables)
		{
			if (var <= oldVar)
				return false;
			oldVar = var;
		}
		return true;
	}

	/**
	 * String representation of this state with proposition names.
	 * 
	 * @return string representation of this state
	 */
	public String toStringWithPropositionNames()
	{
		List<List<String>> propositionNames = Global.problem.propositionNames;
		StringBuffer buffer = new StringBuffer();
		buffer.append("{ ");
		int i = 0;
		for (int var : variableValueAssignment.keySet())
		{
			buffer.append(propositionNames.get(var).get(
					variableValueAssignment.get(var)));
			if (i < variableValueAssignment.size() - 1)
			{
				buffer.append(", ");
			}
			i++;
		}
		buffer.append(" }");
		return buffer.toString();
	}

	/**
	 * String representation of this state with proposition names.
	 * 
	 * @return string representation of this state
	 */
	public String toStringWithPositivePropositionNames()
	{
		List<List<String>> propositionNames = Global.problem.propositionNames;
		StringBuffer buffer = new StringBuffer();
		buffer.append("{ ");
		int i = 0;
		for (int var : variableValueAssignment.keySet())
		{
			String string = propositionNames.get(var).get(
					variableValueAssignment.get(var));
			if (!string.startsWith("(not"))
			{
				if (i != 0)
				{
					buffer.append(", ");
				}
				buffer.append(string);
				i++;
			}

		}
		buffer.append(" }");
		return buffer.toString();
	}

	/**
	 * String representation of this state with proposition names.
	 * 
	 * @return string representation of this state
	 */
	public String toStringOwnVariableValueAssignment()
	{
		if (ownVariableValueAssignment == null)
		{
			return null;
		}

		List<List<String>> propositionNames = Global.problem.propositionNames;
		StringBuffer buffer = new StringBuffer();
		buffer.append("{ ");
		int i = 0;
		for (int var : ownVariableValueAssignment.keySet())
		{
			String string = propositionNames.get(var).get(
					ownVariableValueAssignment.get(var));
			if (!string.startsWith("(not"))
			{
				if (i != 0)
				{
					buffer.append(", ");
				}
				buffer.append(string);
				i++;
			}

		}
		buffer.append(" }");
		return buffer.toString();
	}

	/**
	 * String representation of this state.
	 * 
	 * @return string representation of this state
	 */
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append("ExplicitState: [");
		for (int var : variableValueAssignment.keySet())
		{
			buffer.append(var + ":" + variableValueAssignment.get(var) + ", ");
		}
		buffer.delete(buffer.length() - 2, buffer.length());
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Dump this explicit state.
	 */
	@Override
	public void dump()
	{
		if (DEBUG)
		{
			System.out.print("{");
			int c = 0;
			for (int var : variableValueAssignment.keySet())
			{
				System.out.print(Global.problem.propositionNames.get(var).get(
						variableValueAssignment.get(var)));
				if (c++ < variableValueAssignment.size() - 1)
				{
					System.out.print(", ");
				}
			}
			System.out.println("}");
		} else
		{
			System.out.println(variableValueAssignment);
		}
	}
}
