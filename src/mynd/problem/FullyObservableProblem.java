package mynd.problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import mynd.Global;
import mynd.MyNDPlanner;
import mynd.Preprocessor;
import mynd.MyNDPlanner.Algorithm;
import mynd.explicit.ExplicitAxiomEvaluator;
import mynd.explicit.ExplicitCondition;
import mynd.explicit.ExplicitEffect;
import mynd.explicit.ExplicitOperator;
import mynd.explicit.ExplicitState;
import mynd.explicit.ExplicitOperator.OperatorRule;
import mynd.heuristic.pdb.Abstraction;
import mynd.heuristic.pdb.PatternCollectionSearch;
import mynd.state.Condition;
import mynd.state.Operator;
import mynd.state.State;
import mynd.util.Pair;

/**
 * A fully observable problem consists of a set of multi-valued state variables,
 * an initial state, a goal condition, and a set of operators. There is no
 * uncertainty compared to partially observable planning problems. Therefore no
 * sensing operators are needed.
 * 
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class FullyObservableProblem extends Problem
{

	/**
	 * Explicit initial state.
	 */
	private ExplicitState initialState;

	/**
	 * Explicit axiom evaluator.
	 */
	public final ExplicitAxiomEvaluator explicitAxiomEvaluator;

	/**
	 * Set DEBUG to true for more console outputs.
	 */
	public static final boolean DEBUG = Global.PROBLEM_DEBUG;

	/**
	 * Constructor to create a fully observable planning problem.
	 */
	private FullyObservableProblem(int[] initialValuation,
			ExplicitCondition goal, ArrayList<String> variableNames,
			List<List<String>> propositionNames,
			ArrayList<Integer> domainSizes, ArrayList<Integer> axiomLayer,
			ArrayList<Integer> defaultAxiomValues,
			LinkedHashSet<Operator> causativeOperators, Set<OperatorRule> axioms)
	{
		super(goal, variableNames, propositionNames, domainSizes, axiomLayer,
				defaultAxiomValues, causativeOperators, axioms, true);
		Global.problem = this;
		performSanityCheck();
		List<Integer> defaultValues = new ArrayList<Integer>();
		for (int i = 0; i < initialValuation.length; i++)
		{
			defaultValues.add(i, initialValuation[i]);
		}
		explicitAxiomEvaluator = new ExplicitAxiomEvaluator();
		initialState = new ExplicitState(initialValuation,
				explicitAxiomEvaluator);
	}

	/**
	 * Creates a new fully observable problem.
	 * 
	 * @param goal
	 *            goal of this planning problem
	 * @param variableNames
	 *            variable names of this planning problem
	 * @param propositionNames
	 *            proposition names of this planning problem
	 * @param domainSizes
	 *            domain sizes of multi-valued variables
	 * @param numStateVars
	 *            number of multi-valued state variables
	 * @param operators
	 *            actions of this planning problem
	 * @return fully observable planning problem
	 */
	public static Problem getInstance(int[] initialValuation,
			ExplicitCondition goal, ArrayList<String> variableNames,
			List<List<String>> propositionNames,
			ArrayList<Integer> domainSizes, ArrayList<Integer> axiomLayer,
			ArrayList<Integer> defaultAxiomValues,
			LinkedHashSet<Operator> operators, Set<OperatorRule> axioms)
	{
		if (instance == null)
		{
			instance = new FullyObservableProblem(initialValuation, goal,
					variableNames, propositionNames, domainSizes, axiomLayer,
					defaultAxiomValues, operators, axioms);
		}
		return instance;
	}

	/**
	 * Sanity check: Make sure that all variables have domain sizes of at least
	 * one.
	 */
	private void checkDomainSizes()
	{
		for (int var = 0; var < domainSizes.size(); var++)
		{
			if (domainSizes.get(var) < 1)
			{
				System.err.println("Variable " + var + " has a domain size of "
						+ domainSizes.get(var) + ".");
				System.exit(1);
			}
		}
	}

	/**
	 * Sanity check: Test for equal state sizes
	 */
	private void checkStateSizes()
	{
		if (variableNames.size() != domainSizes.size())
		{
			if (DEBUG)
			{
				System.out.println("Variable names length = "
						+ variableNames.size());
				System.out
						.println("Domain size length = " + domainSizes.size());
			}
			System.err.println("Numbers of state variables in inputs differ.");
			System.exit(1);
		}

	}

	/**
	 * Perform a sanity check on the inputs.
	 */
	private void performSanityCheck()
	{
		if (variableNames != null && domainSizes != null
				&& propositionNames != null)
		{
			checkStateSizes();
			checkDomainSizes();
		}
	}

	/**
	 * Get explicit initial state. Special case: there is only one explicit
	 * initial state in a fully observable planning problem.
	 * 
	 * @return set which contains the single initial state of this planning task
	 */
	public List<ExplicitState> getExplicitInitialStates()
	{
		if (explicitInitialStateList == null)
			explicitInitialStateList = Collections
					.unmodifiableList(new ArrayList<ExplicitState>(Arrays
							.asList(initialState)));
		return explicitInitialStateList;
	}

	/**
	 * Abstract fully observable problem to given pattern.
	 * 
	 * @param pattern
	 *            the set of state variables to which this problem is abstracted
	 * @return abstraction induced by given pattern
	 */
	public Abstraction abstractToPattern(Set<Integer> pattern)
	{
		assert (initialState != null);

		// Abstract goal condition.
		Condition abstractGoal = explicitGoal.abstractToPattern(pattern);

		// Abstract operators.
		Set<Operator> abstractedOperators = new LinkedHashSet<Operator>(
				getOperators().size());
		for (Operator op : getOperators())
		{
			Operator absOp = op.abstractToPattern(pattern);
			if (absOp != null)
			{
				assert absOp.isAbstracted;
				abstractedOperators.add(absOp);
			}
		}
		assert Operator.assertNoDuplicateInNames(abstractedOperators);
		if (DEBUG)
		{
			System.out.println("Abstracted ops:");
			for (Operator op : abstractedOperators)
			{
				((ExplicitOperator) op).dump();
			}
		}

		// An axiom is either completely conserved or completely removed in an
		// abstraction.
		Set<OperatorRule> abstractedAxioms = new LinkedHashSet<ExplicitOperator.OperatorRule>(
				axioms.size());
		for (OperatorRule axiom : axioms)
		{
			if (pattern.contains(axiom.head.first))
			{
				for (Pair<Integer, Integer> fact : axiom.body)
				{
					assert pattern.contains(fact.first);
				}
				abstractedAxioms.add(axiom);
			}
		}

		// Abstract and set initial state.
		SortedSet<Integer> sortedPattern = new TreeSet<Integer>(pattern);
		Abstraction abstraction = new Abstraction(sortedPattern, abstractGoal,
				abstractedOperators, abstractedAxioms);
		abstraction.setInitialState(Arrays.asList(initialState
				.abstractToPattern(abstraction)));
		return abstraction;
	}

	/**
	 * Get string representation of this fully observable problem.
	 * 
	 * @return string representation of this fully observable planning problem
	 */
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("Init:    " + initialState.toString() + "\n");
		buffer.append("Goal:    " + explicitGoal.toString() + "\n");
		buffer.append("Vars:    ");
		for (int var = 0; var < variableNames.size(); var++)
		{
			buffer.append(var + ":" + domainSizes.get(var) + " ");
		}
		buffer.append("\n");
		buffer.append("Num ops: " + getOperators().size() + "\n");
		return buffer.toString();
	}

	/**
	 * Return the single initial state of this planning task.
	 * 
	 * @return the initial state
	 */
	@Override
	public State getSingleInitialState()
	{
		assert (initialState != null);
		return initialState;
	}

	/**
	 * Preprocess operators.
	 */
	@Override
	public void finishInitializationAndPreprocessing()
	{
		// Store operators as original operators before their preprocessing.
		setOriginalOperators(operators);
		LinkedHashSet<Operator> ops;
		if (Global.algorithm == Algorithm.LAO_STAR)
		{
			ops = Preprocessor
					.preprocessForStrongCyclicPlanning(getOperators());
		} else
		{
			// assert (Global.algorithm == Algorithm.AO_STAR);
			ops = Preprocessor.preprocessForStrongPlanning(getOperators());
		}
		operators = ops;
		PatternCollectionSearch.fullObservablePatternSearch = false; // TODO:
		// Could be mistakable.
		// This variable is only relevant for POND planning and should be false
		// in
		// FOND planning, although
		// in FOND planning the pattern collection search is always in a full
		// observable search space.
		MyNDPlanner.assumeFullObservability = false; // TODO same here...
	}

	/**
	 * Get explicit goal of this planning task.
	 * 
	 * @return goal
	 */
	@Override
	public Condition getGoal()
	{
		return explicitGoal;
	}

	/**
	 * Computes the determinization of given set of operators. After
	 * determinization each operator contains only deterministic effects.
	 * 
	 * @param ops
	 *            set of explicit operators
	 * @return determinization of given set of operators
	 */
	@Override
	public Set<Operator> determinization(Set<Operator> ops)
	{
		if (DEBUG)
			System.out.println("Determinize following operators: " + ops);
		Set<Operator> result = new HashSet<Operator>(ops.size() * 2);
		// Processing of explicit Operators.
		for (Operator op : ops)
		{
			ExplicitOperator explicitOp = (ExplicitOperator) op;
			assert !op.isDeterminized();
			Set<Set<ExplicitEffect>> nondeterministicEffect = explicitOp
					.getNondeterministicEffect();
			if (nondeterministicEffect == null
					|| nondeterministicEffect.size() == 1)
			{
				// Operator is already deterministic, since it has only an empty
				// effect or only a single deterministic effect.
				result.add(new ExplicitOperator(op.getName() + "_det",
						explicitOp.precondition, explicitOp
								.getNondeterministicEffect(), op.observation,
						op.isAbstracted, true, op.getCost()));
			} else
			{
				// There is more than one deterministic effect.
				int i = 0;
				if (DEBUG)
					System.out.println("Operator " + op + " has "
							+ nondeterministicEffect.size() + " choices.");
				for (Set<ExplicitEffect> choice : nondeterministicEffect)
				{
					if (choice.isEmpty())
						continue;
					Set<Set<ExplicitEffect>> modifiedNondeterministicEffect = new HashSet<Set<ExplicitEffect>>();
					modifiedNondeterministicEffect.add(choice);
					String name = op.getName() + "_det_" + i++;
					result.add(new ExplicitOperator(name,
							explicitOp.precondition,
							modifiedNondeterministicEffect, op.observation,
							op.isAbstracted, true, op.getCost()));
				}
			}
		}
		if (DEBUG)
		{
			System.out.println("Result of determinization: " + result);
			System.out.println();
		}
		assert (Operator.assertNoDuplicateInNames(result));
		return result;
	}

	@Override
	public ExplicitAxiomEvaluator getExplicitAxiomEvaluator()
	{
		return explicitAxiomEvaluator;
	}

	@Override
	public void setInitialState(State initState)
	{
		initialState = (ExplicitState) initState;
	}

	@Override
	protected void setOriginalOperators(Set<Operator> ops)
	{
		// FIXME duplicate code (since of op.copy()).
		// See FullyObservable Problem. Constructor not possible since symbolic
		// operators
		// are build with BDDManager.
		Map<String, Operator> original = new HashMap<String, Operator>(
				(int) (operators.size() * 0.75) + 1);
		int index = 0; // Each operator should get a unique name. Append a index
						// if it has a duplicate name.
		for (Operator op : operators)
		{
			if (original.containsKey(op.getName()))
			{
				original.put(op.getName() + index, op.copy());
				op.setName(op.getName() + index);
				++index;
			} else
			{
				original.put(op.getName(), op.copy());
			}
		}
		originalOperators = Collections.unmodifiableMap(original);
	}
}
