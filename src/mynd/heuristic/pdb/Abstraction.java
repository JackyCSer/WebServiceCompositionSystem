package mynd.heuristic.pdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import mynd.Global;
import mynd.explicit.ExplicitAxiomEvaluator;
import mynd.explicit.ExplicitOperator.OperatorRule;
import mynd.state.Condition;
import mynd.state.Operator;
import mynd.state.State;
import mynd.symbolic.SymbolicAxiomEvaluator;
import net.sf.javabdd.BDD;

/**
 * 
 * @author Manuela Ortlieb
 * 
 */
public class Abstraction
{

	/**
	 * Pattern which induced this abstraction.
	 */
	public final Set<Integer> pattern; // TODO should not be public

	/**
	 * Abstracted explicit state(s).
	 */
	private final List<State> initialState;

	/**
	 * Abstracted goal.
	 */
	public final Condition goal;

	/**
	 * Abstracted operators.
	 */
	public final Set<Operator> operators;

	/**
	 * Symbolic pattern complement.
	 */
	public final BDD symbolicPatternComplement;

	/**
	 * Explicit axiom evaluator.
	 */
	private ExplicitAxiomEvaluator explicitAxiomEvaluator;

	/**
	 * Symbolic axiom evaluator.
	 */
	private SymbolicAxiomEvaluator symbolicAxiomEvaluator;

	/**
	 * Abstracted axioms.
	 */
	public final Set<OperatorRule> axioms;

	public Abstraction(SortedSet<Integer> pattern, Condition goal,
			Set<Operator> operators, Set<OperatorRule> axioms)
	{
		this(pattern, goal, operators, axioms, null);
	}

	public Abstraction(SortedSet<Integer> pattern, Condition goal,
			Set<Operator> operators, Set<OperatorRule> axioms,
			BDD symbolicPatternComplement)
	{
		this.pattern = pattern;
		this.initialState = new ArrayList<State>();
		this.goal = goal;
		this.operators = Collections.unmodifiableSet(operators);
		this.axioms = Collections.unmodifiableSet(axioms);
		this.symbolicPatternComplement = symbolicPatternComplement;
	}

	private void initializeExplicitAxiomEvaluator()
	{
		assert explicitAxiomEvaluator == null;
		explicitAxiomEvaluator = new ExplicitAxiomEvaluator(axioms);
		// return explicitAxiomEvaluator;
	}

	private void initializeSymbolicAxiomEvaluator()
	{
		assert symbolicAxiomEvaluator == null;
		symbolicAxiomEvaluator = new SymbolicAxiomEvaluator(axioms);
	}

	public void setInitialState(List<State> initialState)
	{
		assert this.initialState.isEmpty();
		for (State state : initialState)
			this.initialState.add(state);
	}

	public List<State> getInitialState()
	{
		assert initialState != null;
		return Collections.unmodifiableList(initialState);
	}

	/**
	 * Abstract a given mapping to a given pattern.
	 * 
	 * @param variableValueMap
	 *            mapping from variables to values
	 * @param pattern
	 *            set of variables
	 * @return abstracted mapping
	 */
	public static Map<Integer, Integer> abstractVariableValueMap(
			Map<Integer, Integer> variableValueMap, Set<Integer> pattern)
	{
		Map<Integer, Integer> abstractedVariableValueMap = new LinkedHashMap<Integer, Integer>(
				variableValueMap.size());
		for (int var : variableValueMap.keySet())
		{
			if (pattern.contains(var))
				abstractedVariableValueMap.put(var, variableValueMap.get(var));
		}
		return abstractedVariableValueMap;
	}

	/**
	 * Get complement of given pattern.
	 * 
	 * @param pattern
	 *            set of state variables
	 * @return complement set of state variables
	 */
	public static Set<Integer> getPatternComplement(Set<Integer> pattern)
	{
		Set<Integer> patternComplement = new HashSet<Integer>(
				(int) (Global.problem.numStateVars - pattern.size() * 0.75) + 1);
		for (int var = 0; var < Global.problem.numStateVars; var++)
		{
			if (!pattern.contains(var))
				patternComplement.add(var);
		}
		return patternComplement;
	}

	public SymbolicAxiomEvaluator getSymbolicAxiomEvaluator()
	{
		if (symbolicAxiomEvaluator == null)
		{
			initializeSymbolicAxiomEvaluator();
		}
		return symbolicAxiomEvaluator;
	}

	public ExplicitAxiomEvaluator getExplicitAxiomEvaluator()
	{
		if (explicitAxiomEvaluator == null)
		{
			initializeExplicitAxiomEvaluator();
		}
		return explicitAxiomEvaluator;
	}

	/**
	 * Dump this abstraction. Only for debugging.
	 */
	public void dump()
	{
		System.out.println("Dumping abstraction induced by pattern:");
		System.out.println(pattern);
		System.out.println("Abstracted initial state:");
		for (State state : initialState)
			state.dump();
		System.out.println("Abstracted goal: ");
		goal.dump();
		System.out.println("Abstracted operators:");
		for (Operator op : operators)
			op.dump();
	}
}
