package mynd.heuristic.pdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import mynd.Global;
import mynd.MyNDPlanner;
import mynd.explicit.ExplicitEffect;
import mynd.explicit.ExplicitOperator;
import mynd.explicit.ExplicitOperator.OperatorRule;
import mynd.state.Operator;
import mynd.state.State;
import mynd.symbolic.PartiallyObservableProblem;
import mynd.util.Pair;

/**
 * This class does a hill climbing search to find a pattern selection. 1.
 * Initial Collection: {{v} | v is a state variable mentioned in the goal } 2.
 * Termination: as soon as the current pattern collection has no successors of
 * better quality or there are no successors at all because further growth of
 * the the current collection would exceed a memory limit 3. Neighborhood: the
 * neighbors of the collection C are all pattern collections C union {P'} where
 * - P' = P union {v} for some P in C - P' not in C - all variables of P' are
 * causally relevant in P' - P' is causally connected and - all pattern
 * databases C union {P'} can be represented within some prespecified space
 * limit
 * 
 * @author Manuela Ortlieb
 * @author Robert Mattmueller
 */

public class PatternCollectionSearch
{

	/**
	 * Maximal overall abstract states over all PDBs.
	 */
	public static int pdbsOverallMaxSize = -1;

	/**
	 * Only if *minImprovement* of the samples are improved, take this pattern
	 * if it is the best. Special case: meaning of 0.0 is that at least one
	 * sample has to be improved. default: 0.0
	 */
	public static double minImprovement = 0.1;

	/**
	 * If *greedyImprovement* of the samples are improved, take this pattern
	 * immediately. default: 1.0
	 */
	public static double greedyImprovement = 1.0;

	/**
	 * Timeout (in ms) for the pattern selection procedure.
	 */
	public static int timeout = Integer.MAX_VALUE;

	/**
	 * Number of iterations of the hill climbing algorithm.
	 */
	public static int steps = Integer.MAX_VALUE;

	/**
	 * Temporary pattern database during pattern search are stored.
	 */
	public static boolean cacheTemporaryPDBs = true;

	/**
	 * Start time of the pattern selection procedure to calculate the overall
	 * time of the pattern selection.
	 */
	private long starttime;

	/**
	 * When pattern search is done under the assumption of full observability,
	 * belief states are only used during problem solving (search).
	 */
	public static boolean fullObservablePatternSearch = true;

	/**
	 * Under partial observability not every variable is allowed for a pattern.
	 * 1. variable is "good" if it is observable. 2. variable is "good" if it
	 * does not occur in a condition. 3. variable is "good" if it is does not
	 * become uncertain and its value is initially known. Variables which are
	 * not "good" are not allowed, because they can make the abstracted problem
	 * unsolvable, although the planning task is solvable.
	 */
	private Set<Integer> allowedVarsForPatterns;

	public final Map<Integer, Set<Integer>> derivedVarsDependencies;

	/**
	 * Set true for debug output information.
	 */
	public static boolean DEBUG = false;

	/**
	 * Initialize hill climbing search
	 * 
	 * @param problem
	 *            The underlying planning problem.
	 */
	public PatternCollectionSearch()
	{
		starttime = System.currentTimeMillis();
		// Check options.
		if (PDB.pdbMaxSize == -1)
		{
			// PDB max size not set.
			if (Global.problem.isFullObservable
					|| MyNDPlanner.assumeFullObservability)
			{
				PDB.pdbMaxSize = 50000;
			} else
			{
				PDB.pdbMaxSize = 10000;
			}
		}
		if (pdbsOverallMaxSize == -1)
		{
			// PDBs overall size not set.
			pdbsOverallMaxSize = 10 * PDB.pdbMaxSize;
		}
		if (steps == 0)
		{
			// No pattern search at all.
			fullObservablePatternSearch = false;
		}
		if (DEBUG)
		{
			System.out.println("pdbMaxSize = " + PDB.pdbMaxSize);
			System.out.println("pdbsOverallMaxSize = " + pdbsOverallMaxSize);
		}
		// Compute dependencies of derived variables.
		if (Global.problem.numAxioms > 0)
		{
			derivedVarsDependencies = Collections
					.unmodifiableMap(computeDependenciesOfDerivedVars());
		} else
		{
			derivedVarsDependencies = Collections.unmodifiableMap(Collections
					.<Integer, Set<Integer>> emptyMap());
		}
	}

	/**
	 * Returns all causally relevant variables to the given set of variables
	 * without the variables itself.
	 * 
	 * @param vars
	 * @return
	 */
	public static Set<Integer> causallyRelevantVariables(Set<Integer> vars)
	{
		// More than one step in the causal graph?
		// Actually yes, but maybe this way we get a better pruning and still do
		// not
		// lose too much.
		Set<Integer> result = new LinkedHashSet<Integer>();

		for (Operator op : Global.problem.getOperators())
		{
			Set<Integer> affectedVariables = op.getAffectedVariables();
			assert !affectedVariables.isEmpty(); // Useless operator.
			boolean affectsVars = false;
			for (int var : vars)
			{
				if (affectedVariables.contains(var))
				{
					affectsVars = true;
					break;
				}
			}
			if (affectsVars)
			{
				for (int precondVar : op.getExplicitOperator().precondition.variableValueMap
						.keySet())
				{
					if (!vars.contains(precondVar))
					{
						result.add(precondVar);
					}
				}
				for (int var : affectedVariables)
				{
					if (!vars.contains(var))
					{
						result.add(var);
					}
				}
			}
		}
		return result;
	}

	private Map<Integer, Set<Integer>> computeDependenciesOfDerivedVars()
	{
		Map<Integer, Set<Integer>> dependencies = new HashMap<Integer, Set<Integer>>(
				Global.problem.numStateVars);
		Map<Integer, Set<Integer>> directDependency = new HashMap<Integer, Set<Integer>>(
				Global.problem.numStateVars);
		for (OperatorRule axiom : Global.problem.axioms)
		{
			Set<Integer> vars;
			if (directDependency.containsKey(axiom.head.first))
			{
				vars = directDependency.get(axiom.head.first);
			} else
			{
				vars = new HashSet<Integer>();
			}
			for (Pair<Integer, Integer> fact : axiom.body)
			{
				vars.add(fact.first);
			}
			if (!vars.isEmpty())
			{
				directDependency.put(axiom.head.first, vars);
			}
		}
		for (int var : directDependency.keySet())
		{
			Set<Integer> varDependsOn = new HashSet<Integer>();
			Queue<Integer> queue = new LinkedList<Integer>();
			queue.add(var);
			while (!queue.isEmpty())
			{
				int v = queue.poll();
				if (v != var)
				{
					varDependsOn.add(v);
				}
				if (directDependency.containsKey(v))
				{
					for (int w : directDependency.get(v))
					{
						if (!queue.contains(w) && !varDependsOn.contains(w))
						{
							queue.add(w);
						}
					}
				}
			}
			dependencies.put(var, varDependsOn);
		}
		return dependencies;
	}

	private Set<Set<Integer>> computeAdditionalPatterns(
			CanonicalPDBHeuristic canonical, Set<Integer> pattern)
	{

		System.err.println("Computing additional patterns:");

		Set<Set<Integer>> result = new HashSet<Set<Integer>>();
		for (int var : causallyRelevantVariables(pattern))
		{
			if (!allowedVarsForPatterns.contains(var))
			{
				continue;
			}
			Set<Integer> newPattern = new LinkedHashSet<Integer>(); // keeps
																	// order of
																	// variables
			newPattern.addAll(pattern);
			// Add new causally relevant variable.
			newPattern.add(var);
			if (derivedVarsDependencies.containsKey(var))
			{
				// Add variables on which the new variable depends.
				newPattern.addAll(derivedVarsDependencies.get(var));
			}
			if (newPattern.equals(pattern)
					|| canonical.temporaryPDBs.containsKey(newPattern)
					|| canonical.patterntoPDB.containsKey(newPattern))
				continue;

			System.err.println("  Candidate for additional pattern: "
					+ newPattern);

			if ((projectedOverallMemoryUsageOK(canonical, newPattern)
					&& (!canonical.temporaryPDBs.containsKey(newPattern)) && PatternCollectionSearch.cacheTemporaryPDBs)
					|| (projectedOverallMemoryUsageOK(canonical, newPattern)
							&& !canonical.patterntoPDB.containsKey(newPattern) && !PatternCollectionSearch.cacheTemporaryPDBs))
			{
				result.add(newPattern);
				System.err.println("    ... accepted!");
			} else
			{
				if (!projectedOverallMemoryUsageOK(canonical, newPattern))
				{
					System.err.println("    ... rejected! Too large!");
				} else
				{
					System.err.println("    ... rejected! Already there!");
				}
			}
		}

		System.err.println("Done!");

		return result;
	}

	/**
	 * Create initial pattern collection which consists of singleton patterns
	 * each containing a goal variable.
	 * 
	 * @return initial pattern collection
	 */
	private Set<Set<Integer>> createInitialPatternCollection()
	{
		Set<Set<Integer>> result = new LinkedHashSet<Set<Integer>>(
				(int) Math.ceil(Global.problem.explicitGoal.size / 0.75));
		for (int var : Global.problem.explicitGoal.variableValueMap.keySet())
		{
			if (DEBUG)
			{
				System.out.println("goalvar " + var);
			}
			if (allowedVarsForPatterns.contains(var))
			{
				Set<Integer> pattern = createSingletonPattern(var);
				// Check for dependencies of derived variables.
				if (derivedVarsDependencies.containsKey(var))
				{
					pattern.addAll(derivedVarsDependencies.get(var));
				}
				if (projectedOverallMemoryUsageOK(null, pattern))
				{
					result.add(pattern);
				} else
				{
					System.err.println("Pattern " + pattern
							+ " rejected! Too large!");
					System.err
							.println("Warning! There is a (maybe derived?) goal variable");
					System.err
							.println("which can not be taken as pattern for initial pattern");
					System.err
							.println("collection! Try to increase pdbMaxSize/pdbsMaxSize.");
				}
			}
		}
		if (DEBUG)
		{
			System.out
					.println("Following goal variables are allowed as pattern variables:");
			System.out.println(result);
		}
		return result;
	}

	private Set<Integer> createSingletonPattern(int var)
	{
		Set<Integer> patternSet = new HashSet<Integer>();
		patternSet.add(var);
		return patternSet;
	}

	private int patternImprovesHeuristic(CanonicalPDBHeuristic canonical,
			Set<Integer> newPattern, Collection<State> samples)
	{
		Set<Set<Set<Integer>>> maxAdditiveSubsets = canonical.compatibilityGraph
				.getMaxAdditiveSubsets(newPattern);
		int numberOfImprovements = 0;
		for (State sample : samples)
		{
			if (improvement(canonical, newPattern, sample, maxAdditiveSubsets))
			{
				numberOfImprovements++;
			}

		}
		return numberOfImprovements;
	}

	private boolean improvement(CanonicalPDBHeuristic canonical,
			Set<Integer> newPattern, State sample,
			Set<Set<Set<Integer>>> maxAdditiveSubsets)
	{
		double oldCanonicalHeuristicValue = canonical
				.getCanonicalHeuristic(sample);
		double newPatternHeuristicValue;
		newPatternHeuristicValue = canonical.temporaryPDBs.get(newPattern)
				.getHeuristic(sample);
		if (oldCanonicalHeuristicValue == Double.POSITIVE_INFINITY)
		{
			return false;
		}
		if (newPatternHeuristicValue == Double.POSITIVE_INFINITY)
		{
			// It has to be ensured that the set of
			// candidates is free of duplicates! Elsewhere this "return"
			// won't be correct, if newPattern is
			// in the collection yet.
			return true;
		}
		for (Set<Set<Integer>> subset : maxAdditiveSubsets)
		{
			// for each maximal subset...
			double subsetHeuristicValue = 0;
			for (Set<Integer> pattern : subset)
			{
				double d = canonical.patterntoPDB.get(pattern).getHeuristic(
						sample);
				if (d == Double.POSITIVE_INFINITY)
				{
					System.err.println(pattern);
					System.err.println(canonical.patterntoPDB.get(pattern));
					System.err.println(sample);
				}
				assert (!(d == Double.POSITIVE_INFINITY));
				subsetHeuristicValue += d;
			}
			if ((newPatternHeuristicValue + subsetHeuristicValue) > oldCanonicalHeuristicValue
					+ 2 * AbstractCostComputation.EPSILON)
			{
				return true;
			}
		}
		return false;
	}

	private boolean projectedOverallMemoryUsageOK(
			CanonicalPDBHeuristic canonical, Set<Integer> newPattern)
	{
		// a) check if the new pattern alone is too large (this does not
		// immediately
		// have to do with memory usage and rather with trying to steer the
		// search
		// towards roughly equal sized patterns instead of one large dominating
		// pattern
		// plus a few relatively unimportant small patterns.
		int sizeOfNewPattern = PDB.numAbstractStates(newPattern);
		if (sizeOfNewPattern > PDB.pdbMaxSize)
		{
			return false;
		}

		// b) check if the sum of the sizes of the pattern databases represented
		// by the
		// current pattern collection together with the new pattern will not
		// exceed
		// a given threshold.
		int canonicalSize = 0;
		if (canonical != null)
			canonicalSize = canonical.size;
		if (canonicalSize + sizeOfNewPattern > pdbsOverallMaxSize)
		{
			return false;
		}

		return true;
	}

	public static Set<Integer> computeAllowedPatternVariablesForPOND()
	{
		Set<Integer> result = new HashSet<Integer>();
		assert !Global.problem.isFullObservable;
		int capacity = (int) (Global.problem.numStateVars / 0.75) + 1;
		// HashSet<Integer> observableVars = new HashSet<Integer>(capacity);
		HashMap<Integer, HashSet<Integer>> observableValues = new HashMap<Integer, HashSet<Integer>>(
				capacity);
		HashSet<Integer> conditionalVars = new HashSet<Integer>(capacity);
		HashSet<Integer> varsWhichCouldBecomeUncertain = new HashSet<Integer>(
				capacity);

		for (Operator op : Global.problem.getOperators())
		{
			// 1. variable is "good" if it is observable.
			// That means that at least k-1 values has to be observable, if
			// domain size is k.
			for (Pair<Integer, Integer> varVal : op.observation)
			{
				if (observableValues.containsKey(varVal.first))
				{
					observableValues.get(varVal.first).add(varVal.second);
				} else
				{
					HashSet<Integer> values = new HashSet<Integer>(
							(int) (Global.problem.domainSizes.get(varVal.first) / 0.75) + 1);
					values.add(varVal.second);
					observableValues.put(varVal.first, values);
				}
			}
			ExplicitOperator explicitOp = op.getExplicitOperator();
			// 3. variable is "good" if it is does not become uncertain and its
			// value is initially known.
			// Check all nondeterministic effects.
			if (explicitOp.isCausative
					&& explicitOp.getNondeterministicEffect().size() > 1)
			{
				// Start by creating a map from every affected var to its
				// precondition value or -1 if
				// it is not part of the precondition.
				HashMap<Integer, Integer> affectedVarsToPreconditionValue = new HashMap<Integer, Integer>();
				for (int var : explicitOp.getAffectedVariables())
				{
					if (explicitOp.precondition.variableValueMap
							.containsKey(var))
					{
						affectedVarsToPreconditionValue.put(var,
								explicitOp.precondition.variableValueMap
										.get(var));
					} else
					{
						affectedVarsToPreconditionValue.put(var, -1);
					}
				}

				HashMap<Integer, Integer> variableValuePairsOfAllChoices = new HashMap<Integer, Integer>();
				for (Set<ExplicitEffect> choice : explicitOp
						.getNondeterministicEffect())
				{
					HashMap<Integer, Integer> choiceVarVals = new HashMap<Integer, Integer>();
					for (ExplicitEffect eff : choice)
					{
						choiceVarVals.put(eff.variable, eff.value); // collect
																	// all
																	// variables
																	// and
																	// values of
																	// this
																	// choice.
					}

					// For each var (which is not yet in
					// varsWhichCouldBecomeUncertain), test if
					// value is the same as in other choices (of earlier
					// iterations).
					for (int var : affectedVarsToPreconditionValue.keySet())
						if (!varsWhichCouldBecomeUncertain.contains(var))
						{
							if (!variableValuePairsOfAllChoices
									.containsKey(var))
							{ // initialization
								if (choiceVarVals.containsKey(var))
								{
									variableValuePairsOfAllChoices.put(var,
											choiceVarVals.get(var));
								} else
								{
									variableValuePairsOfAllChoices.put(var,
											affectedVarsToPreconditionValue
													.get(var));
								}
							} else
							{ // Test for equality.
								if (choiceVarVals.containsKey(var))
								{
									if (!variableValuePairsOfAllChoices
											.get(var).equals(
													choiceVarVals.get(var)))
									{
										varsWhichCouldBecomeUncertain.add(var);
									}
								} else
								{
									if (!variableValuePairsOfAllChoices
											.get(var).equals(
													affectedVarsToPreconditionValue
															.get(var)))
									{
										varsWhichCouldBecomeUncertain.add(var);
									}
								}
							}
						}
				}
			}
			// 2. variable is "good" if it does not occur in a condition or the
			// goal.
			conditionalVars.addAll(explicitOp.precondition.variableValueMap
					.keySet());
		}
		for (int var = 0; var < Global.problem.numStateVars; ++var)
		{
			// 1. variable is "good" if it is observable.
			if (observableValues.containsKey(var)
					&& observableValues.get(var).size() >= Global.problem.domainSizes
							.get(var) - 1)
			{
				result.add(var);
			}
			// 2. variable is "good" if it is does not become uncertain and is
			// initially known.
			else if (!varsWhichCouldBecomeUncertain.contains(var)
					&& ((PartiallyObservableProblem) Global.problem).variablesWhichAreInitiallyKnown
							.contains(var))
			{
				result.add(var);
			}
			// 3. variable is "good" if it does not occur in a condition or the
			// goal.
			else if (!conditionalVars.contains(var))
			{
				result.add(var);
			}
		}
		return result;
	}

	/**
	 * Perform hill climbing search until there is no more improvement or the
	 * possible improvers are pruned away because of memory constraints.
	 * 
	 * @return The canonical PDB heuristic corresponding to the pattern
	 *         collection found by this search
	 */
	public CanonicalPDBHeuristic search()
	{
		if (!Global.problem.isFullObservable)
		{
			allowedVarsForPatterns = computeAllowedPatternVariablesForPOND();
			System.err.println();
			System.err
					.println("These variables are allowed as pattern variables.");
			System.err.println(allowedVarsForPatterns);
			System.err.println();
		} else
		{
			allowedVarsForPatterns = new HashSet<Integer>(
					(int) (Global.problem.numStateVars * 0.75) + 1);
			for (int var = 0; var < Global.problem.numStateVars; var++)
			{
				allowedVarsForPatterns.add(var);
			}
		}
		System.err.println("Starting search for pattern collection.");

		// Initialize pattern collection (all singleton patterns for goal
		// variables)
		Set<Set<Integer>> currentCollection = createInitialPatternCollection();
		assert !currentCollection.isEmpty();

		// Initialize corresponding canonical PDB heuristic
		CanonicalPDBHeuristic canonical = new CanonicalPDBHeuristic(
				currentCollection);
		if (DEBUG)
		{
			System.out.println("initial canonical pdb heuristic.");
			System.out.println(canonical);
		}

		if (steps == 0)
		{
			if (DEBUG)
				System.out
						.println("Using initial pattern collection for search.");
			if (CanonicalPDBHeuristic.determinization)
				Global.problem.getSingleInitialState().resetApplicableOps();
			return canonical; // pattern collection with goal variables as
								// patterns
		}

		// Initial pattern candidate collection
		Set<Set<Integer>> candidatePatterns = new HashSet<Set<Integer>>();

		for (Set<Integer> pattern : currentCollection)
			candidatePatterns.addAll(computeAdditionalPatterns(canonical,
					pattern));

		// Is there still progress in hillclimbing search?
		boolean betterSuccessor = true;

		// Best successor pattern
		Set<Integer> bestAdditionalPattern;

		// Number of samples for which best successor pattern improves
		// the heuristic value
		int bestNumberOfImprovements;

		// steepest ascent hillclimbing
		boolean outOfTime = System.currentTimeMillis() - starttime > timeout;

		int step = 0;
		while (betterSuccessor && !outOfTime && step < steps)
		{
			Collection<State> samples = new RandomWalk(canonical).getSamples();
			betterSuccessor = false;
			bestAdditionalPattern = null;
			bestNumberOfImprovements = 0;

			// check all patterns of the current collection
			// int c = 0;
			for (Set<Integer> candidate : candidatePatterns)
			{
				// c++;
				// System.out.println("pattern " + c + " of " +
				// candidatePatterns.size());

				int numberOfImprovements = 0;

				// builds new PDB if there is no one for this candidate
				canonical.addTemporaryPatternDatabase(candidate);

				numberOfImprovements = patternImprovesHeuristic(canonical,
						candidate, samples);

				// System.out.println("numberOfImprovements " +
				// numberOfImprovements + " for pattern " + candidate);

				if (numberOfImprovements >= (greedyImprovement * samples.size()))
				{
					// take this pattern immediately
					bestAdditionalPattern = candidate;
					betterSuccessor = true;
					System.err.println("Pattern taken immediately.");
					break;
				}
				// TODO evaluate tie breaking
				// if (numberOfImprovements == bestNumberOfImprovements &&
				// ((double) numberOfImprovements >= minImprovement
				// * (double) samples.size())) {
				// // tie breaking rule (prefer smaller pattern?)
				// if (bestNumberOfImprovements > 0 && candidate.size() <
				// bestAdditionalPattern.size()) {
				// bestAdditionalPattern = candidate;
				// betterSucc = true;
				// }
				// }
				else if (numberOfImprovements > bestNumberOfImprovements
						&& (numberOfImprovements >= minImprovement
								* samples.size()))
				{

					bestNumberOfImprovements = numberOfImprovements;
					// System.out.println("bestNumberofImprovements " +
					// bestNumberOfImprovements);
					bestAdditionalPattern = candidate;
					// System.out.println("bestPattern " +
					// bestAdditionalPattern);
					betterSuccessor = true;
				}
				outOfTime = System.currentTimeMillis() - starttime > timeout;
				if (outOfTime)
					break;
				if (!PatternCollectionSearch.cacheTemporaryPDBs)
					canonical.temporaryPDBs.clear();
			}
			if (betterSuccessor)
			{
				currentCollection.add(bestAdditionalPattern);
				canonical.addPatternToPatternCollection(bestAdditionalPattern);
				canonical.dominancePruning();
				if (!outOfTime)
				{
					candidatePatterns.remove(bestAdditionalPattern);
					candidatePatterns.addAll(computeAdditionalPatterns(
							canonical, bestAdditionalPattern));
				}
			}
			step++;
			// clean bdds of sample states != initial state
			// if (!assumeFullObservability) {
			// for (State sample : samples) {
			// if (!sample.equals(Global.problem.manager.initialState))
			// sample.deleteBeliefStateBDD();
			// }
			// }
			System.err.println("Number of temporary PDBs: "
					+ canonical.temporaryPDBs.size());
			// System.err.println("Estimated memory usage for temporary PDBs: "
			// + canonical.sizesOfTemporaryPDBs * 16/1000 + " KByte");
		}
		System.err.println("Finished search for pattern collection.");

		if (outOfTime)
		{
			System.err.println("Reason: out of time!");
		} else if (step >= steps)
		{
			System.err.println("Reason: hillclimbing step bound of " + steps
					+ " reached!");
		} else
		{
			System.err.println("Reason: local minimum!");
		}

		System.err.println("Canonical heuristic:");
		System.err.println(canonical);

		if (!Global.problem.isFullObservable
				&& PatternCollectionSearch.fullObservablePatternSearch)
		{
			// use found pattern collection to build up a BeliefState PDB for
			// the search
			PatternCollectionSearch.fullObservablePatternSearch = false; // necessary
																			// for
																			// canonical
																			// heuristic
			canonical = new CanonicalPDBHeuristic(
					canonical.patterntoPDB.keySet());
			canonical.dominancePruning();
		}

		// delete temporary PDBs
		canonical.temporaryPDBs = null;
		RandomWalk.applicableOpsforSamples.clear();
		System.gc();

		return canonical;
	}
}
