package mynd.symbolic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mynd.Global;
import mynd.explicit.ExplicitOperator.OperatorRule;
import mynd.util.Pair;
import net.sf.javabdd.BDD;


/**
 * Symbolic axiom evaluator to apply axiom rules in case of partial observability.
 * 
 * @author Manuela Ortlieb
 */

public class SymbolicAxiomEvaluator {
	
	/**
	 * Symbolic representation of axioms.
	 */
	private BDD axioms;
	
	/**
	 * Symbolic representation of derived variables.
	 */
	private BDD derivedVars;
	
	/**
	 * Number of axioms.
	 */
	private int numberOfAxioms;
	
	/**
	 * Set true for debug information.
	 */
	public final static boolean DEBUG = false;
	
	/**
	 * Create and initialize a symbolic axiom evaluator.
	 */
	public SymbolicAxiomEvaluator() {
		numberOfAxioms = Global.problem.numAxioms;
		if (numberOfAxioms == 0)
			return;
		initialize(Global.problem.axioms);
	}
	
	public SymbolicAxiomEvaluator(Set<OperatorRule> axioms) {
		numberOfAxioms = axioms.size();
		if (numberOfAxioms == 0)
			return;
		initialize(axioms);
	}
	
	private void initialize(Set<OperatorRule> setOfAxioms) {
		Map<Pair<Integer, Integer>, Set<Set<Pair<Integer, Integer>>>> derivedVarsToAxioms = new HashMap<Pair<Integer,Integer>, Set<Set<Pair<Integer,Integer>>>>((int) (Global.problem.numAxioms * 0.75) + 1);
		for (OperatorRule axiom : setOfAxioms) {
			if (derivedVarsToAxioms.containsKey(axiom.head)) {
				derivedVarsToAxioms.get(axiom.head).add(axiom.body);
			}
			else {
				Set<Set<Pair<Integer, Integer>>> newSet = new HashSet<Set<Pair<Integer,Integer>>>();
				newSet.add(axiom.body);
				derivedVarsToAxioms.put(axiom.head, newSet);
			}
		}
		
		Set<Integer> derivedVariables = new HashSet<Integer>(derivedVarsToAxioms.size());
		// Build symbolic representation of axioms.
		axioms = Global.BDDManager.B.one();
		for (Pair<Integer, Integer> fact : derivedVarsToAxioms.keySet()) {
			derivedVariables.add(fact.first);
			BDD disjunction = Global.BDDManager.B.zero();
			for (Set<Pair<Integer, Integer>> condition : derivedVarsToAxioms.get(fact)) {
				BDD conjunction = Global.BDDManager.B.one();
				for (Pair<Integer, Integer> conditionFact : condition) {
					conjunction = conjunction.and(Global.BDDManager.factBDDs[conditionFact.first][conditionFact.second]);
				}
				disjunction.orWith(conjunction);
			}
			BDD biimp = Global.BDDManager.factBDDs[fact.first][fact.second].biimp(disjunction);
			axioms.andWith(biimp);
			disjunction.free();
		}		
		derivedVars = Global.BDDManager.B.one();
		for (int var : derivedVariables) {
			derivedVars = derivedVars.and(Global.BDDManager.stateVarDomains[var].set());
			derivedVars = derivedVars.and(Global.BDDManager.primedStateVarDomains[var].set());
		}
		derivedVarsToAxioms = null;
		derivedVariables = null;
	}
	
	/**
	 * Evaluate a given belief state BDD.
	 *   
	 * @param beliefStateBDD
	 * @return given BDD after applying axiom rules.
	 */
	public BDD evaluate(BDD beliefStateBDD) {
		if (numberOfAxioms == 0)
			return beliefStateBDD;
		beliefStateBDD = beliefStateBDD.id().exist(derivedVars);
		if (DEBUG) {
			System.out.println("Evaluate...");
			System.out.println(beliefStateBDD);
		}
		beliefStateBDD = beliefStateBDD.and(axioms);
		if (DEBUG) {
			System.out.println("Deduce...");
			System.out.println(beliefStateBDD);
		}
		return beliefStateBDD;
	}
}
