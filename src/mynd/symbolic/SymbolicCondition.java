package mynd.symbolic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mynd.Global;
import mynd.explicit.ExplicitState;
import mynd.state.Condition;
import mynd.state.State;
import net.sf.javabdd.BDD;

/**
 * A condition is a formula represented by a BDD.
 * 
 * @author Manuela Ortlieb
 */

public class SymbolicCondition implements Condition {
	
	/**
	 * BDD representation of a partial variable assignment.
	 */
	public final BDD conditionBDD;
	
	/**
	 * All variables which are assigned to a value.
	 */
	public final Set<Integer> scope;
	
	/**
	 * Set true for debugging outputs.
	 */
	public final static boolean DEBUG = false;
	
    /**
     * Creates a symbolic condition from variable-value map.
     * 
     * @param variableValueAssignment assignment from variables to values
     */
	public SymbolicCondition(Map<Integer, Integer> variableValueAssignment) {
		BDD bdd = Global.BDDManager.B.one();
		for (int var : variableValueAssignment.keySet()) {
			bdd = Global.BDDManager.factBDDs[var][variableValueAssignment.get(var)].and(bdd);
		}
		conditionBDD = bdd;
		scope = Collections.unmodifiableSortedSet(new TreeSet<Integer>(variableValueAssignment.keySet()));
		assert ExplicitState.assertVariableOrder(scope);
	}
    
    /**
     * Create a condition from a given formula as BDD and corresponding scope.
     * 
     * @param condition BDD which describes a condition.
     * @param scope scope of this condition
     */
    private SymbolicCondition(BDD conditionBDD, Set<Integer> scope) {
		this.conditionBDD = conditionBDD;
		this.scope = Collections.unmodifiableSortedSet(new TreeSet<Integer>(scope));
		assert ExplicitState.assertVariableOrder(this.scope);
    }

	/**
	 *  Tests if this condition is satisfied by the given state.
	 *  
	 * @param state
	 * @return true iff this condition is satisfied by the given state.
	 */
    public boolean isSatisfiedIn(State state) {
    	return ((((BeliefState) state).beliefStateBDD.and(conditionBDD.id().not())).isZero());
    }

	/**
	 * Abstraction of this condition induced by given pattern.
	 * 
	 * @param pattern set of variables
	 * @return abstracted condition
	 */
	public SymbolicCondition abstractToPattern(Set<Integer> pattern, BDD symbolicPatternComplement) {
		BDD abstractedConditionBDD = conditionBDD.exist(symbolicPatternComplement);
		Set<Integer> abstractedScope = new TreeSet<Integer>(scope);
		abstractedScope.retainAll(pattern);
		assert ExplicitState.assertVariableOrder(abstractedScope);
		return new SymbolicCondition(abstractedConditionBDD, abstractedScope);
	}
	
   /**
     * Get string representation of this partial variable assignment.
     * 
     * @return string representation of the BDD
     */
	@Override
    public String toString() {
        return conditionBDD.toString();
    }
    
    /**
     * Hash code of this partial variable assignment.
     */
    @Override
    public int hashCode() {
        return conditionBDD.hashCode();
    }
    
	/**
	 * Test if given object and this condition are equal.
	 * 
	 * @param obj
	 * @return true iff given object and this condition are equal
	 */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SymbolicCondition)) {
            return false;
        }
        SymbolicCondition c = (SymbolicCondition) obj;
        
        return c.conditionBDD.equals(this.conditionBDD);
    }
    
    /**
     * Delete BDDs.
     * 
     * Warning: Symbolic operator can not be used anymore after calling this method. 
     */
    public void free() {
    	conditionBDD.free();
    }
    
    public SymbolicCondition id() {
    	return new SymbolicCondition(conditionBDD.id(), new HashSet<Integer>(scope));
    }
    
    /**
     * Dumping method. Use this only for debugging.
     */
    public void dump() {
    	List<Map<Integer, Integer>> valuations = Global.BDDManager.getValuations(conditionBDD, scope);
    	assert (valuations.size() == 1);	
    	Map<Integer, Integer> valuation = valuations.get(0);
    	String val = "{";
    	for (int var : valuation.keySet())
    		val = val + var + "=" + valuation.get(var) + ", ";
    	if (valuation.isEmpty())
    		System.out.println(val + "}");
    	else
    		System.out.println(val.substring(0, val.length() - 2) + "}");
    }

	/* (non-Javadoc)
	 * @see mynd.state.Condition#isSatisfiedInForLAOStar(mynd.state.State)
	 */
	@Override
	public boolean isSatisfiedInForLAOStar(State state)
	{
		// TODO Auto-generated method stub
		return false;
	}
}