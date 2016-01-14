package mynd.problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import luna.web.model.LunaPlannerOutput;
import mynd.Global;
import mynd.explicit.ExplicitAxiomEvaluator;
import mynd.explicit.ExplicitCondition;
import mynd.explicit.ExplicitState;
import mynd.explicit.ExplicitOperator.OperatorRule;
import mynd.heuristic.pdb.Abstraction;
import mynd.state.Condition;
import mynd.state.Operator;
import mynd.state.State;

/**
 * 
 * @author Manuela Ortlieb
 * 
 */
public abstract class Problem
{
	/**
	 * Planner output
	 */
	public LunaPlannerOutput lunaPlannerOutput = new LunaPlannerOutput();

	/**
	 * Singleton problem instance.
	 */
	protected static Problem instance;

	/**
	 * Explicit goal of this planning problem. Note: Explicit representation is
	 * also used in a partially observable problem for pattern selection search.
	 */
	public final ExplicitCondition explicitGoal;

	/**
	 * Mapping from variables to their names.
	 */
	public final List<String> variableNames;

	/**
	 * List of proposition names corresponding to variable-value pairs.
	 * propositionNames.get(i).get(j) is the name of the proposition
	 * corresponding to the equality (var_i = j).
	 */
	public final List<List<String>> propositionNames;

	/**
	 * Mapping from variables to their domain sizes, i.e., the numbers of
	 * possible values the variables can assume.
	 */
	public final List<Integer> domainSizes;

	/**
	 * Number of state variables.
	 */
	public final int numStateVars;

	/**
	 * Number of axioms.
	 */
	public final int numAxioms;

	/**
	 * Operators of this planning task.
	 */
	protected Set<Operator> operators;

	/**
	 * Original given operators of the planning task.
	 */
	protected Map<String, Operator> originalOperators;

	/**
	 * Indicates if problem is full observable or not.
	 */
	public final boolean isFullObservable;

	/**
	 * Explicit initial state(s) of the planning task. Note: Not final since
	 * explicit initial states are only computed if they are really needed. This
	 * is because this computation is expensive for a partially observable
	 * problem where belief state BDDs are used.
	 */
	protected List<ExplicitState> explicitInitialStateList;

	/**
	 * Axioms of this planning problem.
	 */
	public final Set<OperatorRule> axioms;

	/**
	 * Axiom layer of each variable.
	 */
	public final List<Integer> axiomLayer;

	/**
	 * Default values of all variables in current evaluated state.
	 */
	public final List<Integer> defaultAxiomValues;

	public Problem(ExplicitCondition explicitGoal, List<String> variableNames,
			List<List<String>> propositionNames,
			ArrayList<Integer> domainSizes, ArrayList<Integer> axiomLayer,
			ArrayList<Integer> defaultAxiomValues,
			LinkedHashSet<Operator> operators, Set<OperatorRule> axioms,
			boolean isFullObservable)
	{
		this.explicitGoal = explicitGoal;
		this.variableNames = Collections.unmodifiableList(variableNames);
		this.propositionNames = Collections.unmodifiableList(propositionNames);
		this.domainSizes = Collections.unmodifiableList(domainSizes);
		this.numStateVars = domainSizes.size();
		this.axiomLayer = Collections.unmodifiableList(axiomLayer);
		this.axioms = Collections.unmodifiableSet(axioms);
		this.numAxioms = axioms.size();
		this.defaultAxiomValues = Collections
				.unmodifiableList(defaultAxiomValues);
		this.isFullObservable = isFullObservable;
		this.operators = operators;
	}

	/**
	 * Get set of explicit initial states of this planning task.
	 * 
	 * @return set of explicit initial states
	 */
	public abstract List<ExplicitState> getExplicitInitialStates();

	/**
	 * Get the single initial state of this planning task.
	 * 
	 * @return the initial state of this planning task
	 */
	public abstract State getSingleInitialState();

	/**
	 * Create an abstraction of this problem induced by given pattern.
	 * 
	 * @param pattern
	 *            the set of state variables to which this problem is abstracted
	 * @return abstraction induced by given pattern
	 */
	public abstract Abstraction abstractToPattern(Set<Integer> pattern);

	/**
	 * Get goal of this planning task.
	 * 
	 * @return goal
	 */
	public abstract Condition getGoal();

	/**
	 * Finish initialization of components and preprocessing.
	 */
	public abstract void finishInitializationAndPreprocessing();

	/**
	 * Dumping method for debugging reasons.
	 */
	public void dump()
	{
		System.err.println("mynd.problem.Problem.dump()");
		System.err.println("There are " + numStateVars + " state variables.");
		System.err.println("Domain sizes:");
		for (int i = 0; i < variableNames.size(); i++)
		{
			System.err.println(variableNames.get(i).toString() + ":"
					+ domainSizes.get(i));
		}

		System.err.println();
		System.err.println("Variable Name Table:");
		for (int i = 0; i < variableNames.size(); i++)
		{
			String string = Global.problem.propositionNames.get(i).get(0);

			System.err.println(variableNames.get(i).toString() + " = 0, name: "
					+ string);
		}

		System.err.println();
		System.err.println("Explicit initial state(s):");
		for (State state : getExplicitInitialStates())
		{
			if (state instanceof ExplicitState)
			{
				System.err.println(((ExplicitState) state)
						.toStringWithPropositionNames());
			} else
			{
				System.err.println(state);
			}

		}

		System.err.println();
		System.err.println("Explicit goal state(s):");
		Condition goal = getGoal();
		System.out.println(goal);

		System.err.println();
		System.err.println("Actions:");
		for (Operator op : operators)
		{
			op.dump();
		}
	}

	/**
	 * Test whether this problem is propositional in the sense that all
	 * variables occurring in it are Boolean.
	 * 
	 * @return true iff. all variables in this problem have a domain size of at
	 *         most two
	 */
	public boolean isPropositional()
	{
		for (int i = 0; i < numStateVars; i++)
		{
			if (domainSizes.get(i) > 2)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the planning problem's operators.
	 * 
	 * @return operators of the planning task.
	 */
	public Set<Operator> getOperators()
	{
		return operators;
	}

	/**
	 * Get a determinization of the given set of operators. A nondeterministic
	 * operator with n nontrivial outcomes is splitted in n deterministic
	 * operators.
	 * 
	 * @param ops
	 *            operators to determinize.
	 * @return determinization of the given set of operators.
	 */
	public abstract Set<Operator> determinization(Set<Operator> ops);

	/**
	 * Get the explicit axiom evaluator.
	 * 
	 * @return explicit axiom evaluator.
	 */
	public abstract ExplicitAxiomEvaluator getExplicitAxiomEvaluator();

	/**
	 * Destroy this problem. Only reasonable when running this planner multiple
	 * times (such that more than one planning problem is solved).
	 */
	public void destroy()
	{
		instance = null;
		Global.problem = null;
	}

	/**
	 * Change the operators of this planning problem.
	 */
	public void setOperators(Set<Operator> ops, boolean updateOriginalOperators)
	{
		operators = ops;
		if (updateOriginalOperators)
		{
			setOriginalOperators(operators); // TODO: explanation
		}
	}

	/**
	 * Set initial state of this planning problem.
	 * 
	 * @param initState
	 */
	public abstract void setInitialState(State initState);

	/**
	 * Get the original operators of planning task.
	 * 
	 * @return original operator map
	 */
	public Map<String, Operator> getOriginalOperatorMap()
	{
		return originalOperators;
	}

	public static Problem getInstance()
	{
		return instance;
	}

	public static void setInstance(Problem instance)
	{
		Problem.instance = instance;
	}

	protected abstract void setOriginalOperators(Set<Operator> ops);

}
