package mynd.symbolic;

import java.util.Collections;
import java.util.Set;

import mynd.Global;
import mynd.explicit.ExplicitOperator;
import mynd.state.Condition;
import mynd.state.Operator;
import mynd.util.Pair;
import net.sf.javabdd.BDD;



/**
 * 
 * @author Manuela Ortlieb
 *
 */
public class SymbolicOperator extends Operator {

    /**
     * Precondition of this operator.
     */
    public final SymbolicCondition precondition;

    /**
     * Effect of this operator.
     * Note: If this operator has no effect, then effect is null.
     */
    public final BDD effect;

    /**
     * Set DEBUG to true to get Debug information.
     */
    public final static boolean DEBUG = false;

    /**
     * Creates a new BDDOperator.
     * 
     * @param name         name of the operator
     * @param precondition precondition of the operator
     * @param effect       effect of the operator
     * @param observations observations of the operator
     * @param isAbstracted true iff operator is an abstracted operator
     */
    public SymbolicOperator(String name, SymbolicCondition precondition, BDD effect, Set<Pair<Integer, Integer>> observation, boolean isAbstracted, double cost) {
        super(name, observation, isAbstracted, effect != null, cost);
        this.precondition = precondition;
        this.effect = effect;
        if (!isCausative) {
            assert effect == null;
            assert !observation.isEmpty();
        }
        if (!isSensing) {
            assert effect != null;
            assert observation.isEmpty();
        }
    }

    public SymbolicOperator(String name, SymbolicCondition precondition, BDD effect, Set<Pair<Integer, Integer>> observation, boolean isAbstracted, boolean isDeterminized, double cost) {
        this(name, precondition, effect, observation, isAbstracted, cost);
        this.isDeterminized = isDeterminized;
    }

    /**
     * Two BDD operators are equal if their precondition, their effect and their observations
     * are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SymbolicOperator))
            return false;
        SymbolicOperator op = (SymbolicOperator) o;
        if (isDeterminized != op.isDeterminized) {
            return false;
        }
        if (isAbstracted != op.isAbstracted) {
            return false;
        }
        if (!observation.equals(op.observation)) {
            return false;
        }
        if (getPrecondition().equals(op.getPrecondition())) {
            return ((effect == null && op.effect == null) || effect.equals(op.effect));
        }
        return false;
    }

    /**
     * Get hash code of this operator.
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        if (effect == null)
            return observation.hashCode() + precondition.hashCode();
        else
            return observation.hashCode() + precondition.hashCode() + effect.hashCode();
    }

    /**
     * Get precondition of this operator.
     * 
     * @return precondition of this operator.
     */
    @Override
    public Condition getPrecondition() {
        return precondition;
    }

    /**
     * Abstract this symbolic operator to given pattern. First corresponding explicit op is abstracted, and after that, 
     * a symbolic operator is created from abstracted explicit operator.
     * 
     * @param pattern
     * @return abstracted operator or null if resulting operator has no effects and no observations
     */
    @Override
    public SymbolicOperator abstractToPattern(Set<Integer> pattern) {
        assert (!isDeterminized); // A determinized operator must not be abstracted.
        ExplicitOperator abstractedExplicitOp = Global.BDDManager.operatorMapping.get(this).abstractToPattern(pattern);
        if (abstractedExplicitOp == null)
            return null;
        return Global.BDDManager.createSymbolicOperator(name + "_abs", abstractedExplicitOp);
    }

    @Override
    public ExplicitOperator getExplicitOperator() {
        assert (!isDeterminized);
        return Global.BDDManager.operatorMapping.get(this);
    }

    /**
     * Delete BDDs.
     * 
     * Warning: This operator can not be used anymore after calling this method!
     */
    @Override
    public void free() {
        if (effect != null)
            effect.free();
        precondition.free();
    }

    /**
     * Dump this symbolic operator. Only for debugging.
     */
    @Override
    public void dump() {
        System.out.println("Symbolic operator: " + name);
        if (!isDeterminized()) {
            System.out.println("Explicit representation:");
            Global.BDDManager.operatorMapping.get(this).dump();
        }
        else 
            System.out.println("!! Determinized operator !!");
    }

    /**
     * Set the set of affected variables.
     * 
     * @param affectedVariables
     */
    public void setAffectedVariables(Set<Integer> affectedVariables) {
        assert this.affectedVariables.isEmpty(); // Set them only once!
        this.affectedVariables = Collections.unmodifiableSet(affectedVariables);
    }

    @Override
    public Operator copy() {
        assert false;
        return null;
    }

    /**
     * Set cost of this operator.
     * 
     * @param cost
     */
    public void setCost(double cost) {
        this.cost = cost;
        // update the corresponding explicit operator, too.
        Global.BDDManager.operatorMapping.get(this).setCost(cost);
    }
}
