package mynd.symbolic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mynd.Global;
import mynd.explicit.ExplicitEffect;
import mynd.explicit.ExplicitOperator;
import mynd.explicit.ExplicitState;
import mynd.heuristic.pdb.Abstraction;
import mynd.state.Operator;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.JFactory;
import net.sf.javabdd.BDD.BDDIterator;

/**
 * @author Manuela Ortlieb
 *
 */
public class BDDManager {
	
	/**
	 * Set true for debugging outputs.
	 */
	public static final boolean DEBUG = false;
	
    /**
     * BDDFactory used
     */
    public final BDDFactory B;
	
	/**
	 * BDDs corresponding to state variables. //
	 */
	public BDD[][] factBDDs;
	
	/**
	 * BDDs corresponding to primed state variables.
	 */
	BDD[][] primedFactBDDs;
	
	/**
	 * BDD Domain of state variables.
	 */
	BDDDomain[] stateVarDomains;
	
	/**
	 * BDD Domain of primed state variables.
	 */
	BDDDomain[] primedStateVarDomains;
	
	/**
	 * BDD representing the conjunction of all unprimed state variables.
	 */
	BDD setOfUnprimedStateVars;
	
	/**
	 * BDD representing the conjunction of all primed state variables.
	 */
	BDD setOfPrimedStateVars;
	
	/**
	 * Set of all state variables;
	 */
	Set<Integer> stateVariables;
	
	/**
	 * Map from symbolic operator to corresponding explicit operator.
	 */
	Map<SymbolicOperator, ExplicitOperator> operatorMapping = new HashMap<SymbolicOperator, ExplicitOperator>(); 
	
	/**
	 * Create a BDD Manager.
	 */
	public BDDManager() {
		if (Runtime.getRuntime().maxMemory() > 3000000000L)
			B = JFactory.init(100000000,1000000);
		else {
			System.err.println("Warning! Not enough RAM to initialize BDD library with desired size.");
			B = JFactory.init(10000000,100000); // for local testing on machine with too less RAM
		}
	}

	/**
	 * Initialize BDD Manager.
	 * 
	 * @param numStateVars number of state variables
	 * @param domainSizes domain sizes of state variables
	 */
	public void initialize(int numStateVars, ArrayList<Integer> domainSizes) {
		// Global problem isn't initialized yet!
		factBDDs = new BDD[numStateVars][];
		primedFactBDDs = new BDD[numStateVars][];		
		stateVarDomains = new BDDDomain[numStateVars];
		primedStateVarDomains = new BDDDomain[numStateVars];
		initializeFactBDDs(numStateVars, domainSizes);
	}
	
	/**
	 * Initialize fact BDDs.
	 * 
	 * @param numStateVars number of state variables
	 * @param domainSizes domain sizes of state variables
	 */
	private void initializeFactBDDs(int numStateVars, ArrayList<Integer> domainSizes) {
		// Global problem isn't initialized yet!
		setOfUnprimedStateVars = B.one();
		setOfPrimedStateVars = B.one();
		Set<Integer> stateVars = new LinkedHashSet<Integer>((int) Math.ceil(numStateVars / 0.75));
		for(int var = 0; var < numStateVars; var++) {
			stateVars.add(var);
			stateVarDomains[var] = B.extDomain(domainSizes.get(var));
			primedStateVarDomains[var] = B.extDomain(domainSizes.get(var));
			factBDDs[var] = new BDD[domainSizes.get(var)];
			primedFactBDDs[var] = new BDD[domainSizes.get(var)];
			setOfUnprimedStateVars = setOfUnprimedStateVars.and(stateVarDomains[var].set());
			setOfPrimedStateVars = setOfPrimedStateVars.and(primedStateVarDomains[var].set());
			for (int val = 0; val < domainSizes.get(var); val++) {
				factBDDs[var][val] = stateVarDomains[var].ithVar(val);
				primedFactBDDs[var][val] = primedStateVarDomains[var].ithVar(val);				
			}
		}
		stateVariables = Collections.unmodifiableSet(stateVars);
	}
	
	/**
	 * Create a symbolic operator from given explicit operator.
	 * 
	 * @param explicitOp explicit operator
	 * @return symbolic operator of given explicit operator
	 */
	public SymbolicOperator createSymbolicOperator(String operatorName, ExplicitOperator explicitOp) {
		SymbolicCondition precondition = new SymbolicCondition(explicitOp.precondition.variableValueMap);
		assert (!precondition.conditionBDD.isZero()); // Trivially inapplicable operator.
		
		BDD disjunctionOverChoices = null;
		if (explicitOp.getNondeterministicEffect() != null) {
			disjunctionOverChoices = B.zero();
			for (Set<ExplicitEffect> choice : explicitOp.getNondeterministicEffect()) {
				BDD conjunctionForEffects = B.one();
				Set<Integer> affectedVarsByThisChoice = new HashSet<Integer>();
				for (ExplicitEffect effect : choice) {
					assert (effect.condition.size == 0); // Effect preconditions are not supported.
					affectedVarsByThisChoice.add(effect.variable);
					// new value of var is val
					conjunctionForEffects = conjunctionForEffects.and(primedFactBDDs[effect.variable][effect.value]);
				}
				BDD frameAxiom = B.one();
				for (int var = 0; var < Global.problem.numStateVars; var++) {
					if (!affectedVarsByThisChoice.contains(var)) {
						// frame axiom, new value stays old value
						for (int val = 0; val < Global.problem.domainSizes.get(var); val++) {
							frameAxiom = frameAxiom.and(factBDDs[var][val].biimp(primedFactBDDs[var][val]));
						}
					}
				}
				conjunctionForEffects.andWith(frameAxiom);
				disjunctionOverChoices.orWith(conjunctionForEffects);
			}
		}
		assert (disjunctionOverChoices != null || !explicitOp.observation.isEmpty());
		SymbolicOperator result = new SymbolicOperator(operatorName, precondition, disjunctionOverChoices, explicitOp.observation, explicitOp.isAbstracted, explicitOp.isDeterminized(), explicitOp.getCost());
		result.setAffectedVariables(explicitOp.getAffectedVariables());
		
		// Store mapping from symbolic op to explicit op. Determinized ops are not stored since they are not abstracted or
		// determinized again in future.
		if (!result.isDeterminized()) {
			if (operatorMapping.containsKey(result)) {
				if (DEBUG) {
					System.out.println("result: already there.");
					explicitOp.dump();
					System.out.println("here:");
					operatorMapping.get(result).dump();
				}
				assert operatorMapping.get(result).equals(explicitOp);
				// TODO somehow unnecessary to build this symbolic operator a second time... use another map? 
			}
			else {
				operatorMapping.put(result, explicitOp);
			}
		}
		return result;
	}
	
	/**
	 * Initialize symbolic operators.
	 * 
	 * @return set of symbolic operators.
	 */
	public Set<Operator> initializeOperators(Set<ExplicitOperator> operators) {
		Set<Operator> symbolicOps = new LinkedHashSet<Operator>((int) (operators.size() * 0.75) + 1);
		for (ExplicitOperator op : operators) {
			symbolicOps.add(createSymbolicOperator(op.getName(), op));
		}
		return symbolicOps;
	}
	
	/**
	 * Initialize symbolic initial state.
	 * 
	 * @param factVars
	 * @param factVals
	 * @param oneOfVars
	 * @param oneOfVals
	 * @param orVars
	 * @param orVals
	 * @return initial belief state
	 */
	public BDD initializeInitialStateBDD(int[] factVars, int[] factVals, int[][] oneOfVars, 
										int[][] oneOfVals, String[] formulae) {
		// handle facts
		BDD init = B.one();
		for (int i = 0; i < factVars.length; i++) {
			int var = factVars[i];
			int val = factVals[i];
			init = init.and(factBDDs[var][val]);	
			
		}
		// handle oneOfs
		for (int i = 0; i < oneOfVars.length; i++) {
			BDD oneOf = B.zero();
			int truePair = 0;
			BDD oneComb = B.one(); 
			int j = 0;
			while (j < oneOfVars[i].length) {
				if (j == truePair) {
					oneComb = factBDDs[oneOfVars[i][j]][oneOfVals[i][j]].and(oneComb);
				}
				else {
					oneComb = ((factBDDs[oneOfVars[i][j]][oneOfVals[i][j]]).id().not()).and(oneComb);
				}
				if (j == (oneOfVars[i].length - 1) && (truePair != (oneOfVars[i].length - 1))) {
					j = -1;
					truePair++;
					oneOf.orWith(oneComb);
					oneComb = B.one();
				}
				else if (j == (oneOfVars[i].length - 1) && (truePair == (oneOfVars[i].length - 1))) {
					oneOf.orWith(oneComb);
				}
				j++;
			}
			init.andWith(oneOf);
		}
		for (int i = 0; i < formulae.length; i++) {
			init.andWith(processFormula(formulae[i]));
		}
		return init;
	}
	
	
	private BDD processFormula(String formulaAsString) {
		boolean DEBUG = false;
		if (DEBUG) {
			System.out.println("Formula as string is: " + formulaAsString);
		}
		if (formulaAsString.startsWith("or(") || formulaAsString.startsWith("and(") || formulaAsString.startsWith("not(")) {
			String operand = "";
			BDD formula;
			if (formulaAsString.startsWith("or(")) {
				operand = "or";
				if (DEBUG) {
					System.out.println("Processing an or...");
				}
				formula = B.zero();
			}
			else if (formulaAsString.startsWith("and(")) {
				operand = "and";
				if (DEBUG) {
					System.out.println("Processing an and...");
				}
				formula = B.one();
			}
			else if (formulaAsString.startsWith("not(")) {
				if (DEBUG) {
					System.out.println("Processing a not...");
				}
				return processFormula(formulaAsString.substring(4, formulaAsString.length() - 1)).id().not();
			}
			else {
				formula = B.zero();
				assert false; // TODO not
			}
			String sub = formulaAsString.substring(operand.length() + 1, formulaAsString.length() - 1);
			if (DEBUG) {
				System.out.println("sub is " + sub);
			}
			
			boolean open = false;
			int counter = 0;
			int startIndex = -1;
			// Get all parts and process them recursively.
			for (int i = 0; i < sub.length(); i++) {
				if (sub.charAt(i) == '(') {
					if (!open) {
						open = true;
						if (i > 0 && sub.charAt(i-1) == 'r') {
							// or
							startIndex = i - 2;
						}
						else if (i > 0 && sub.charAt(i-1) == 'd') {
							// and
							startIndex = i - 3;
						}
						else {
							startIndex = i;
						}
						assert startIndex >= 0;
					}
					counter++;
				}
				else if (sub.charAt(i) == ')') {
					assert open;
					assert startIndex > -1;
					counter--;
					if (counter == 0) {
						if (operand.equals("or")) {
							formula = processFormula(sub.substring(startIndex, i + 1)).or(formula);
						}
						else if (operand.equals("and")) {
							formula = processFormula(sub.substring(startIndex, i + 1)).and(formula);
						}
						open = false;
						startIndex = -1;
					}
				}
			}
			return formula;
			    
		}
		else if (formulaAsString.startsWith("(")) {
			assert formulaAsString.endsWith(")");
			String sub = formulaAsString.substring(1, formulaAsString.length() - 1); 
			assert !sub.contains("(");
			assert !sub.contains(")");
			int var = new Integer(sub.split(" ")[0]);
			int val = new Integer(sub.split(" ")[1]);
			if (DEBUG) {
				System.out.println("Processing an atom...");
				System.out.println("with var " + var);
				System.out.println("and val " + val);
			}
			return factBDDs[var][val];
		}
		else {
			assert false;
			return B.zero();
		}
	}
		
	/**
	 * Initialize symbolic goal.
	 * 
	 * @return symbolic goal
	 */
	public SymbolicCondition initializeGoal() {
		assert(Global.problem != null);
		return new SymbolicCondition(Global.problem.explicitGoal.variableValueMap);
	}
	
	/**
	 * Replace primed BDD variables by unprimed BDD variables.
	 * 
	 * @return a BDDPairing where primed variables are replaced by unprimed variables
	 */
	public BDDPairing primedToUnprimed() {
		BDDPairing pairing = B.makePair();
		pairing.set(primedStateVarDomains, stateVarDomains);
		return pairing;
	}
	
	/**
	 * Get conjunction of unprimed variables of given set of variables.
	 * 
	 * @param subSetOfStateVars set of variables
	 * @return conjunction of unprimed variables
	 */
	public BDD getUnprimedVariables(Set<Integer> subsetOfStateVariables) {
		BDD setOfUnprimedVariables = B.one();
		for (int var : subsetOfStateVariables) 
			setOfUnprimedVariables = setOfUnprimedVariables.and(stateVarDomains[var].set());
		return setOfUnprimedVariables;
	}
	
	/**
	 * Get all satisfying assignments of the given BDD with regard to given state variables.
	 * 
	 * @param bdd
	 * @param subsetOfStateVariables
	 * @return list of variable value maps over given state variables.
	 */
	public List<Map<Integer, Integer>> getValuations(BDD bdd, Set<Integer> subsetOfStateVariables) {
		return computeValuation(bdd, getUnprimedVariables(subsetOfStateVariables), subsetOfStateVariables);
	}
	
	/**
	 * Get all satisfying assignments of the given BDD with regard to all state variables.
	 * Note: This BDD may not be existential quantified wrt a pattern before. Use 
	 * getValuations(BDD bdd, Set<Integer> subsetOfStateVariables) in that case.
	 * 
	 * @param bdd 
	 * @return list of variable value maps over all state variables. 
	 */
	public List<Map<Integer, Integer>> getCompleteValuations(BDD bdd) {
		return computeValuation(bdd, setOfUnprimedStateVars, stateVariables);
	}
	
	/**
	 * Computes all satisfying assignments of the given BDD over the set of state variables.
	 * 
	 * @param bdd
	 * @param setOfUnprimedVars wrt the given state variables
	 * @param setOfStateVariables 
	 * @return list of variable value maps over given state variables
	 */
	private List<Map<Integer, Integer>> computeValuation(BDD bdd, BDD setOfUnprimedVars, Set<Integer> setOfStateVariables) {
		//System.out.println("tada." + setOfStateVariables);
		assert ExplicitState.assertVariableOrder(setOfStateVariables);
		assert bdd.satCount(setOfUnprimedVars) <= Integer.MAX_VALUE; // an ArrayList can not store more than Integer.MAX_VALUE elements
		List<Map<Integer, Integer>> result = new ArrayList<Map<Integer, Integer>>();
		BDDIterator it = bdd.iterator(setOfUnprimedVars);	
		while (it.hasNext()) {
			// get a satisfying assignment
			BDD sat = (BDD) it.next();		
			Map<Integer, Integer> variableValueMap = new LinkedHashMap<Integer, Integer>((int) Math.ceil(setOfStateVariables.size() / 0.75));
			for (int var : setOfStateVariables) {
				BigInteger value = sat.scanVar(stateVarDomains[var]);
				variableValueMap.put(var, value.intValue());
			}
			result.add(variableValueMap);
			sat.free();
		}
		return result;
	}
	
	/**
	 * Draw a random satisfying assignment of the given BDD.
	 *
	 * @param bdd BDD with at least one satisfying assignment
	 * @return a randomly drawn satisfying assignment of given BDD
	 */
	public Map<Integer, Integer> getOneRandomValuation(BDD bdd) {
		assert bdd.satCount(setOfUnprimedStateVars) >= 1;
		Set<Integer> stateVars = new LinkedHashSet<Integer>();
		for (int var = 0; var < Global.problem.numStateVars; var++) 
			stateVars.add(var); // TODO store it
		Map<Integer, Integer> variableValueMap = new LinkedHashMap<Integer, Integer>((int) Math.ceil(stateVars.size() / 0.75));
		BDD result = bdd.satOneRandom(setOfUnprimedStateVars);
		for (int var : stateVars) {
			BigInteger value = result.scanVar(stateVarDomains[var]);
			variableValueMap.put(var, value.intValue());
		}
		result.free();
		return variableValueMap;
	}
	
	/**
	 * Get symbolic pattern complement of given pattern.
	 * 
	 * @param pattern set of state variables
	 * @return symbolic pattern complement
	 */
	// TODO caching?
	public BDD getSymbolicPatternComplement(Set<Integer> pattern) {
		BDD patternComplement = B.one();	
		for (int var : Abstraction.getPatternComplement(pattern)) {
			patternComplement = patternComplement.and(stateVarDomains[var].set());
			patternComplement = patternComplement.and(primedStateVarDomains[var].set());
		}
		return patternComplement;
	}
	
	/**
	 * Clear operator mapping, because this mapping is only needed until pattern databases are created.
	 */
	public void clearOperatorMapping() {
		operatorMapping = null;
	}
}
